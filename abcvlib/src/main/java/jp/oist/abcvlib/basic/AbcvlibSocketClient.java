package jp.oist.abcvlib.basic;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;

public class AbcvlibSocketClient implements Runnable{

    private Socket socket;

    private int serverPort = 0;
    private String serverIp = "";
    public JSONObject inputs_S = null;
    public JSONObject controls_S = null;

    BufferedWriter bufferedWriter = null;
    BufferedReader bufferedReader = null;

    public boolean ready = false;
    public boolean readPermission = false;
    public boolean writePermission = false;

    private String line = "";

    private JSONObject controls;

    public AbcvlibSocketClient(String host, int port, JSONObject inputs, JSONObject controls){
        this.serverIp = host;
        this.serverPort = port;
        this.inputs_S = inputs;
        this.controls_S = controls;
    }

    @Override
    public void run() {
        initializeControls();
        connect();
    }

    public void connect(){
        try{
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            socket = new Socket(serverAddr, serverPort);
            Log.i("abcvlib", "Created new socket connection");
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        } catch (ConnectException e1){
            try {
                Thread.sleep(1000);
                Log.i("abcvlib", "Waiting on Python server to initialize");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        ready = true;
        writePermission = true;
        readPermission = false;
    }

    public JSONObject getControlsFromServer(){

        int timeout = 50;
        int timeoutCounter = 0;

        try {
            while (bufferedReader == null){
//                Log.i("abcvlib", "bufferedReader == null");
            }
            while (!bufferedReader.ready()){
                if (timeoutCounter >= timeout){
                    Log.i("abcvlib", "timeout counter exceeded. Assuming socket server closed. Retrying connection");
                    closeAll();
                    connect();
                }
                Log.i("abcvlib", "bufferedReader not ready @getControlsFromServer");
                Thread.sleep(100);
                timeoutCounter++;
            }
            while ((line = bufferedReader.readLine()) == null){
                Log.i("abcvlib", "bufferedReader line is null");
            }
            controls = new JSONObject(line);
        }

        catch (NullPointerException e2){
            Log.i("abcvlib", "bufferedReader still null. Trying to reconnect to socket");
            e2.printStackTrace();}

        catch (IOException | JSONException e3) {
            try {
                Thread.sleep(1000);
                connect();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            e3.printStackTrace();
        }

        catch (InterruptedException e4) {
            e4.printStackTrace();
        }

        writePermission = true;
        readPermission = false;

        return controls;
    }

    public void writeInputsToServer(JSONObject inputs){

        String timeStamp =  Long.toString(System.nanoTime());

        try{
            bufferedWriter.write(inputs.toString());
            bufferedWriter.flush();
        } catch (NullPointerException e1){
            Log.i("abcvlib", "bufferedWriter still null. Trying to reconnect to socket");
            e1.printStackTrace();
        } catch (IOException e2){
            try {
                Thread.sleep(1000);
                connect();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            e2.printStackTrace();
        }

        writePermission = false;
        readPermission = true;

    }

    private void initializeControls(){
        String jsonString = "{\"timeServer\": 0, \"k_p\": 0, \"k_i\": 0, \"k_d\": 0, \"setPoint\": 0, \"wheelSpeedL\": 0, \"wheelSpeedR\": 0}";
        try {
            controls = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void closeAll() throws IOException {
        ready = false;
        bufferedReader.close();
        bufferedWriter.close();
        socket.close();
    }
}
