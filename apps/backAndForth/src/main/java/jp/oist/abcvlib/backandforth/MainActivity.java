package jp.oist.abcvlib.backandforth;

import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
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
        // ID within the R class
        setContentView(R.layout.activity_main);

        Log.i(TAG, "Step 0");

        int[][] speedProfile = {{50, 0, -50, 0}, {50, 0, -50, 0}, {2000, 1000, 2000, 1000}};
        BackAndForth backAndForth = new BackAndForth(speedProfile);

    }

    class BackAndForth {

        /**
         * 3xn matrix with column c, row 1 representing speed of left wheel
         * row 2 representing speed of right wheel, and row 3 the time window for
         * for speed c. Speed specified as -100 to 100 representing PWM pulse width. Time window
         * specified in milliseconds
          */
        int[][] speedProfile;
        ScheduledExecutorService executor;
        SpeedSetter speedSetter;

        public BackAndForth(int[][] speedProfile){
            this.speedProfile = speedProfile;
            executor = Executors.newScheduledThreadPool(1);
            int endOfCurrentTimeSlot = 0;
            for (int i = 0 ; i < speedProfile[0].length; i++){
                speedSetter = new SpeedSetter(speedProfile[0][i], speedProfile[1][i]);
                executor.schedule(speedSetter, endOfCurrentTimeSlot, TimeUnit.MILLISECONDS);
                endOfCurrentTimeSlot+=speedProfile[2][i];
            }
        }


    }

    class SpeedSetter implements Runnable{

        private int left;
        private int right;

        public SpeedSetter(int left, int right){
            this.left = left;
            this.right = right;
        }

        @Override
        public void run() {
            outputs.motion.setWheelOutput(left, right);
            Log.i(TAG, "LeftWheel = " + left + " RightWheel = " + right);
        }
    }

}
