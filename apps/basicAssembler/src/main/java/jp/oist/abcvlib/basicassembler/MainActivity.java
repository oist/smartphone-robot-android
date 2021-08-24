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
import jp.oist.abcvlib.core.learning.CommActionSet;
import jp.oist.abcvlib.core.learning.MotionActionSet;
import jp.oist.abcvlib.core.learning.TimeStepDataAssembler;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework. This MainActivity class
 * does not implement the various listener interfaces in order to subscribe to updates from various
 * sensor data, but instead sets up a custom
 * {@link jp.oist.abcvlib.core.learning.TimeStepDataAssembler} object that handles setting up the
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

    private GuiUpdater guiUpdater;
    private final String TAG = getClass().getName();
    private final int maxTimeStepCount = 100;
    private final int maxEpisodeCount = 10;

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

        MyTrial myTrial = (MyTrial) new MyTrial(this, guiUpdater)
                .setTimeStepLength(100)
                .setMaxTimeStepCount(maxTimeStepCount)
                .setMaxEpisodeCount(maxEpisodeCount)
                .setMaxReward(100000)
                .setMotionActionSet(motionActionSet)
                .setCommActionSet(commActionSet);

        // Initialize an ArrayList of AbcvlibInputs that you want the TimeStepDataAssembler to gather data for
        ArrayList<AbcvlibInput> inputs = new ArrayList<>();

        // reusing the same timeStepDataBuffer shared by all. You could alternatively create a new
        // one or extend it in another class.
        BatteryData batteryData = new BatteryData(this.getInputs().getTimeStepDataBuffer());
        // bufferLength and exptWeight set how the speed is calculated.
        WheelData wheelData = new WheelData.Builder()
                .setTimeStepDataBuffer(this.getInputs().getTimeStepDataBuffer())
                .setBufferLength(50)
                .setExpWeight(0.01)
                .build();
        OrientationData orientationData = new OrientationData(
                this.getInputs().getTimeStepDataBuffer(), this);

        // Get local reference to MicrophoneData and start the record buffer (within MicrophoneData Class)
        MicrophoneData microphoneData = getInputs().getMicrophoneData();
        microphoneData.start();

        ImageData imageData = getInputs().getImageData();
        imageData.setPreviewView(findViewById(R.id.camera_x_preview));
        imageData.startCamera(this, this);

        // Add all data inputs to the array list
        inputs.add(batteryData);
        inputs.add(wheelData);
        inputs.add(orientationData);
        inputs.add(microphoneData);
        inputs.add(imageData);

        /* Overwrites the default instances used by the subscriber architecture. If you don't do
         * this AbcvlibLooper will not refer to the correct instance and IOIO sensor data will not
         * be updated
        */
        try {
            getInputs().overwriteDefaults(inputs, abcvlibLooper);
        } catch (ClassNotFoundException e) {
            ErrorHandler.eLog(TAG, "", e, true);
        }

        // Pass your inputs list to a new instance of TimeStepDataAssember along with all other references
        TimeStepDataAssembler timeStepDataAssembler = new TimeStepDataAssembler(inputs, myTrial, null, null, getInputs().getTimeStepDataBuffer());
        try {
            timeStepDataAssembler.startGatherers();
        } catch (RecordingWithoutTimeStepBufferException e) {
            ErrorHandler.eLog(TAG, "", e, true);
        }
    }
}

