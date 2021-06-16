package jp.oist.abcvlib.basicassembler;

import android.Manifest;
import android.os.Bundle;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.PermissionsListener;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.learning.CommAction;
import jp.oist.abcvlib.core.learning.CommActionSet;
import jp.oist.abcvlib.core.learning.MotionAction;
import jp.oist.abcvlib.core.learning.MotionActionSet;
import jp.oist.abcvlib.core.learning.TimeStepDataAssembler;
import jp.oist.abcvlib.core.outputs.ActionSelector;
import jp.oist.abcvlib.core.outputs.StepHandler;
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
public class MainActivity extends AbcvlibActivity implements PermissionsListener, ActionSelector {

    private long lastFrameTime = System.nanoTime();
    private GuiUpdater guiUpdater;
    private StepHandler myStepHandler;
    private String TAG = getClass().getName();
    private int reward = 0;
    private int maxTimeStepCount;
    private int maxEpisodeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup Android GUI object references such that we can write data to them later.
        setContentView(R.layout.activity_main);

        // Creates an another thread that schedules updates to the GUI every 100 ms. Updaing the GUI every 100 microseconds would bog down the CPU
        ScheduledExecutorServiceWithException executor = new ScheduledExecutorServiceWithException(1, new ProcessPriorityThreadFactory(Thread.MIN_PRIORITY, "GuiUpdates"));
        guiUpdater = new GuiUpdater(this);
        executor.scheduleAtFixedRate(guiUpdater, 0, 100, TimeUnit.MILLISECONDS);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

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

        maxTimeStepCount = 30;
        maxEpisodeCount = 3;

        myStepHandler = new StepHandler.StepHandlerBuilder()
                .setTimeStepLength(100)
                .setMaxTimeStepCount(maxTimeStepCount)
                .setMaxEpisodeCount(maxEpisodeCount)
                .setMaxReward(100000)
                .setMotionActionSet(motionActionSet)
                .setCommActionSet(commActionSet)
                .setActionSelector(this)
                .build();

        // Initialize an ArrayList of AbcvlibInputs that you want the TimeStepDataAssembler to gather data for
        ArrayList<AbcvlibInput> inputs = new ArrayList<>();

        // reusing the same timeStepDataBuffer shared by all. You could alternatively create a new
        // one or extend it in another class.
        BatteryData batteryData = new BatteryData(this.getInputs().getTimeStepDataBuffer());
        WheelData wheelData = new WheelData(this.getInputs().getTimeStepDataBuffer());
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
            getInputs().overwriteDefaults(inputs);
        } catch (ClassNotFoundException e) {
            ErrorHandler.eLog(TAG, "", e, true);
        }

        // Pass your inputs list to a new instance of TimeStepDataAssember along with all other references
        TimeStepDataAssembler timeStepDataAssembler = new TimeStepDataAssembler(inputs, myStepHandler, null, null, getInputs().getTimeStepDataBuffer());
        try {
            timeStepDataAssembler.startGatherers();
        } catch (RecordingWithoutTimeStepBufferException e) {
            ErrorHandler.eLog(TAG, "", e, true);
        }
    }

    @Override
    public ActionSet forward(TimeStepDataBuffer.TimeStepData data) {
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

        if (reward >= myStepHandler.getMaxReward()){
            myStepHandler.setLastTimestep(true);
            // reseting reward after each episode
            reward = 0;
        }

        // Note this will never be called when the myStepHandler.getTimeStep() >= myStepHandler.getMaxTimeStep() as the forward method will no longer be called
        updateGUIValues(data, myStepHandler.getTimeStep(), myStepHandler.getEpisodeCount());

        return actionSet;
    }

    private void updateGUIValues(TimeStepDataBuffer.TimeStepData data, int timeStepCount, int episodeCount){
        guiUpdater.timeStep = timeStepCount + " of " + maxTimeStepCount;
        guiUpdater.episodeCount = episodeCount + " of " + maxEpisodeCount;
        if (data.getBatteryData().getVoltage().length > 0){
            guiUpdater.batteryVoltage = data.getBatteryData().getVoltage()[0]; // just taking the first recorded one
        }
        if (data.getChargerData().getChargerVoltage().length > 0){
            guiUpdater.chargerVoltage = data.getChargerData().getChargerVoltage()[0];
            guiUpdater.coilVoltage = data.getChargerData().getCoilVoltage()[0];
        }
        if (data.getOrientationData().getTiltAngle().length > 20){
            double thetaDeg = OrientationData.getThetaDeg(data.getOrientationData().getTiltAngle()[0]);
            double angularVelocityDeg = OrientationData.getAngularVelocityDeg(data.getOrientationData().getAngularVelocity()[0]);
            guiUpdater.thetaDeg = thetaDeg;
            guiUpdater.angularVelocityDeg = angularVelocityDeg;
        }
        if (data.getWheelData().getLeft().getCounts().length > 0){
            guiUpdater.wheelCountL = data.getWheelData().getLeft().getCounts()[0];
            guiUpdater.wheelCountR = data.getWheelData().getRight().getCounts()[0];
            guiUpdater.wheelDistanceL = data.getWheelData().getLeft().getDistances()[0];
            guiUpdater.wheelDistanceR = data.getWheelData().getRight().getDistances()[0];
            guiUpdater.wheelSpeedL = data.getWheelData().getLeft().getSpeedsInstantaneous()[0];
            guiUpdater.wheelSpeedR = data.getWheelData().getRight().getSpeedsInstantaneous()[0];
        }
        if (data.getSoundData().getLevels().length > 0){
            float[] arraySlice = Arrays.copyOfRange(data.getSoundData().getLevels(), 0, 5);
            DecimalFormat df = new DecimalFormat("0.#E0");
            String arraySliceString = "";
            for (double v : arraySlice) {
                arraySliceString = arraySliceString.concat(df.format(v)) + ", ";
            }
            guiUpdater.audioDataString = arraySliceString;
        }
        if (data.getImageData().getImages().size() > 1){
            double frameRate = 1.0 / ((data.getImageData().getImages().get(1).getTimestamp() -
                    data.getImageData().getImages().get(0).getTimestamp()) / 1000000000.0) ; // just taking difference between two but one could do an average over all differences
            DecimalFormat df = new DecimalFormat("#.0000000000000");
            guiUpdater.frameRateString = df.format(frameRate);
        }
    }
}

