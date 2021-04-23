package jp.oist.abcvlib.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Vector;

public class SocketMessage {
    
    private final SocketChannel sc;
    private final Selector selector;
    private final ByteBuffer _recv_buffer;
    private ByteBuffer _send_buffer;
    private int _jsonheader_len = 0;
    private JSONObject jsonHeaderRead; // Will tell Java at which points in msgContent each model lies (e.g. model1 is from 0 to 1018, model2 is from 1019 to 2034, etc.)
    private byte[] jsonHeaderBytes;
    private ByteBuffer msgContent; // Should contain ALL model files. Parse to individual files after reading
    private final Vector<ByteBuffer> writeBufferVector = new Vector<>(); // List of episodes
    private final String TAG = "SocketConnectionManager";
    private JSONObject jsonHeaderWrite;
    private boolean msgReadComplete = false;
    private SocketListener socketListener;
    private long socketWriteTimeStart;
    private long socketReadTimeStart;


    public SocketMessage(SocketListener socketListener, SocketChannel sc, Selector selector){
        this.socketListener = socketListener;
        this.sc = sc;
        this.selector = selector;
        this._recv_buffer = ByteBuffer.allocate(1024);
        this._send_buffer = ByteBuffer.allocate(1024);
    }

    public void process_events(SelectionKey selectionKey){
        SocketChannel sc = (SocketChannel) selectionKey.channel();
//        Log.i(TAG, "process_events");
        try{
            if (selectionKey.isConnectable()){
                boolean connected = sc.finishConnect();
                if (connected){
                    Log.d(TAG, "Finished connecting to " + ((SocketChannel) selectionKey.channel()).getRemoteAddress());
                    Log.v(TAG, "socketChannel.isConnected ? : " + sc.isConnected());
                }
            }
            if (selectionKey.isWritable()){
//                Log.i(TAG, "write event");
                write(selectionKey);
            }
            if (selectionKey.isReadable()){
//                Log.i(TAG, "read event");
                read(selectionKey);

//                int ops = SelectionKey.OP_WRITE;
//                sc.register(selectionKey.selector(), ops, selectionKey.attachment());
            }

        } catch (ClassCastException | IOException | JSONException e){
            Log.e(TAG,"Error", e);
        }
    }

    private void read(SelectionKey selectionKey) throws IOException, JSONException {

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        while(!msgReadComplete){
            // At this point the _recv_buffer should have been cleared (pointer 0 limit=cap, no mark)
            int bitsRead = socketChannel.read(_recv_buffer);

            if (bitsRead > 0 || _recv_buffer.position() > 0){
                if (bitsRead > 0){
//                    Log.v(TAG, "Read " + bitsRead + " bytes from " + socketChannel.getRemoteAddress());
                }

                // If you have not determined the length of the header via the 2 byte short protoheader,
                // try to determine it, though there is no gaurantee it will have enough bytes. So it may
                // pass through this if statement multiple times. Only after it has been read will
                // _jsonheader_len have a non-zero length;
                if (this._jsonheader_len == 0){
                    socketReadTimeStart = System.nanoTime();
                    process_protoheader();
                }
                // _jsonheader_len will only be larger than 0 if set properly (finished being set).
                // jsonHeaderRead will be null until the buffer gathering it has filled and converted it to
                // a JSONobject.
                else if (this.jsonHeaderRead == null){
                    process_jsonheader();
                }
                else if (!msgReadComplete){
                    process_msgContent(selectionKey);
                } else {
                    Log.e(TAG, "bitsRead but don't know what to do with them");
                }
            }
        }
    }

