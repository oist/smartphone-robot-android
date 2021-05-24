package jp.oist.abcvlib.serverlearning;

import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.learning.CommAction;
import jp.oist.abcvlib.core.learning.CommActionSet;
import jp.oist.abcvlib.core.learning.MotionAction;
import jp.oist.abcvlib.core.learning.MotionActionSet;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataAssembler;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.core.outputs.ActionSelector;
import jp.oist.abcvlib.core.outputs.StepHandler;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.FileOps;

public class MainActivity extends AbcvlibActivity implements ActionSelector {

    InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.27.226", 3000);
    private StepHandler myStepHandler;
    private int reward = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        CommActionSet commActionSet = new CommActionSet(3);
        commActionSet.addCommAction("action1", (byte) 0); // I'm just overwriting an existing to show how
        MotionActionSet motionActionSet = new MotionActionSet(5);
        motionActionSet.addMotionAction("stop", (byte) 0, 0, 0); // I'm just overwriting an existing to show how

        myStepHandler = new StepHandler.StepHandlerBuilder()
                .setMaxTimeStepCount(20)
                .setMaxEpisodeCount(3)
                .setMaxReward(100000)
                .setMotionActionSet(motionActionSet)
                .setCommActionSet(commActionSet)
                .buildStepHandler();

        myStepHandler.setActionSelector(this);

        FileOps.copyAssets(getApplicationContext(), "models/");

        // Setup a live preview of camera feed to the display. Remove if unwanted.
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        TimeStepDataAssembler timeStepDataAssembler = new TimeStepDataAssembler(this, inetSocketAddress, myStepHandler);

        ArrayList<AbcvlibInput> inputArrayList = new ArrayList<>();
        ImageData imageData = new ImageData(this);
        MicrophoneData microphoneData = new MicrophoneData(this);
        inputArrayList.add(imageData);
        inputArrayList.add(microphoneData);

        initializer(this, null, timeStepDataAssembler, inputArrayList, null);

        super.onCreate(savedInstanceState);
    }

    @Override
    public ActionSet forward(TimeStepDataBuffer.TimeStepData data, int timeStepCount) {
        ActionSet actionSet;
        MotionAction motionAction;
        CommAction commAction;

        // Set actions based on above results. e.g: the first index of each
        motionAction = myStepHandler.getMotionActionSet().getMotionActions()[0];
        commAction = myStepHandler.getCommActionSet().getCommActions()[0];

        // Bundle them into ActionSet so it can return both
        actionSet = new ActionSet(motionAction, commAction);

        // set your action to some ints
        data.getActions().add(motionAction, commAction);

        if (timeStepCount >= myStepHandler.getMaxTimeStepCount() || (reward >= myStepHandler.getMaxReward())){
            myStepHandler.setLastTimestep(true);
            myStepHandler.incrementEpisodeCount();
            // reseting reward after each episode
            reward = 0;
        }

        if (myStepHandler.getEpisodeCount() >= myStepHandler.getMaxEpisodecount()){
            myStepHandler.setLastTimestep(true);
        }

        return actionSet;
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