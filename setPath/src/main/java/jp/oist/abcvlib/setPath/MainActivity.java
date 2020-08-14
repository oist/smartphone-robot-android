package jp.oist.abcvlib.setPath;

import android.os.Bundle;
import jp.oist.abcvlib.core.AbcvlibActivity;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move along a set path repeatedly
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        initialzer(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        // Linear Back and Forth every 10 mm
        setPath setPathThread = new setPath();
        new Thread(setPathThread).start();

    }

    public class setPath implements Runnable{

        int speed = 60; // Duty Cycle from 0 to 100.

        public void run(){

            while(appRunning) {

                try {
                    // Set Initial Speed
                    outputs.motion.setWheelOutput(speed, speed);
                    Thread.sleep(2000);
                    // Turn left
                    outputs.motion.setWheelOutput(speed / 3, speed);
                    Thread.sleep(5000);
                    // Go straight
                    outputs.motion.setWheelOutput(speed, speed);
                    Thread.sleep(2000);
                    // turn the other way
                    outputs.motion.setWheelOutput(speed, speed / 3);
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
