package jp.oist.abcvlib.backandforth;

import android.os.Bundle;
import android.util.Log;
import jp.oist.abcvlib.core.AbcvlibActivity;
import java.lang.System;
/**
 * Android application showing connection to Robot3.1 PCB, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
 * @author Christopher Buckley <a href="https://github.com/topherbuckley">...</a>
 */
public class MainActivity extends AbcvlibActivity {
    float speed = 0.35f;
    float increment = 0.01f;
    int cnt = 0;
    long startTime;
    // start timer to measure how long to get to cnt = 100

    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void abcvlibMainLoop(){
        if (cnt == 0) {
            // start timer
            startTime = System.nanoTime();
        }
        Log.i("BackAndForth", "Current command speed: " + speed);
        serialCommManager.setMotorLevels(speed, speed, false, false);
        if (speed >= 1.00f || speed <= -1.00f) {
            increment = -increment;
        }
        speed += increment;
        cnt++;
        if (cnt == 100) {
            cnt = 0;
            // stop timer
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);
            // divide by 100 to get average time per command and convert from nanoseconds to milliseconds
            Log.i("BackAndForth", "Average time per command: " + duration/100000000 + "ms");
        }
    }
}