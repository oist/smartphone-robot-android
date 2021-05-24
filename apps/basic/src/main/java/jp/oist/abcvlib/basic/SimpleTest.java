package jp.oist.abcvlib.basic;

import android.util.Log;
import android.widget.TextView;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;

public class SimpleTest implements Runnable{

    AbcvlibActivity abcvlibActivity;
    Inputs inputs;
    TextView voltageBattDisplay;
    TextView voltageChargerDisplay;

    public SimpleTest(AbcvlibActivity abcvlibActivity){
        this.abcvlibActivity = abcvlibActivity;
        this.inputs = abcvlibActivity.inputs;
        voltageBattDisplay = abcvlibActivity.findViewById(R.id.voltageBatt);
        voltageChargerDisplay = abcvlibActivity.findViewById(R.id.voltageCharger);
    }

    // Every runnable needs a public run method
    public void run(){

        // Prints most recent theta and angular velocity to android logcat
        Log.i("SimpleTest", "theta:" + inputs.orientationData.getThetaDeg() +
                " thetaDot:" +  inputs.orientationData.getThetaDegDot());

        // Another way of accessing this data, but you also get all readings within the current timestep;
        if (abcvlibActivity.getTimeStepDataAssembler() != null){
            TimeStepDataBuffer.TimeStepData.OrientationData orientationData = abcvlibActivity.getTimeStepDataAssembler().getTimeStepDataBuffer().getWriteData().getOrientationData();
            double[] tiltAngles = orientationData.getTiltAngle();
            double mostRecentTiltAngle = tiltAngles[tiltAngles.length - 1];
            double[] angularVelocities = orientationData.getAngularVelocity();
            double mostRecentAngularVelocity = angularVelocities[angularVelocities.length - 1];
        }

        abcvlibActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                if (abcvlibActivity.getTimeStepDataAssembler() != null){
                    TimeStepDataBuffer.TimeStepData.BatteryData batteryData = abcvlibActivity.getTimeStepDataAssembler().getTimeStepDataBuffer().getWriteData().getBatteryData();
                    TimeStepDataBuffer.TimeStepData.ChargerData chargerData = abcvlibActivity.getTimeStepDataAssembler().getTimeStepDataBuffer().getWriteData().getChargerData();

                    voltageBattDisplay.setText("Battery: " + batteryData.getVoltage() + "V");
                    voltageChargerDisplay.setText("Charger: " + chargerData.getVoltage() + "V");
                }
            }
        });
    }
}
