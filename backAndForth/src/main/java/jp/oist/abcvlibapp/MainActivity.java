package jp.oist.abcvlibapp;

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
        setContentView(R.layout.activity_main);

        // Linear Back and Forth every 10 mm
        BackAndForth backAndForthThread = new BackAndForth();
        new Thread(backAndForthThread).start();

    }

    public class BackAndForth implements Runnable{

        // PID Setup
        double distanceL; // distances traveled by left wheel from start point (mm)
        double distanceR; // distances traveled by right wheel from start point (mm)

        int speed = 500; // PWM from 0 to 1000.

        public void run(){

            // Set Initial Speed
            abcvlibMotion.setWheelSpeed(speed, speed);

            while(true) {

                distanceL = abcvlibQuadEncoders.getDistanceL();
                distanceR = abcvlibQuadEncoders.getDistanceR();

                if (distanceR >= 200){
                    abcvlibMotion.setWheelSpeed(-speed, -speed);
                }
                else if (distanceR <= -200){
                    abcvlibMotion.setWheelSpeed(speed, speed);
                }

            }
        }
    }

}
