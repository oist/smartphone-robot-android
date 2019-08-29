package jp.oist.abcvlib.basic;

import android.os.Bundle;


/**
 * Most basic Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Shows basics of setting up any standard Android Application framework, and a simple log output of
 * theta and angular velocity via Logcat using onboard Android sensors.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

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
                System.out.println("theta:" + abcvlibSensors.getThetaDeg() + "thetaDot:" + abcvlibSensors.getThetaDegDot());
            }
        }
    }

}

