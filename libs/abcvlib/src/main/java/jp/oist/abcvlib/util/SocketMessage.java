package jp.oist.abcvlib.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Vector;

public class SocketMessage {
    
    private Selector selector;
    private Channel sc;
    private InetSocketAddress addr;
    private ByteBuffer _recv_buffer;
    private ByteBuffer _send_buffer;
    private boolean _request_queued;
    private int _jsonheader_len = 0;
    private JSONObject jsonheader; // Will tell Java at which points in msgContent each model lies (e.g. model1 is from 0 to 1018, model2 is from 1019 to 2034, etc.)
    private ByteBuffer jsonheaderBuffer;
    private ByteBuffer msgContent; // Should contain ALL model files. Parse to individual files after reading
    private int connid;
    private int msgFromServerTotalLength = 1024; // Predetermined length
    private int msgFromServerTotalBitsRecv = 0;
    private Vector<ByteBuffer> writeBufferVector = new Vector<>(); // List of episodes
    private final String TAG = "SocketConnectionManager";


    public SocketMessage(Selector selector, Channel sc, InetSocketAddress addr){
        this.sc = sc;
        this.addr = addr;
        this._recv_buffer = ByteBuffer.allocate(1024);
        this._send_buffer = ByteBuffer.allocate(1024);
        this._request_queued = false;
    }

    public void process_events(SelectionKey selectionKey){
        Log.i(TAG, "process_events");
        try{
            if (selectionKey.isReadable()){
                Log.i(TAG, "read event");
                read(selectionKey);
            }
            if (selectionKey.isWritable()){
                Log.i(TAG, "write event");
                write(selectionKey);
            }
        } catch (ClassCastException | IOException | JSONException e){
            e.printStackTrace();
        }
    }

    private void read(SelectionKey selectionKey) throws IOException, JSONException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        // At this point the _recv_buffer should have been cleared (pointer 0 limit=cap, no mark)
        int bitsRead = channel.read(_recv_buffer);
        Log.v(TAG, "Read " + bitsRead + " from connection " + channel.getRemoteAddress());

