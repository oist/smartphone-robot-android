package jp.oist.abcvlib.serverlearning;

import android.Manifest;
import android.os.Bundle;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.PermissionsListener;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.learning.CommActionSet;
import jp.oist.abcvlib.core.learning.MotionActionSet;
import jp.oist.abcvlib.core.learning.TimeStepDataAssembler;
import jp.oist.abcvlib.core.outputs.StepHandler;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.FileOps;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;

public class MainActivity extends AbcvlibActivity implements IOReadyListener, PermissionsListener{

    InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.27.231", 3000);
    private MyStepHandler myStepHandler;
    private int reward = 0;
    private String TAG = getClass().toString();
    private ServerComm serverComm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup a live preview of camera feed to the display. Remove if unwanted.
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        // Initialize server socket listener
        serverComm = new ServerComm(this);

        // Copies all files from assets/models to local storage
        FileOps.copyAssets(getApplicationContext(), "models/");

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onIOReady() {
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

        myStepHandler = (MyStepHandler) new MyStepHandler()
                .setMaxTimeStepCount(20)
                .setMaxEpisodeCount(3)
                .setMaxReward(100000)
                .setMotionActionSet(motionActionSet)
                .setCommActionSet(commActionSet);

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
        TimeStepDataAssembler timeStepDataAssembler = new TimeStepDataAssembler(inputs, myStepHandler, inetSocketAddress, serverComm, getInputs().getTimeStepDataBuffer());
        try {
            timeStepDataAssembler.startGatherers();
        } catch (RecordingWithoutTimeStepBufferException e) {
            ErrorHandler.eLog(TAG, "Make sure to initialize a TimeStepDataBuffer object prior " +
                    "to setting isRecording to true", e, true);
        }
    }
}