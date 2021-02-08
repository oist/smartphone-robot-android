package jp.oist.abcvlib.basic;

import android.util.Log;
import android.widget.TextView;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.Inputs;

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

        // Prints theta and angular velocity to android logcat
        Log.i("SimpleTest", "theta:" + inputs.motionSensors.getThetaDeg() +
                " thetaDot:" +  inputs.motionSensors.getThetaDegDot() +
                " Battery Voltage:" + inputs.battery.getVoltageBatt() +
                " Charger Voltage:" + inputs.battery.getVoltageCharger());

        abcvlibActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                voltageBattDisplay.setText("Battery: " + inputs.battery.getVoltageBatt() + "V");
                voltageChargerDisplay.setText("Charger: " + inputs.battery.getVoltageCharger() + "V");

            }
        });
    }
}
