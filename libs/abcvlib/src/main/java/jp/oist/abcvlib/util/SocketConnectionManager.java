package jp.oist.abcvlib.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

public class SocketConnectionManager {

    SocketChannel sc;
    Selector selector;

    public void start_connections(String serverIp, int serverPort){
        try {
            selector = Selector.open();
            sc = SocketChannel.open();
            int events = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
//            sc.register(selector, events, )
            InetSocketAddress inetSocketAddress = new InetSocketAddress(serverIp, serverPort);
            sc.connect(inetSocketAddress);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeFlatBufferToServer(byte[] byteArray){
        try {
            sc.write(ByteBuffer.wrap(byteArray));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class data{
        private int connid;
        private int msg_total;
        private int recv_total;
        private ByteBuffer outb;

        public data(int connid, List<byte[]> messages){
            this.connid = connid;
            msg_total = messages.size();
        }
    }



}
