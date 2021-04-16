package jp.oist.abcvlib.util;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.Vector;

public class SocketConnectionManager implements Runnable{

    private SocketChannel sc;
    private Selector selector;
    private int events;
    private final String TAG = "SocketConnectionManager";
    private SocketListener socketListener;
    private SocketMessage socketMessage;
    private String serverIp;
    private int serverPort;

    public SocketConnectionManager(SocketListener socketListener, String serverIp, int serverPort){
        this.socketListener = socketListener;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            start_connection(serverIp, serverPort);
            do {
//                Log.i(TAG, "selector.select");
                events = selector.select(); // events is int representing how many keys are ready
                if (events > 0){
                    Log.i(TAG, "events > 0");
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    for (SelectionKey selectedKey : selectedKeys){
                        try{
                            SocketMessage socketMessage = (SocketMessage) selectedKey.attachment();
                            socketMessage.process_events(selectedKey);
                        }catch (ClassCastException e){
                            e.printStackTrace();
                            Log.e(TAG, "selectedKey attachment not a SocketMessage type");
                        }
                    }
                }
            } while (selector.isOpen()); //todo remember to close the selector somewhere

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start_connection(String serverIp, int serverPort){
        try {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(serverIp, serverPort);
            sc = SocketChannel.open();
            sc.configureBlocking(false);
            int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
            socketMessage = new SocketMessage(selector, sc, inetSocketAddress);
            sc.register(selector, ops, socketMessage);
            sc.connect(inetSocketAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsgToServer(byte[] episode){
        socketMessage.addEpisodeToWriteBuffer(episode);
    }

    /**
     * Should be called prior to exiting app to ensure zombie threads don't remain in memory.
     */
    public void close(){
        try {
            Log.v(TAG, "Closing connection: " + sc.getRemoteAddress());
            selector.close();
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





}
