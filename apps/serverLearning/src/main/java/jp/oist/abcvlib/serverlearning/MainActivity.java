package jp.oist.abcvlib.serverlearning;

import android.Manifest;
import android.os.Bundle;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.PermissionsListener;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.learning.ActionSpace;
import jp.oist.abcvlib.core.learning.CommActionSpace;
import jp.oist.abcvlib.core.learning.MetaParameters;
import jp.oist.abcvlib.core.learning.MotionActionSpace;
import jp.oist.abcvlib.core.learning.StateSpace;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.FileOps;

public class MainActivity extends AbcvlibActivity implements IOReadyListener, PermissionsListener{

    InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.31.178", 3000);
    private final String TAG = getClass().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup a live preview of camera feed to the display. Remove if unwanted.
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        setIoReadyListener(this);

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
        /*------------------------------------------------------------------------------
        ------------------------------ Set MetaParameters ------------------------------
        --------------------------------------------------------------------------------
         */
        MetaParameters metaParameters = new MetaParameters(this, 1000, 5,
                100, 3, inetSocketAddress, getTimeStepDataBuffer());

        /*------------------------------------------------------------------------------
        ------------------------------ Define Action Space -----------------------------
        --------------------------------------------------------------------------------
         */
        // Defining custom actions
        CommActionSpace commActionSpace = new CommActionSpace(3);
        commActionSpace.addCommAction("action1", (byte) 0); // I'm just overwriting an existing to show how
        commActionSpace.addCommAction("action2", (byte) 1);
        commActionSpace.addCommAction("action3", (byte) 2);

        MotionActionSpace motionActionSpace = new MotionActionSpace(5);
        motionActionSpace.addMotionAction("stop", (byte) 0, 0, 0); // I'm just overwriting an existing to show how
        motionActionSpace.addMotionAction("forward", (byte) 1, 100, 100);
        motionActionSpace.addMotionAction("backward", (byte) 2, -100, 100);
        motionActionSpace.addMotionAction("left", (byte) 3, -100, 100);
        motionActionSpace.addMotionAction("right", (byte) 4, 100, -100);

        ActionSpace actionSpace = new ActionSpace(commActionSpace, motionActionSpace);

        /*------------------------------------------------------------------------------
        ------------------------------ Define State Space ------------------------------
        --------------------------------------------------------------------------------
         */
        // Customize how each sensor data is gathered via constructor params or builders
        WheelData wheelData = new WheelData.Builder()
                .setTimeStepDataBuffer(getTimeStepDataBuffer())
                .setBufferLength(50)
                .setExpWeight(0.01)
                .build();

        // You can also just use the default ones if you're not interested in customizing them via:
        WheelData wheelData1 = getInputs().getWheelData();

        OrientationData orientationData = new OrientationData(getTimeStepDataBuffer(), this);

        MicrophoneData microphoneData = new MicrophoneData(getTimeStepDataBuffer());
        microphoneData.start();
        // Add the reference to your ArrayList

        ImageData imageData = new ImageData(getTimeStepDataBuffer(), findViewById(R.id.camera_x_preview), null);
        imageData.startCamera(this, this);

        // Initialize an ArrayList of AbcvlibInputs that you want to gather data for
        ArrayList<AbcvlibInput> inputs = new ArrayList<>();
        inputs.add(wheelData);
        inputs.add(orientationData);
        inputs.add(microphoneData);
        inputs.add(imageData);

        StateSpace stateSpace = new StateSpace(inputs);

        /* Overwrites the default sensor data objects with the ones listed in inputs. If you don't do
         * this AbcvlibLooper will not refer to the correct instances and IOIO sensor data will not
         * be published to your gatherers
         */
        try {
            getInputs().overwriteDefaults(inputs, abcvlibLooper);
        } catch (ClassNotFoundException e) {
            ErrorHandler.eLog(TAG, "", e, true);
        }

        /*------------------------------------------------------------------------------
        ------------------------------ Initialize and Start Trial ----------------------
        --------------------------------------------------------------------------------
         */
        MyTrial myTrial = new MyTrial(metaParameters, actionSpace, stateSpace);
        myTrial.startTrail();
    }
}