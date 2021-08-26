package jp.oist.abcvlib.basicassembler;

import android.Manifest;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.PermissionsListener;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
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
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework. This MainActivity class
 * does not implement the various listener interfaces in order to subscribe to updates from various
 * sensor data, but instead sets up a custom
 * {@link jp.oist.abcvlib.core.learning.Trial} object that handles setting up the
 * subscribers and assembles the data into a {@link jp.oist.abcvlib.core.inputs.TimeStepDataBuffer}
 * comprising of multiple {@link jp.oist.abcvlib.core.inputs.TimeStepDataBuffer.TimeStepData} objects
 * that each represent all the data gathered from one or more sensors over the course of one timestep
 *
 * Optional commented out lines in each listener method show how to write the data to the Android
 * logcat log. As these occur VERY frequently (tens of microseconds) this spams the logcat and such
 * I have reserved them only for when necessary. The updates to the GUI via the GuiUpdate object
 * are intentionally delayed or sampled every 100 ms so as not to spam the GUI thread and make it
 * unresponsive.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements PermissionsListener, IOReadyListener {

    private final String TAG = getClass().getName();
    private GuiUpdater guiUpdater;
    private int maxEpisodeCount = 10;
    private int maxTimeStepCount = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup Android GUI object references such that we can write data to them later.
        setContentView(R.layout.activity_main);

        setIoReadyListener(this);

        // Creates an another thread that schedules updates to the GUI every 100 ms. Updaing the GUI every 100 microseconds would bog down the CPU
        ScheduledExecutorServiceWithException executor = new ScheduledExecutorServiceWithException(1, new ProcessPriorityThreadFactory(Thread.MIN_PRIORITY, "GuiUpdates"));
        guiUpdater = new GuiUpdater(this, maxTimeStepCount, maxEpisodeCount);
        executor.scheduleAtFixedRate(guiUpdater, 0, 100, TimeUnit.MILLISECONDS);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onIOReady() {
        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        checkPermissions(this, permissions);
    }

    @Override
    public void onPermissionsGranted(){
       /*------------------------------------------------------------------------------
        ------------------------------ Set MetaParameters ------------------------------
        --------------------------------------------------------------------------------
         */
        MetaParameters metaParameters = new MetaParameters(this, 100, maxTimeStepCount,
                100, maxEpisodeCount, null, getTimeStepDataBuffer());

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

        BatteryData batteryData = new BatteryData(getTimeStepDataBuffer());

        OrientationData orientationData = new OrientationData(getTimeStepDataBuffer(), this);

        MicrophoneData microphoneData = new MicrophoneData(getTimeStepDataBuffer());
        microphoneData.start();
        // Add the reference to your ArrayList

        ImageData imageData = new ImageData(getTimeStepDataBuffer(), findViewById(R.id.camera_x_preview), null);
        imageData.startCamera(this, this);

        // Initialize an ArrayList of AbcvlibInputs that you want to gather data for
        ArrayList<AbcvlibInput> inputs = new ArrayList<>();
        inputs.add(wheelData);
        inputs.add(batteryData);
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
        MyTrial myTrial = new MyTrial(this, guiUpdater, metaParameters, actionSpace, stateSpace);
        myTrial.startTrail();
    }
}

