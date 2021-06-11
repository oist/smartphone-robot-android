package jp.oist.abcvlib.basicassembler;

import android.Manifest;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.PermissionsListener;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
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
        TimeStepDataAssembler timeStepDataAssembler = new TimeStepDataAssembler(inputs, myStepHandler, null, null, getInputs().getTimeStepDataBuffer());
        try {
            timeStepDataAssembler.startGatherers();
        } catch (RecordingWithoutTimeStepBufferException e) {
            ErrorHandler.eLog(TAG, "", e, true);
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
            myStepHandler.setLastTimestep(true);
            myStepHandler.incrementEpisodeCount();
            // reseting reward after each episode
            reward = 0;
        }

        if (myStepHandler.getEpisodeCount() >= myStepHandler.getMaxEpisodecount()){
            myStepHandler.setLastTimestep(true);
        }

        updateGUIValues(data, timeStepCount);

        return actionSet;
    }

    private void updateGUIValues(TimeStepDataBuffer.TimeStepData data, int timeStepCount){
        guiUpdater.batteryVoltage = data.getBatteryData().getVoltage()[0]; // just taking the first recorded one
        guiUpdater.chargerVoltage = data.getChargerData().getChargerVoltage()[0];
        guiUpdater.coilVoltage = data.getChargerData().getCoilVoltage()[0];
        double thetaDeg = OrientationData.getThetaDeg(thetaRad);
        double angularVelocityDeg = OrientationData.getAngularVelocityDeg(angularVelocityRad);
        guiUpdater.thetaDeg = thetaDeg;
        guiUpdater.angularVelocityDeg = angularVelocityDeg;
        guiUpdater.wheelCountL = data.getWheelCounts().getLeft()[0];
        guiUpdater.wheelCountR = data.getWheelCounts().getRight()[0];
        guiUpdater.wheelDistanceL = data.getWheelData().getWheelDistanceL()[0];
        guiUpdater.wheelDistanceR = data.getWheelData().getWheelDistanceR()[0];
        guiUpdater.wheelSpeedL = data.getWheelData().getWheelSpeedL()[0];
        guiUpdater.wheelSpeedR = data.getWheelData().getWheelSpeedR()[0];
        float[] arraySlice = Arrays.copyOfRange(data.getSoundData().getLevels(), 0, 9);
        guiUpdater.audioDataString = Arrays.toString(arraySlice);
        double frameRate = (data.getImageData().getImages().get(1).getTimestamp() -
                data.getImageData().getImages().get(0).getTimestamp()) / 1000000000.0 ; // just taking difference between two but one could do an average over all differences
        frameRate = Math.round(frameRate);
        guiUpdater.frameRateString = String.format(Locale.JAPAN,"%.0f", frameRate);

    }
}

