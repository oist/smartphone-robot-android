package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

import jp.oist.abcvlib.core.AbcvlibActivity;


public class SocketClient implements Runnable{

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
    public JSONObject socketMsgIn;
    private AbcvlibActivity abcvlibActivity;
    double currentTime = 0.0;
    double prevTime = 0.0;
    float[] pythonControlTimer = new float[3];
    float[] pythonControlTimeSteps = new float[3];
    int timerCount = 1;
    JSONArray qvalueArray = new JSONArray();
    JSONArray weightArray = new JSONArray();
    int arrayIndex = 0;
    SocketListener socketListener;

    public SocketClient(String host, int port, JSONObject inputs, JSONObject socketMsg,
                        AbcvlibActivity abcvlibActivity, SocketListener socketListener){
        this.serverIp = host;
        this.serverPort = port;
        this.inputs_S = inputs;
        this.controls_S = socketMsg;
        this.abcvlibActivity = abcvlibActivity;
        this.socketListener = socketListener;
    }

    @Override
    public void run() {
        Log.i("abcvlib", "SocketClient.run");
        connect();
        while(!ready) {
            Thread.yield();
        }

        while (abcvlibActivity.switches.pythonControlledPIDBalancer){

            if (abcvlibActivity.switches.timersOn){
                pythonControlTimer[0] = System.nanoTime();
            }

            readControlData();

            if (abcvlibActivity.switches.loggerOn){
                System.out.println(abcvlibActivity.outputs.controls);
            }
            if (abcvlibActivity.switches.timersOn){
                pythonControlTimer[1] = System.nanoTime();
            }

            writeAndroidData();

            if (abcvlibActivity.switches.loggerOn){
                System.out.println(abcvlibActivity.inputs.stateVariables);
            }
            if (abcvlibActivity.switches.timersOn){
                pythonControlTimeSteps[2] += System.nanoTime() - pythonControlTimer[1];
                pythonControlTimeSteps[1] += pythonControlTimer[1] - pythonControlTimer[0];
                pythonControlTimeSteps[0] = pythonControlTimer[0];

                // Take basic stats of every 1000 time step lengths rather than pushing all.
                if (timerCount % abcvlibActivity.avgCount == 0){
                    for (int i=1; i < pythonControlTimeSteps.length; i++){
                        pythonControlTimeSteps[i] = (pythonControlTimeSteps[i] / abcvlibActivity.avgCount) / 1000000;
                    }
                    Log.i("timers", "PythonControlTimer Averages = " + Arrays.toString(pythonControlTimeSteps) + "(ms)");
                }
                timerCount ++;
            }
        }
    }

