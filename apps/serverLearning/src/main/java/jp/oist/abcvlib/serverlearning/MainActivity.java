package jp.oist.abcvlib.serverlearning;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.PermissionsListener;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.learning.CommAction;
import jp.oist.abcvlib.core.learning.CommActionSet;
import jp.oist.abcvlib.core.learning.MotionAction;
import jp.oist.abcvlib.core.learning.MotionActionSet;
import jp.oist.abcvlib.core.learning.TimeStepDataAssembler;
import jp.oist.abcvlib.core.outputs.ActionSelector;
import jp.oist.abcvlib.core.outputs.StepHandler;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.FileOps;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;
import jp.oist.abcvlib.util.SocketListener;

public class MainActivity extends AbcvlibActivity implements PermissionsListener, ActionSelector,
        SocketListener {

    InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.27.226", 3000);
    private StepHandler myStepHandler;
    private int reward = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup a live preview of camera feed to the display. Remove if unwanted.
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        // Copies all files from assets/models to local storage
        FileOps.copyAssets(getApplicationContext(), "models/");

        super.onCreate(savedInstanceState);

        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        checkPermissions(this, permissions);
    }

    @Override
    public void onPermissionsGranted() {

        // Defining custom actions
        CommActionSet commActionSet = new CommActionSet(3);
        commActionSet.addCommAction("action1", (byte) 0); // I'm just overwriting an existing to show how
        commActionSet.addCommAction("action2", (byte) 1);
        commActionSet.addCommAction("action3", (byte) 2);

        MotionActionSet motionActionSet = new MotionActionSet(5);
        motionActionSet.addMotionAction("stop", (byte) 0, 0, 0); // I'm just overwriting an existing to show how
        motionActionSet.addMotionAction("forward", (byte) 1, 100, 100);
        motionActionSet.addMotionAction("backward", (byte) 2, -100, 100);
        motionActionSet.addMotionAction("left", (byte) 3, -100, 100);
        motionActionSet.addMotionAction("right", (byte) 4, 100, -100);

        myStepHandler = new StepHandler.StepHandlerBuilder()
                .setMaxTimeStepCount(20)
                .setMaxEpisodeCount(3)
                .setMaxReward(100000)
                .setMotionActionSet(motionActionSet)
                .setCommActionSet(commActionSet)
                .setActionSelector(this)
                .build();

        // Initialize an ArrayList of AbcvlibInputs that you want the TimeStepDataAssembler to gather data for
        ArrayList<AbcvlibInput> inputs = new ArrayList<>();

        // Get local reference to MicrophoneData and start the record buffer (within MicrophoneData Class)
        MicrophoneData microphoneData = getInputs().getMicrophoneData();
        microphoneData.start();
        // Add the reference to your ArrayList
        inputs.add(microphoneData);

        ImageData imageData = getInputs().getImageData();
        imageData.setPreviewView(findViewById(R.id.camera_x_preview));
        imageData.startCamera(this, this);
        inputs.add(imageData);

        // Pass your inputs list to a new instance of TimeStepDataAssember along with all other references
        TimeStepDataAssembler timeStepDataAssembler = new TimeStepDataAssembler(inputs, myStepHandler, inetSocketAddress, this, getInputs().getTimeStepDataBuffer());
        try {
            timeStepDataAssembler.startGatherers();
        } catch (RecordingWithoutTimeStepBufferException e) {
            ErrorHandler.eLog(TAG, "Make sure to initialize a TimeStepDataBuffer object prior " +
                    "to setting isRecording to true", e, true);
        }
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
            // Do anything you need to wrap up an episode here. The episode count and timeStep counts will automatically be incremented or set to zero as necessary.

            // reseting reward after each episode
            reward = 0;
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