    private void write(SelectionKey selectionKey) throws IOException, JSONException {

        if (!writeBufferVector.isEmpty()){
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

//            Log.v(TAG, "writeBufferVector contains data");

            if (jsonHeaderWrite == null){
                int numBytesToWrite = writeBufferVector.get(0).limit();

                // Create JSONHeader containing length of episode in Bytes
                Log.v(TAG, "generating jsonheader");
                jsonHeaderWrite = generate_jsonheader(numBytesToWrite);
                byte[] jsonBytes = jsonHeaderWrite.toString().getBytes(StandardCharsets.UTF_8);

                // Encode length of JSONHeader to first two bytes and write to socketChannel
                int jsonLength = jsonBytes.length;

                // Add up length of protoHeader, JSONheader and episode bytes
                int totalNumBytesToWrite = Integer.BYTES + jsonLength + numBytesToWrite;

                // Create new buffer that compiles protoHeader, JsonHeader, and Episode
                _send_buffer = ByteBuffer.allocate(totalNumBytesToWrite);

                Log.v(TAG, "Assembling _send_buffer");
                // Assemble all bytes and flip to prepare to read
                _send_buffer.putInt(jsonLength);
                _send_buffer.put(jsonBytes);
                _send_buffer.put(writeBufferVector.get(0));
                _send_buffer.flip();

                int total = _send_buffer.limit() / 1000000;

                Log.d(TAG, "Writing to " + total + "MB to server ...");

                // Write Bytes to socketChannel //todo shouldn't be while as should be non-blocking
                if (_send_buffer.remaining() > 0){
                    int numBytes = socketChannel.write(_send_buffer); // todo memory dump error here!
//                    int percentDone = (int) Math.ceil((((double) _send_buffer.limit() - (double) _send_buffer.remaining())
//                            / (double) _send_buffer.limit()) * 100);
//                    int total = _send_buffer.limit() / 1000000;
//                    Log.d(TAG, "Sent " + percentDone + "% of " + total + "Mb to " + socketChannel.getRemoteAddress());
                }
            } else{
                // Write Bytes to socketChannel
                if (_send_buffer.remaining() > 0){
                    socketChannel.write(_send_buffer);
//                    int percentDone = (int) Math.ceil((((double) _send_buffer.limit() - (double) _send_buffer.remaining())
//                            / (double) _send_buffer.limit()) * 100);
//                    int total = _send_buffer.limit() / 1000000;
//                    Log.d(TAG, "Sent " + percentDone + "% of " + total + "Mb to " + socketChannel.getRemoteAddress());
                }
            }
            if (_send_buffer.remaining() == 0){
                int total = _send_buffer.limit() / 1000000;
                double timeTaken = (System.nanoTime() - socketWriteTimeStart) * 10e-10;
                DecimalFormat df = new DecimalFormat();
                df.setMaximumFractionDigits(2);
                Log.i(TAG, "Sent " + total + "Mb in " + df.format(timeTaken) + "s");
                // Remove episode from buffer so as to not write it again.
                writeBufferVector.remove(0);
                // Clear sending buffer
                _send_buffer.clear();
                // make null so as to catch the initial if statement to write a new one.
                jsonHeaderWrite = null;

                // Set socket to read now that writing has finished.
                Log.d(TAG, "Reading from server ...");
                int ops = SelectionKey.OP_READ;
                sc.register(selectionKey.selector(), ops, selectionKey.attachment());
            }

        }
    }

    private JSONObject generate_jsonheader(int numBytesToWrite) throws JSONException {
        JSONObject jsonHeader = new JSONObject();

        jsonHeader.put("byteorder", ByteOrder.nativeOrder().toString());
        jsonHeader.put("content-length", numBytesToWrite);
        jsonHeader.put("content-type", "flatbuffer"); // todo Change to flatbuffer later
        jsonHeader.put("content-encoding", "flatbuffer"); //Change to flatbuffer later
        return jsonHeader;
    }

    /**
     * recv_buffer may contain 0, 1, or several bytes. If it has more than hdrlen, then process
     * the first two bytes to obtain the length of the jsonheader. Else exit this function and
     * read from the buffer again until it fills past length hdrlen.
     */
    private void process_protoheader() {
        Log.v(TAG, "processing protoheader");
        int hdrlen = 2;
        if (_recv_buffer.position() >= hdrlen){
            _recv_buffer.flip(); //pos at 0 and limit set to bitsRead
            _jsonheader_len = _recv_buffer.getShort(); // Read 2 bytes converts to short and move pos to 2
            // allocate new ByteBuffer to store full jsonheader
            jsonHeaderBytes = new byte[_jsonheader_len];

            _recv_buffer.compact();

            Log.v(TAG, "finished processing protoheader");
        }
    }