        if (bitsRead != 0){
            // If you have not determined the length of the header via the 2 byte short protoheader,
            // try to determine it, though there is no gaurantee it will have enough bytes. So it may
            // pass through this if statement multiple times.
            if (this._jsonheader_len == 0){
                process_protoheader();
            }
            // _jsonheader_len will only be larger than 0 if set properly (finished being set).
            // jsonheader will be null until the buffer gathering it has filled and converted it to
            // a JSONobject.
            else if (this._jsonheader_len > 0 && this.jsonheader == null){
                process_jsonheader();
            }
            else if (msgContent.remaining() > 0){
                process_msgContent();
            } else {
                Log.e(TAG, "bitsRead but don't know what to do with them");
            }
        }
        // If total msgContent received
        else if ((msgFromServerTotalLength == msgFromServerTotalBitsRecv)){
            // Maybe use later to break
        }
        // No data being sent from server
        else{
            // Maybe use later to break
        }
    }

    private void write(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        if (!writeBufferVector.isEmpty()){
            Log.i(TAG, "writeBufferVector not empty");
            ByteBuffer bytesToWrite = writeBufferVector.get(0);
            bytesToWrite.rewind()

            if (bytesToWrite.remaining() > 0){
                int numBytesToWrite = bytesToWrite.remaining();

                Log.v(TAG, "Sending Episode to " + channel.getRemoteAddress());
                int bytesWritten = channel.write(bytesToWrite);

                writeBufferVector.set(0, bytesToWrite);

                if (bytesToWrite.remaining() == 0){
                    // Remove episode from buffer so as to not write it again.
                    writeBufferVector.remove(0);
                }
            }
        }else{
            Log.i(TAG, "writeBufferVector empty");
        }
    }

    /**
     * recv_buffer may contain 0, 1, or capacity bytes. If it has more than hdrlen, then process
     * the first two bytes to obtain the length of the jsonheader. Else exit this function and
     * read from the buffer again until it fills past length hdrlen.
     */
    private void process_protoheader(){
        int hdrlen = 2;
        if (_recv_buffer.position() >= hdrlen){
            byte[] byteVal = new byte[2];
            _recv_buffer.flip(); //pos at 0 and limit set to bitsRead
            _jsonheader_len = _recv_buffer.getShort(); // Read 2 bytes converts to short and move pos to 2
            // allocate new ByteBuffer to store full jsonheader
            if (_jsonheader_len <= _recv_buffer.capacity()){
                jsonheaderBuffer = ByteBuffer.allocate(_jsonheader_len);
                // Store the rest of the read bytes to the jsonheaderBuffer before trying to read more
                jsonheaderBuffer.put(_recv_buffer);
                // clear the recv buffer before trying to read to it again
                _recv_buffer.clear();
            }else{
                Log.e(TAG, "jsonheader length cannot exceed recv_buffer capacity");
            }
        }
    }

    /**
     *  Take bytes from _recv_buffer and dump into jsonheaderBuffer
     *  _recv_buffer may have less than, equal or more than the number of bytes remaining to be
     *  filled in jsonheaderBuffer. Need to handle each case.
     */
    private void process_jsonheader() throws JSONException {

        int remaining = _recv_buffer.position() - jsonheaderBuffer.remaining();
        _recv_buffer.flip(); //pos at 0 and limit set to bitsRead set ready to read

        // _recv_buffer cannot be 0 at this point, so if remaining is non-positive everything can
        // be put into jsonheaderBuffer
        if (remaining <= 0){
            // put all values in _recv_buffer into jsonheaderBuffer
            jsonheaderBuffer.put(_recv_buffer);
            // clear the recv buffer before trying to read to it again
            _recv_buffer.clear();
        }
        else {
            // attempting to put all values in _recv_buffer into jsonheaderBuffer will cause buffer
            // overflow so need to
            do{
                // fill jsonheaderBuffer
                jsonheaderBuffer.put(_recv_buffer.get());
                remaining--;
            }while (remaining > 0);

            // jsonheaderBuffer should now be full and ready to convert to a JSONobject
            jsonheader = new JSONObject(jsonheaderBuffer.asCharBuffer().toString());

            // take remainder of _recv_buffer and put into msgContent
            msgContent.put(_recv_buffer);
            // clear the recv buffer before trying to read to it again
            _recv_buffer.clear();
        }
    }

    private void process_msgContent() throws JSONException {

        int remaining = _recv_buffer.position() - msgContent.remaining();
        _recv_buffer.flip(); //pos at 0 and limit set to bitsRead set ready to read

        // _recv_buffer cannot be 0 at this point, so if remaining is non-positive everything can
        // be put into msgContent
        if (remaining <= 0){
            // put all values in _recv_buffer into msgContent
            msgContent.put(_recv_buffer);
            // clear the recv buffer before trying to read to it again
            _recv_buffer.clear();
        }
        else {
            // attempting to put all values in _recv_buffer into msgContent will cause buffer
            // overflow so need to
            do{
                // fill msgContent
                msgContent.put(_recv_buffer.get());
                remaining--;
            }while (remaining > 0);

            // msgContent should now be full and ready to convert to a various model files.
            parseMsgContent();

            if (_recv_buffer.remaining() > 0){
                Log.e(TAG, "Server sent more bytes than it said it specified it would in JSONheader");
            }
            // final clearing of the recv buffer
            _recv_buffer.clear();
        }
    }

    private void parseMsgContent(){
        // Take info from JSONheader to parse msgContent into individual model files

        // After parsing all models notify MainActivity that models have been updated
    }

    // todo should be able deal with ByteBuffer from FlatBuffer rather than byte[]
    public boolean addEpisodeToWriteBuffer(byte[] episode){
        boolean success = false;
        try{
            ByteBuffer bb = ByteBuffer.wrap(episode);
            success = writeBufferVector.add(bb);
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.e(TAG, "SocketConnectionManager.data not initialized yet");
        }
        return success;
    }
}
