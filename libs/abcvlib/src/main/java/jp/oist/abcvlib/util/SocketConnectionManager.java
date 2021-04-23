package jp.oist.abcvlib.util;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class SocketConnectionManager implements Runnable{

    private SocketChannel sc;
    private Selector selector;
    private SocketListener socketListener;
    private final String TAG = "SocketConnectionManager";
    private SocketMessage socketMessage;
    private final String serverIp;
    private final int serverPort;

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
                int eventCount = selector.select(0);
                Set<SelectionKey> events = selector.selectedKeys(); // events is int representing how many keys have changed state
                if (eventCount != 0){
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    for (SelectionKey selectedKey : selectedKeys){
                        try{
                            SocketMessage socketMessage = (SocketMessage) selectedKey.attachment();
                            socketMessage.process_events(selectedKey);
                            selectedKeys.remove(selectedKey);
                        }catch (ClassCastException e){
                            Log.e(TAG,"Error", e);
                            Log.e(TAG, "selectedKey attachment not a SocketMessage type");
                        }
                    }
                }
            } while (selector.isOpen()); //todo remember to close the selector somewhere

        } catch (IOException e) {
            Log.e(TAG,"Error", e);
        }
    }

    private void start_connection(String serverIp, int serverPort){
        try {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(serverIp, serverPort);
            sc = SocketChannel.open();
            sc.configureBlocking(false);
            socketMessage = new SocketMessage(socketListener, sc, selector);

            Log.d(TAG, "Initializing connection with " + inetSocketAddress);
            boolean connected = sc.connect(inetSocketAddress);
            Log.v(TAG, "socketChannel.isConnected ? : " + sc.isConnected());

            Log.v(TAG, "registering with selector to connect");
            int ops = SelectionKey.OP_CONNECT;
            sc.register(selector, ops, socketMessage);

        } catch (IOException | ClosedSelectorException | IllegalBlockingModeException
                | CancelledKeyException | IllegalArgumentException e) {
            Log.e(TAG, "Initial socket connect and registration:", e);
        }
    }

    public void sendMsgToServer(byte[] episode){
        boolean writeSuccess = socketMessage.addEpisodeToWriteBuffer(episode);
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
            Log.e(TAG,"Error", e);
        }
    }





}
