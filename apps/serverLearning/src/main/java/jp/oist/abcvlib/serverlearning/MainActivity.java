package jp.oist.abcvlib.serverlearning;

import android.os.Bundle;

import java.net.InetSocketAddress;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.learning.ActionSpace;
import jp.oist.abcvlib.core.learning.CommActionSpace;
import jp.oist.abcvlib.core.learning.MetaParameters;
import jp.oist.abcvlib.core.learning.MotionActionSpace;
import jp.oist.abcvlib.core.learning.StateSpace;
import jp.oist.abcvlib.util.FileOps;

public class MainActivity extends AbcvlibActivity implements IOReadyListener{

    InetSocketAddress inetSocketAddress = new InetSocketAddress(BuildConfig.HOST, BuildConfig.PORT);
    @SuppressWarnings("unused")
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
    public void onIOReady(AbcvlibLooper abcvlibLooper) {
        /*------------------------------------------------------------------------------
        ------------------------------ Set MetaParameters ------------------------------
        --------------------------------------------------------------------------------
         */
        TimeStepDataBuffer timeStepDataBuffer = new TimeStepDataBuffer(10);
        MetaParameters metaParameters = new MetaParameters(this, 10, 1,
                100000, 1000, inetSocketAddress, timeStepDataBuffer, getOutputs(), 1);

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
        PublisherManager publisherManager = new PublisherManager();
        // Customize how each sensor data is gathered via constructor params or builders
        WheelData wheelData = new WheelData.Builder(this, publisherManager, abcvlibLooper)
                .setBufferLength(50)
                .setExpWeight(0.01)
                .build();
        wheelData.addSubscriber(timeStepDataBuffer);

        BatteryData batteryData = new BatteryData.Builder(this, publisherManager, abcvlibLooper).build();
        batteryData.addSubscriber(timeStepDataBuffer);

        OrientationData orientationData = new OrientationData.Builder(this, publisherManager).build();
        orientationData.addSubscriber(timeStepDataBuffer);

        MicrophoneData microphoneData = new MicrophoneData.Builder(this, publisherManager).build();
        microphoneData.addSubscriber(timeStepDataBuffer);

        ImageData imageData = new ImageData.Builder(this, publisherManager, this)
                .setPreviewView(findViewById(R.id.camera_x_preview)).build();
        imageData.addSubscriber(timeStepDataBuffer);

        StateSpace stateSpace = new StateSpace(publisherManager);
        /*------------------------------------------------------------------------------
        ----------- Initialize and Start Trial After PublisherManager Ready ----------------------
        --------------------------------------------------------------------------------
         */
        MyTrial myTrial = new MyTrial(metaParameters, actionSpace, stateSpace);
        myTrial.startTrail();
    }
}