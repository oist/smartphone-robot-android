package jp.oist.abcvlib.backandforth;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        int[][] speedProfile = {{100, 0, -100, 0}, {100, 0, -100, 0}, {2000, 1000, 2000, 1000}};
        BackAndForth backAndForth = new BackAndForth(speedProfile);
        backAndForth.start();
    }

    class BackAndForth {

        /**
         * 3xn matrix with column c, row 1 representing speed of left wheel
         * row 2 representing speed of right wheel, and row 3 the time window for
         * for speed c. Speed specified as -100 to 100 representing PWM pulse width. Time window
         * specified in milliseconds
          */
        int[][] speedProfile;
        ScheduledExecutorServiceWithException executor;
        SpeedSetter speedSetter;

        @RequiresApi(api = Build.VERSION_CODES.N)
        public BackAndForth(int[][] speedProfile){
            this.speedProfile = speedProfile;
            executor = new ScheduledExecutorServiceWithException(1,
                    new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY, "BackAndForth"));
        }

        public void start(){
            executor.scheduleAtFixedRate(() -> {
                int endOfCurrentTimeSlot = 0;
                for (int i = 0 ; i < speedProfile[0].length; i++){
                    speedSetter = new SpeedSetter(speedProfile[0][i], speedProfile[1][i]);
                    executor.schedule(speedSetter, endOfCurrentTimeSlot, TimeUnit.MILLISECONDS);
                    endOfCurrentTimeSlot+=speedProfile[2][i];
                }
            }, 0, Arrays.stream(speedProfile[2]).sum(), TimeUnit.MILLISECONDS);
        }
    }

    class SpeedSetter implements Runnable{

        private final int left;
        private final int right;

        public SpeedSetter(int left, int right){
            this.left = left;
            this.right = right;
        }

        @Override
        public void run() {
            if (getOutputs() == null){
                Log.d("backandforth", "Waiting for outputs to initialize");
            }
            else{
                getOutputs().setWheelOutput(left, right);
                Log.i("BackAndForth", "LeftWheel = " + left + " RightWheel = " + right);
            }
        }
    }
}
