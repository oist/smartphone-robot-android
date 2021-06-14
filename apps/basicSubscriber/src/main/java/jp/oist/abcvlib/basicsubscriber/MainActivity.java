package jp.oist.abcvlib.basicsubscriber;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.PermissionsListener;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataListener;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataListener;
import jp.oist.abcvlib.core.inputs.phone.ImageDataListener;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneDataListener;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.inputs.phone.OrientationDataListener;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework. This MainActivity class
 * implements the various listener interfaces in order to subscribe to updates from various sensor
 * data. Sensor data publishers are running in the background but only write data when a subscriber
 * has been established (via implementing a listener and it's associated method) or a custom
 * {@link jp.oist.abcvlib.core.learning.TimeStepDataAssembler object has been established} setting
 * up such an assembler will be illustrated in a different module.
 *
 * Optional commented out lines in each listener method show how to write the data to the Android
 * logcat log. As these occur VERY frequently (tens of microseconds) this spams the logcat and such
 * I have reserved them only for when necessary. The updates to the GUI via the GuiUpdate object
 * are intentionally delayed or sampled every 100 ms so as not to spam the GUI thread and make it
 * unresponsive.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements PermissionsListener, BatteryDataListener,
        OrientationDataListener, WheelDataListener, MicrophoneDataListener, ImageDataListener {

    private long lastFrameTime = System.nanoTime();
    private GuiUpdater guiUpdater;

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
        /*
         * The various setXXXListener classes set this class as a subscriber to the XXX publisher
         * Battery, Orientation, and WheelData are publishing by default as they are computationally
         * cheap. Image and Microphone data are "lazy start" such that you must call their respective
         * start() methods before they will begin publishing data. ImageData requires that you pass
         * a reference to this class' layout object via the setPreviewView in order to publish the
         * image stream to the GUI. If you don't need a preview on the GUI you don't need to attach
         * this.
         */
        getInputs().getBatteryData().setBatteryDataListener(this);
        getInputs().getOrientationData().setOrientationDataListener(this);
        getInputs().getWheelData().setWheelDataListener(this);

        getInputs().getImageData().setImageDataListener(this);
        getInputs().getImageData().setPreviewView(findViewById(R.id.camera_x_preview));
        getInputs().getImageData().startCamera(this, this);

        getInputs().getMicrophoneData().setMicrophoneDataListener(this);
        getInputs().getMicrophoneData().start();
    }

    @Override
    public void onBatteryVoltageUpdate(double voltage, long timestamp) {
//        Log.i(TAG, "Battery Update: Voltage=" + voltage + " Timestemp=" + timestamp);
        guiUpdater.batteryVoltage = voltage; // make volitile
    }

    @Override
    public void onChargerVoltageUpdate(double chargerVoltage, double coilVoltage, long timestamp) {
//        Log.i(TAG, "Charger Update: Voltage=" + voltage + " Timestemp=" + timestamp);
        guiUpdater.chargerVoltage = chargerVoltage;
        guiUpdater.coilVoltage = coilVoltage;
    }

    @Override
    public void onOrientationUpdate(long timestamp, double thetaRad, double angularVelocityRad) {
//        Log.i(TAG, "Orientation Data Update: Timestamp=" + timestamp + " thetaRad=" + thetaRad
//                + " angularVelocity=" + angularVelocityRad);
//
        // You can also convert them to degrees using the following static utility methods.
        double thetaDeg = OrientationData.getThetaDeg(thetaRad);
        double angularVelocityDeg = OrientationData.getAngularVelocityDeg(angularVelocityRad);
        guiUpdater.thetaDeg = thetaDeg;
        guiUpdater.angularVelocityDeg = angularVelocityDeg;
    }

    @Override
    public void onWheelDataUpdate(long timestamp, int wheelCountL, int wheelCountR,
                                  double wheelDistanceL, double wheelDistanceR,
                                  double wheelSpeedL, double wheelSpeedR) {
//        Log.i(TAG, "Wheel Data Update: Timestamp=" + timestamp + " countLeft=" + countLeft +
//                " countRight=" + countRight);
//        double distanceLeft = WheelData.countsToDistance(countLeft);
        guiUpdater.wheelCountL = wheelCountL;
        guiUpdater.wheelCountR = wheelCountR;
        guiUpdater.wheelDistanceL = wheelDistanceL;
        guiUpdater.wheelDistanceR = wheelDistanceR;
        guiUpdater.wheelSpeedL = wheelSpeedL;
        guiUpdater.wheelSpeedR = wheelSpeedR;
    }

    /**
     * Takes the first 10 samples from the sampled audio data and sends them to the GUI.
     * @param audioData most recent sampling from buffer
     * @param numSamples number of samples copied from buffer
     */
    @Override
    public void onMicrophoneDataUpdate(float[] audioData, int numSamples) {
        float[] arraySlice = Arrays.copyOfRange(audioData, 0, 5);
        DecimalFormat df = new DecimalFormat("0.#E0");
        String arraySliceString = "";
        for (double v : arraySlice) {
            arraySliceString = arraySliceString.concat(df.format(v)) + ", ";
        }
//        Log.i("MainActivity", "Microphone Data Update: First 10 Samples=" + arraySliceString +
//                " of " + numSamples + " total samples");
        guiUpdater.audioDataString = arraySliceString;
    }

    /**
     * Calculates the frame rate and sends it to the GUI. Other input params are ignored in this
     * example, but one could process each bitmap as necessary here.
     * @param timestamp in nanoseconds see {@link java.lang.System#nanoTime()}
     * @param width in pixels
     * @param height in pixels
     * @param bitmap compressed bitmap object
     * @param webpImage byte array representing bitmap
     */
    @Override
    public void onImageDataUpdate(long timestamp, int width, int height, Bitmap bitmap, byte[] webpImage) {
//        Log.i(TAG, "Image Data Update: Timestamp=" + timestamp + " dims=" + width + " x "
//                + height);
        double frameRate = 1.0 / ((System.nanoTime() - lastFrameTime) / 1000000000.0);
        lastFrameTime = System.nanoTime();
        frameRate = Math.round(frameRate);
        guiUpdater.frameRateString = String.format(Locale.JAPAN,"%.0f", frameRate);
    }
}

