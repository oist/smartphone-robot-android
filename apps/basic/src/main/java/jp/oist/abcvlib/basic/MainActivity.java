package jp.oist.abcvlib.basic;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.PermissionsListener;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataListener;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataListener;
import jp.oist.abcvlib.core.inputs.phone.ImageDataListener;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneDataListener;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.inputs.phone.OrientationDataListener;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework, and a simple log output of
 * theta and angular velocity via Logcat using onboard Android sensors.
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
        ScheduledExecutorServiceWithException executor = new ScheduledExecutorServiceWithException(1, new ProcessPriorityThreadFactory(Thread.MIN_PRIORITY, "GuiUpdates"));
        guiUpdater = new GuiUpdater(this);
        executor.scheduleAtFixedRate(guiUpdater, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onBatteryVoltageUpdate(double voltage, long timestamp) {
//        Log.i(TAG, "Battery Update: Voltage=" + voltage + " Timestemp=" + timestamp);
        guiUpdater.batteryVoltage = voltage; // make volitile

    }

    @Override
    public void onChargerVoltageUpdate(double chargerVoltage, double coilVoltage, long timestamp) {
//        Log.i(TAG, "Charger Update: Voltage=" + voltage + " Timestemp=" + timestamp);
    }

    @Override
    public void onOrientationUpdate(long timestamp, double thetaRad, double angularVelocityRad) {
//        Log.i(TAG, "Orientation Data Update: Timestamp=" + timestamp + " thetaRad=" + thetaRad
//                + " angularVelocity=" + angularVelocityRad);
//
        // You can also convert them to degrees using the following static utility methods.
        double thetaDeg = OrientationData.getThetaDeg(thetaRad);
        double angularVelocityDeg = OrientationData.getAngularVelocityDeg(angularVelocityRad);
    }

    @Override
    public void onWheelDataUpdate(long timestamp, WheelData wheelData) {
//        Log.i(TAG, "Wheel Data Update: Timestamp=" + timestamp + " countLeft=" + countLeft +
//                " countRight=" + countRight);
//        double distanceLeft = WheelData.countsToDistance(countLeft);
    }

    @Override
    public void onMicrophoneDataUpdate(float[] audioData, int numSamples) {
        float[] arraySlice = Arrays.copyOfRange(audioData, 0, 9);
        String audioDataString = Arrays.toString(arraySlice);
////        Log.i(TAG, "Microphone Data Update: First 10 Samples=" + audioDataString +
////                 " of " + numSamples + " total samples");
    }

    @Override
    public void onImageDataUpdate(long timestamp, int width, int height, Bitmap bitmap, byte[] webpImage) {
//        Log.i(TAG, "Image Data Update: Timestamp=" + timestamp + " dims=" + width + " x "
//                + height);
        double frameRate = 1.0 / ((System.nanoTime() - lastFrameTime) / 1000000000.0);
        lastFrameTime = System.nanoTime();
        frameRate = Math.round(frameRate);
        String frameRateString = String.format(Locale.JAPAN,"%.0f", frameRate);
    }
}

