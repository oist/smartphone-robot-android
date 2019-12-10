package jp.oist.abcvlib.backandforth;

import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

import jp.oist.abcvlib.AbcvlibActivity;


/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
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
        /*
         * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
         * memory/disk space on the phone and may result in memory failure if run for a long time
         * such as any learning tasks.
         */
        switches.put("loggerOn", false);
        /*
         * Enable/disable this to swap the polarity of the wheels such that the default forward
         * direction will be swapped (i.e. wheels will move cw vs ccw as forward).
         */
        switches.put("wheelPolaritySwap", false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        initialzer("192.168.29.131", 65434, switches);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(jp.oist.abcvlib.backandforth.R.layout.activity_main);

        // Linear Back and Forth every 10 mm
        BackAndForth backAndForthThread = new BackAndForth();
        new Thread(backAndForthThread).start();

    }

    public class BackAndForth implements Runnable{

        // PID Setup
        double distanceL; // distances traveled by left wheel from start point (mm)
        double distanceR; // distances traveled by right wheel from start point (mm)

        int speed = 60; // Duty cycle from 0 to 100.

        public void run(){

            // Set Initial Speed
            outputs.motion.setWheelOutput(speed, speed);

            while(appRunning) {

                distanceL = inputs.quadEncoders.getDistanceL();
                distanceR = inputs.quadEncoders.getDistanceR();

                if (distanceR >= 200){
                    outputs.motion.setWheelOutput(-speed, -speed);
                }
                else if (distanceR <= -200){
                    outputs.motion.setWheelOutput(speed, speed);
                }

            }
        }
    }

}
