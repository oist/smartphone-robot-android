package jp.oist.abcvlib.basicsubscriber;

import android.graphics.Bitmap;
import android.media.AudioTimestamp;
import android.os.Bundle;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataSubscriber;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataSubscriber;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.ImageDataSubscriber;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneDataSubscriber;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.inputs.phone.OrientationDataSubscriber;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework. This MainActivity class
 * implements the various listener interfaces in order to subscribe to updates from various sensor
 * data. Sensor data publishers are running in the background but only write data when a subscriber
 * has been established (via implementing a listener and it's associated method) or a custom
 * {@link jp.oist.abcvlib.core.learning.Trial object has been established} setting
 * up such an assembler will be illustrated in a different module.
 *
 * Optional commented out lines in each listener method show how to write the data to the Android
 * logcat log. As these occur VERY frequently (tens of microseconds) this spams the logcat and such
 * I have reserved them only for when necessary. The updates to the GUI via the GuiUpdate object
 * are intentionally delayed or sampled every 100 ms so as not to spam the GUI thread and make it
 * unresponsive.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements IOReadyListener,
        BatteryDataSubscriber, OrientationDataSubscriber, WheelDataSubscriber, MicrophoneDataSubscriber, ImageDataSubscriber {

    private long lastFrameTime = System.nanoTime();
    private GuiUpdater guiUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup Android GUI object references such that we can write data to them later.
        setContentView(R.layout.activity_main);

        setIoReadyListener(this);

        // Creates an another thread that schedules updates to the GUI every 100 ms. Updaing the GUI every 100 microseconds would bog down the CPU
        ScheduledExecutorServiceWithException executor = new ScheduledExecutorServiceWithException(1, new ProcessPriorityThreadFactory(Thread.MIN_PRIORITY, "GuiUpdates"));
        guiUpdater = new GuiUpdater(this);
        executor.scheduleAtFixedRate(guiUpdater, 0, 100, TimeUnit.MILLISECONDS);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onIOReady(AbcvlibLooper abcvlibLooper) {
        /*
         * Each {XXX}Data class has a builder that you can set various construction input parameters
         * with. Neglecting to set them will assume default values. See each class for its corresponding
         * default values and available builder set methods. Context is passed for permission requests,
         * and {XXX}Listeners are what are used to set the subscriber to the {XXX}Data class.
         * The subscriber in this example is this (MainActivity) class. It can equally be any other class
         * that implements the appropriate listener interface.
         */
        PublisherManager publisherManager = new PublisherManager();
        new WheelData.Builder(this, publisherManager, abcvlibLooper).build().addSubscriber(this);
        new BatteryData.Builder(this, publisherManager, abcvlibLooper).build().addSubscriber(this);
        new OrientationData.Builder(this, publisherManager).build().addSubscriber(this);
        new ImageData.Builder(this, publisherManager, this)
                .setPreviewView(findViewById(R.id.camera_x_preview)).build().addSubscriber(this);
        new MicrophoneData.Builder(this, publisherManager).build().addSubscriber(this);
        publisherManager.initializePublishers();
        publisherManager.startPublishers();
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
                                  double wheelSpeedInstantL, double wheelSpeedInstantR,
                                  double wheelSpeedBufferedL, double wheelSpeedBufferedR,
                                  double wheelSpeedExpAvgL, double wheelSpeedExpAvgR) {
//        Log.i(TAG, "Wheel Data Update: Timestamp=" + timestamp + " countLeft=" + countLeft +
//                " countRight=" + countRight);
//        double distanceLeft = WheelData.countsToDistance(countLeft);
        guiUpdater.wheelCountL = wheelCountL;
        guiUpdater.wheelCountR = wheelCountR;
        guiUpdater.wheelDistanceL = wheelDistanceL;
        guiUpdater.wheelDistanceR = wheelDistanceR;
        guiUpdater.wheelSpeedInstantL = wheelSpeedInstantL;
        guiUpdater.wheelSpeedInstantR = wheelSpeedInstantR;
        guiUpdater.wheelSpeedBufferedL = wheelSpeedBufferedL;
        guiUpdater.wheelSpeedBufferedR = wheelSpeedBufferedR;
        guiUpdater.wheelSpeedExpAvgL = wheelSpeedExpAvgL;
        guiUpdater.wheelSpeedExpAvgR = wheelSpeedExpAvgR;
    }

    /**
     * Takes the first 10 samples from the sampled audio data and sends them to the GUI.
     * @param audioData most recent sampling from buffer
     * @param numSamples number of samples copied from buffer
     */
    @Override
    public void onMicrophoneDataUpdate(float[] audioData, int numSamples, int sampleRate, AudioTimestamp startTime, AudioTimestamp endTime) {
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

