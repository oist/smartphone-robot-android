package jp.oist.abcvlib.basic;

import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataListener;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataListener;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneDataListener;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.inputs.phone.OrientationDataListener;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework, and a simple log output of
 * theta and angular velocity via Logcat using onboard Android sensors.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements BatteryDataListener,
        OrientationDataListener, WheelDataListener, MicrophoneDataListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initalizes various objects in parent class.
        initializer(this);
        getInputs().getBatteryData().setBatteryDataListener(this);
        getInputs().getOrientationData().setOrientationDataListener(this);
        getInputs().getWheelData().setWheelDataListener(this);
        getInputs().getMicrophoneData().setMicrophoneDataListener(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onBatteryVoltageUpdate(double voltage, long timestamp) {
        Log.i(TAG, "Battery Update: Voltage=" + voltage + " Timestemp=" + timestamp);
    }

    @Override
    public void onChargerVoltageUpdate(double voltage, long timestamp) {
        Log.i(TAG, "Charger Update: Voltage=" + voltage + " Timestemp=" + timestamp);
    }

    @Override
    public void onOrientationUpdate(long timestamp, double thetaRad, double angularVelocityRad) {
        Log.i(TAG, "Orientation Data Update: Timestamp=" + timestamp + " thetaRad=" + thetaRad
                + " angularVelocity=" + angularVelocityRad);

        // You can also convert them to degrees using the following static utility methods.
        double thetaDeg = OrientationData.getThetaDeg(thetaRad);
        double angularVelocityDeg = OrientationData.getAngularVelocityDeg(angularVelocityRad);
    }

    @Override
    public void onWheelDataUpdate(long timestamp, int countLeft, int countRight) {
        Log.i(TAG, "Wheel Data Update: Timestamp=" + timestamp + " countLeft=" + countLeft +
                " countRight=" + countRight);
        double distanceLeft = WheelData.countsToDistance(countLeft);
    }

    @Override
    public void onMicrophoneDataUpdate(float[] audioData, int numSamples) {
        float[] arraySlice = Arrays.copyOfRange(audioData, 0, 9);
        String audioDataString = Arrays.toString(arraySlice);
        Log.i(TAG, "Microphone Data Update: First 10 Samples=" + audioDataString +
                 " of " + numSamples + " total samples");

    }
}

