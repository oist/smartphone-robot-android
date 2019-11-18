package jp.oist.abcvlib.basic;

import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

import jp.oist.abcvlib.AbcvlibActivity;


/**
 * Most basic Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Shows basics of setting up any standard Android Application framework, and a simple log output of
 * theta and angular velocity via Logcat using onboard Android sensors.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    /**
     * Various booleans to switch on/off various functionalities. All of these have default values
     * within AbcvlibActivity, so they can be supplied or will default to typical values.
     */
    private static HashMap<String, Boolean> switches;
    static {
        switches = new HashMap<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        initialzer("192.168.28.151", 65434, switches);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(jp.oist.abcvlib.basic.R.layout.activity_main);

        // Create "runnable" object (similar to a thread, but recommended over overriding thread class)
        SimpleTest simpleTest = new SimpleTest();
        // Start the runnable thread
        new Thread(simpleTest).start();

    }

    public class SimpleTest implements Runnable{

        // Every runnable needs a public run method
        public void run(){
            while(true){
                // Prints theta and angular velocity to android logcat
                Log.v(TAG, "theta:" + inputs.motionSensors.getThetaDeg() + "thetaDot:" + inputs.motionSensors.getThetaDegDot());
            }
        }
    }

}