    /**
     *  As with the process_protoheader we will check if _recv_buffer contains enough bytes to
     *  generate the jsonHeader objects, and if not, leave it alone and read more from socket.
     */
    private void process_jsonheader() throws JSONException {

        Log.v(TAG, "processing jsonheader");

        // If you have enough bytes in the _recv_buffer to write out the jsonHeader
        if (_jsonheader_len - _recv_buffer.position() < 0){
            _recv_buffer.flip();
            _recv_buffer.get(jsonHeaderBytes);
            // jsonheaderBuffer should now be full and ready to convert to a JSONobject
            jsonHeaderRead = new JSONObject(new String(jsonHeaderBytes));
            Log.d(TAG, "JSONheader from server: " + jsonHeaderRead.toString());

            try{
                int msgLength = (int) jsonHeaderRead.get("content-length");
                msgContent = ByteBuffer.allocate(msgLength);
            }catch (JSONException e) {
                Log.e(TAG, "Couldn't get content-length from jsonHeader sent from server", e);
            }
        }
        // Else return to selector and read more bytes into the _recv_buffer

        // If there are any bytes left over (part of the msg) then move them to the front of the buffer
        // to prepare for another read from the socket
        _recv_buffer.compact();
    }

    /**
     * Here a bit different as it may take multiple full _recv_buffers to fill the msgContent.
     * So check if msgContent.remaining is larger than 0 and if so, dump everything from _recv_buffer to it
     * @param selectionKey : Used to reference the instance and selector
     * @throws ClosedChannelException :
     */
    private void process_msgContent(SelectionKey selectionKey) throws IOException {

        if (msgContent.remaining() > 0){
            _recv_buffer.flip(); //pos at 0 and limit set to bitsRead set ready to read
            msgContent.put(_recv_buffer);
            _recv_buffer.clear();
        }

        if (msgContent.remaining() == 0){
            // msgContent should now be full and ready to convert to a various model files.
            socketListener.onServerReadSuccess(jsonHeaderRead, msgContent);

            // Clear for next round of communication
            _recv_buffer.clear();
            _jsonheader_len = 0;
            jsonHeaderRead = null;
            msgContent.clear();

            int totalBytes = msgContent.capacity() / 1000000;
            double timeTaken = (System.nanoTime() - socketReadTimeStart) * 10e-10;
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            Log.i(TAG, "Entire message containing " + totalBytes + "Mb recv'd in " + df.format(timeTaken) + "s");

            msgReadComplete = true;

            // Set socket to write now that reading has finished.
            int ops = SelectionKey.OP_WRITE;
            sc.register(selectionKey.selector(), ops, selectionKey.attachment());
        }
    }

    //todo should send this to the mainactivity listener so it can be customized/overridden
    private void onNewMessageFromServer(){
        // Take info from JSONheader to parse msgContent into individual model files

        // After parsing all models notify MainActivity that models have been updated
    }

    // todo should be able deal with ByteBuffer from FlatBuffer rather than byte[]
    public boolean addEpisodeToWriteBuffer(byte[] episode){
        boolean success = false;
        try{
            ByteBuffer bb = ByteBuffer.wrap(episode);
            success = writeBufferVector.add(bb);
            Log.v(TAG, "Added data to writeBuffer");
            int ops = SelectionKey.OP_WRITE;
            socketWriteTimeStart = System.nanoTime();
            sc.register(selector, ops, this);
            // I want this to trigger the selector that this channel is writeReady.
        } catch (NullPointerException | ClosedChannelException e){
            Log.e(TAG,"Error", e);
            Log.e(TAG, "SocketConnectionManager.data not initialized yet");
        }
        return success;
    }
}
