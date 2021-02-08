package jp.oist.abcvlib.basic;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework, and a simple log output of
 * theta and angular velocity via Logcat using onboard Android sensors.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initalizes various objects in parent class.
        initialzer(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        setContentView(R.layout.activity_main);

        // Executors preferred over runnables or threads for built in memory/cleanup/error handling.
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        SimpleTest simpleTest = new SimpleTest();
        // Run the simpleTest every 1 second
        executor.scheduleAtFixedRate(simpleTest, 0, 1, TimeUnit.SECONDS);

    }

    public class SimpleTest implements Runnable{

        TextView voltageBattDisplay = findViewById(R.id.voltageBatt);
        TextView voltageChargerDisplay = findViewById(R.id.voltageCharger);

        // Every runnable needs a public run method
        public void run(){

            // Prints theta and angular velocity to android logcat
            Log.i(TAG, "theta:" + inputs.motionSensors.getThetaDeg() + " thetaDot:" +
                    inputs.motionSensors.getThetaDegDot() + " Battery Voltage:" + inputs.battery.getVoltageBatt() + " Charger Voltage:" + inputs.battery.getVoltageCharger());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Stuff that updates the UI
                    voltageBattDisplay.setText("Battery: " + inputs.battery.getVoltageBatt() + "V");
                    voltageChargerDisplay.setText("Charger: " + inputs.battery.getVoltageCharger() + "V");

                }
            });
        }
    }

}

