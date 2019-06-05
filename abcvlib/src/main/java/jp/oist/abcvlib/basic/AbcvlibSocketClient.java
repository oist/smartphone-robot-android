package jp.oist.abcvlib.basic;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class AbcvlibSocketClient implements Runnable{

    private Socket socket;

    private int serverPort = 0;
    private String serverIp = "";
    public JSONObject inputs_S = null;
    public JSONObject controls_S = null;

    BufferedWriter bufferedWriter = null;
    BufferedReader bufferedReader = null;

    public AbcvlibSocketClient(String host, int port, JSONObject inputs, JSONObject controls){
        this.serverIp = host;
        this.serverPort = port;
        this.inputs_S = inputs;
        this.controls_S = controls;
    }

    @Override
    public void run() {
        connect();
    }

    public void connect(){
        try{
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            socket = new Socket(serverAddr, serverPort);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public JSONObject getControlsFromServer() throws NullPointerException{

        String line = "";
        JSONObject controls = null;

        try {
            while (!bufferedReader.ready()){
                wait(1000);
            }
            while ((line = bufferedReader.readLine()) == null){
                wait(1000);
            }
            controls = new JSONObject(line);
        } catch (IOException | JSONException | InterruptedException e1) {
            e1.printStackTrace();
        }

        return controls;
    }

    public void writeInputsToServer(JSONObject inputs){

        String timeStamp =  Long.toString(System.nanoTime());

        try{
            bufferedWriter.write(inputs.toString());
            bufferedWriter.flush();
        } catch (IOException e){
            e.printStackTrace();
        }

    }

}
