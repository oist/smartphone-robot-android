package jp.oist.abcvlib.basic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class AbcvlibSocketClient implements Runnable{

    private Socket socket;

    private int serverPort = 0;
    private String serverIp = "";
    public HashMap<String, Double> inputsSocket = null;
    public HashMap<String, Double> controlsSocket = null;

    BufferedWriter bufferedWriter = null;
    BufferedReader bufferedReader = null;

    public AbcvlibSocketClient(String host, int port, HashMap<String, Double> inputs, HashMap<String, Double> controls){
        this.serverIp = host;
        this.serverPort = port;
        this.inputsSocket = inputs;
        this.controlsSocket = controls;
    }

    @Override
    public void run() {

        try{
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            socket = new Socket(serverAddr, serverPort);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true){
                if (inputsSocket != null){
                    writeInputs();
                }
                controlsSocket = getControls();
                Thread.sleep(1000);
            }

        } catch (UnknownHostException | InterruptedException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    public void setInputsSocket(HashMap<String, Double> input){
        this.inputsSocket = input;
    }

    public HashMap<String, Double> getControlsSocket(){
        return this.controlsSocket;
    }

    private HashMap<String, Double> getControls(){

        HashMap<String, Double> controls = null;
        String readText = "";

        try {
            if (bufferedReader.ready()){
                readText = bufferedReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return controls;
    }

    private void writeInputs(){

        String timeStamp =  Long.toString(System.nanoTime());

        try{
            String inputsAll = printHashMap(inputsSocket);
            bufferedWriter.write("time:" + timeStamp + " " + inputsAll);
            bufferedWriter.flush();
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    private String printHashMap(HashMap<String, Double> dictionary){
        String output = "";
        for (String name: dictionary.keySet()){
            String key = name.toString();
            String value = dictionary.get(name).toString();
            output = output + " key:" + key + " value:" + value;
        }

        return output;
    }
}
