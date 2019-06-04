package jp.oist.abcvlib.basic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class AbcvlibSocketClient implements Runnable{

    private Socket socket;

    private int serverPort = 0;
    private String serverIp = "";

    BufferedWriter bufferedWriter = null;
    BufferedReader bufferedReader = null;

    public AbcvlibSocketClient(String host, int port){
        this.serverIp = host;
        this.serverPort = port;
    }

    @Override
    public void run() {

        try{
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            socket = new Socket(serverAddr, serverPort);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String readText = "";

            while (true){
                String timeStamp =  Long.toString(System.nanoTime());
                bufferedWriter.write("androidSend at time:" + timeStamp);
                bufferedWriter.flush();

                if (bufferedReader.ready()){
                    readText = bufferedReader.readLine();
                }
                Thread.sleep(1000);
            }

        } catch (UnknownHostException | InterruptedException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }
}