    public void connect(){

        Log.v("abcvlib", "SocketClient.connect");

        try{
            Log.v("abcvlib", "SocketClient.connectTry");
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            Log.v("abcvlib", "Prior to creating socket. IP:" + serverAddr + ", Port:" + serverPort);
            socket = new Socket(serverAddr, serverPort);
            Log.i("abcvlib", "Created new socket connection");
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        } catch (ConnectException e1){
            try {
                Thread.sleep(1000);
                Log.i("abcvlib", "Waiting on Python server to initialize. Exception:" + e1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        ready = true;
        writePermission = true;
        readPermission = false;
//        Log.v("abcvlib", "SocketClient3");

    }

    /**
     * Default PID parameter exchange helper function. Core read method @getControlsFromServer
     */
    private void readControlData(){
        abcvlibActivity.outputs.setControls(getControlsFromServer());
        if (abcvlibActivity.switches.timersOn){
            try {
                currentTime = Double.parseDouble(abcvlibActivity.outputs.controls.get("timeServer").toString());
                double dt = currentTime - prevTime;
                Log.i("abcvlib_timers", "dt=" + dt);
                prevTime = currentTime;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Default PID parameter exchange helper function. Core write method @writeInputsToServer
     */
    private void writeAndroidData(){

        try {
            abcvlibActivity.inputs.stateVariables.put("timeAndroid", System.nanoTime() / 1000000000.0);
            if (abcvlibActivity.inputs.motionSensors != null){
                abcvlibActivity.inputs.stateVariables.put("theta", abcvlibActivity.inputs.motionSensors.getThetaDeg());
                abcvlibActivity.inputs.stateVariables.put("thetaDot", abcvlibActivity.inputs.motionSensors.getThetaDegDot());
            }
            if (abcvlibActivity.inputs.quadEncoders != null){
                abcvlibActivity.inputs.stateVariables.put("wheelCountL", abcvlibActivity.inputs.quadEncoders.getWheelCountL());
                abcvlibActivity.inputs.stateVariables.put("wheelCountR", abcvlibActivity.inputs.quadEncoders.getWheelCountR());
                abcvlibActivity.inputs.stateVariables.put("wheelDistanceL", abcvlibActivity.inputs.quadEncoders.getDistanceL());
                abcvlibActivity.inputs.stateVariables.put("wheelDistanceR", abcvlibActivity.inputs.quadEncoders.getDistanceR());
                abcvlibActivity.inputs.stateVariables.put("wheelSpeedL", abcvlibActivity.inputs.quadEncoders.getWheelSpeedL());
                abcvlibActivity.inputs.stateVariables.put("wheelSpeedR", abcvlibActivity.inputs.quadEncoders.getWheelSpeedR());
                abcvlibActivity.inputs.stateVariables.put("wheelSpeedL_LP", abcvlibActivity.inputs.quadEncoders.getWheelSpeedL_LP());
                abcvlibActivity.inputs.stateVariables.put("wheelSpeedR_LP", abcvlibActivity.inputs.quadEncoders.getWheelSpeedR_LP());
                abcvlibActivity.inputs.stateVariables.put("expWeight", abcvlibActivity.inputs.quadEncoders.getExpWeight());
            }
            if (abcvlibActivity.aD != null){
                abcvlibActivity.inputs.stateVariables.put("action", abcvlibActivity.aD.getSelectedAction());

                arrayIndex = 0;
                for (double qvalue : abcvlibActivity.aD.getqValues()){
                    qvalueArray.put(arrayIndex, qvalue);
                    arrayIndex++;
                }
                arrayIndex = 0;
                for (double weight : abcvlibActivity.aD.getWeights()){
                    weightArray.put(arrayIndex, weight);
                    arrayIndex++;
                }

                abcvlibActivity.inputs.stateVariables.put("weights", weightArray);
                abcvlibActivity.inputs.stateVariables.put("qvalues", qvalueArray);

                abcvlibActivity.inputs.stateVariables.put("reward", abcvlibActivity.aD.getReward());
            }
            if (abcvlibActivity.inputs.micInput != null){
                abcvlibActivity.inputs.stateVariables.put("rms", abcvlibActivity.inputs.micInput.getRms());
                abcvlibActivity.inputs.stateVariables.put("rmsdB", abcvlibActivity.inputs.micInput.getRmsdB());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        writeInputsToServer(abcvlibActivity.inputs.stateVariables);
    }

    public JSONObject getControlsFromServer(){

        int timeout = 50;
        int timeoutCounter = 0;

        try {
            while (bufferedReader == null){
                Log.v("abcvlib", "bufferedReader == null");
                Thread.sleep(100);
            }
            while (!bufferedReader.ready()){
                if (timeoutCounter >= timeout){
                    Log.v("abcvlib", "timeout counter exceeded. Assuming socket server closed. Retrying connection");
                    closeAll();
                    connect();
                }
//                Log.v("abcvlib", "bufferedReader not ready @getControlsFromServer");
                Thread.sleep(100);
                timeoutCounter++;
            }
            while ((line = bufferedReader.readLine()) == null){
                Log.v("abcvlib", "bufferedReader line is null");
            }
            socketMsgIn = new JSONObject(line);
//            Log.v("abcvlib", "wrote to socketMsgIn");
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

        if (socketListener != null){
            socketListener.onServerReadSuccess(socketMsgIn);
        }

        return socketMsgIn;
    }

    public void writeInputsToServer(JSONObject inputs){

        String timeStamp =  Long.toString(System.nanoTime());

        try{
            bufferedWriter.write(inputs.toString());
//            Log.v("abcvlib", "Wrote inputs to server");
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

    private void closeAll() throws IOException {
        ready = false;
        bufferedReader.close();
        bufferedWriter.close();
        socket.close();
    }

}

