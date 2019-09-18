package jp.oist.abcvlib.setPath;

import android.os.Bundle;

import jp.oist.abcvlib.basic.AbcvlibActivity;


/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    /**
     * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
     * memory/disk space on the phone and may result in memory failure if run for a long time
     * such as any learning tasks.
     */
    boolean loggerOn = false;
    /**
     * Enable/disable this to swap the polarity of the wheels such that the default forward
     * direction will be swapped (i.e. wheels will move cw vs ccw as forward).
     */
    boolean wheelPolaritySwap = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
        super.loggerOn = loggerOn;
        super.wheelPolaritySwap = wheelPolaritySwap;

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(jp.oist.abcvlib.setPath.R.layout.activity_main);

        // Linear Back and Forth every 10 mm
        setPath setPathThread = new setPath();
        new Thread(setPathThread).start();
        System.nanoTime();

    }

    public class setPath implements Runnable{

        int speed = 60; // Duty Cycle from 0 to 100.

        public void run(){

            while(true) {

                try {
                    // Set Initial Speed
                    abcvlibMotion.setWheelSpeed(speed, speed);
                    Thread.sleep(2000);
                    // Turn left
                    abcvlibMotion.setWheelSpeed(speed / 3, speed);
                    Thread.sleep(5000);
                    // Go straight
                    abcvlibMotion.setWheelSpeed(speed, speed);
                    Thread.sleep(2000);
                    // turn the other way
                    abcvlibMotion.setWheelSpeed(speed, speed / 3);
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
