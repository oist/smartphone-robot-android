package jp.oist.abcvlib.serverlearning;

import android.os.Bundle;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.serverlearning.gatherers.BatteryDataGatherer;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.FileOps;

public class MainActivity extends AbcvlibActivity {

    TimeStepDataAssembler timeStepDataAssembler;
    InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.27.226", 3000);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        FileOps.copyAssets(getApplicationContext(), "models/");

        // Setup a live preview of camera feed to the display. Remove if unwanted.
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        switches.cameraXApp = true;

        timeStepDataAssembler = new TimeStepDataAssembler(this, inetSocketAddress);
        timeStepDataAssembler.initializeGatherers();

        initialzer(this, null, this, timeStepDataAssembler.getBatteryDataGatherer());

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSetupFinished() {
        try {
            timeStepDataAssembler.startGatherers();
        } catch (InterruptedException e) {
            ErrorHandler.eLog(TAG, "Error starting gathers", e, true);
        }
    }

    // Passes custom ImageAnalysis object to core CameraX lib to bind to lifecycle, and other admin functions
    @Override
    public ImageAnalysis getAnalyzer() {
        return timeStepDataAssembler.getImageAnalysis();
    }
    
    @Override
    protected void onNewAudioData(float[] audioData, int numSamples){
        timeStepDataAssembler.getTimeStepDataBuffer().getWriteData().getSoundData().add(audioData, numSamples);
    }

    @Override
    public void onServerReadSuccess(JSONObject jsonHeader, ByteBuffer msgFromServer) {
        // Parse whatever you sent from python here
        //loadMappedFile...
        try {
            if (jsonHeader.get("content-encoding").equals("modelVector")){
                Log.d(TAG, "Writing model files to disk");
                JSONArray modelNames = (JSONArray) jsonHeader.get("model-names");
                JSONArray modelLengths = (JSONArray) jsonHeader.get("model-lengths");

                msgFromServer.flip();

                for (int i = 0; i < modelNames.length(); i++){
                    byte[] bytes = new byte[modelLengths.getInt(i)];
                    msgFromServer.get(bytes);
                    FileOps.savedata(this, bytes, "models", modelNames.getString(i) + ".tflite");
                }
            }else{
                Log.d(TAG, "Data from server does not contain modelVector content. Be sure to set content-encoding to \"modelVector\" in the python jsonHeader");
            }
        } catch (JSONException e) {
            ErrorHandler.eLog(TAG, "Something wrong with parsing the JSONheader from python", e, true);
        }
    }
}