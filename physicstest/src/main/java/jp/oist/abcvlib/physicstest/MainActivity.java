package jp.oist.abcvlib.physicstest;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.FormatFlagsConversionMismatchException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jp.oist.abcvlib.AbcvlibActivity;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
 * the interval and speed are set to a looping step function from zero->max->zero->-max. This data
 * is saved to a file for post processing where it can be used to determine the static friction and
 * damping of the joins for use in Pybullet simulations.
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

        // Linear Back and Forth every 10 mm
        Motion motionThread = new Motion();
        new Thread(motionThread).start();

    }

    public class Motion implements Runnable{

        int[] speeds = {100, 0, -100, 0}; // Duty cycle from 0 to 100.
        int timestepTtl = 2000;
        int cycles = 2;
//        AtomicInteger stepCount = new AtomicInteger();
        int stepCount = 0;
        int indexCnt = 0;
        int arrayLength = speeds.length * timestepTtl * cycles;

        int[] indexTracker = new int[arrayLength];
        long[] timestampCustom = new long[arrayLength];
        long[] timestampDiff = new long[arrayLength];
        double[] wLSet = new double[arrayLength]; // Angular velocity on Left wheel Setpoint
        double[] wLMeasured = new double[arrayLength]; // Angular velocity on Left wheel Measured
        double[] wRSet = new double[arrayLength]; // Angular velocity on Right wheel Setpoint
        double[] wRMeasured = new double[arrayLength]; // Angular velocity on Right wheel Measured
        double[] wPMeasured = new double[arrayLength]; // Angular velocity of Phone Measured
        double[] tPMeasured = new double[arrayLength]; // theta tilt angle of Phone Measured

        public void run(){
            String header = "stepCount" + ", " + "timestamp" + ", " + "timestampDiff" + ", " + "wLSet" + ", " + "wLMeasured" + ", " + "wRSet" +
                    ", " + "wRMeasured" + ", " + "wPMeasured" + ", " + "tPMeasured";
            writeToFile(header, false);
//            for (int m = 0; m < 200; m++){
//                if (m > 0) {
//                    timestampCustom[m] = System.nanoTime();
//                    timestampDiff[m] = timestampCustom[m] - timestampCustom[m - 1];
//                }
//                else {
//                    timestampCustom[m] = System.nanoTime();
//                    timestampDiff[m] = timestampCustom[m];
//                }
//            }
            int i = 0;

            while (i < cycles){

                int k = 0;

                while (k < speeds.length){
                    // Set wheel speed
//                    outputs.motion.setWheelOutput(speeds[k], speeds[k]);

                    int t = 0;

                    while (t < timestepTtl - 1){
//                         Wait t timesteps and record all variables for each step
                        indexCnt = ((i + 1) * (k + 1) * (t + 1));
                        indexTracker[indexCnt] = indexCnt;

                        timestampCustom[indexCnt] = System.nanoTime();
                        timestampDiff[indexCnt] = (timestampCustom[indexCnt] - timestampCustom[indexCnt - 1]) / 1000000;

//                        wLSet[indexCnt] = speeds[k] * (12.5/100); // Convert pulse with % to rad/s assuming max angular vel is 12.5 rad/s
//                        wRSet[indexCnt] = wLSet[indexCnt];
//                        wLMeasured[indexCnt] = inputs.quadEncoders.getWheelSpeedL() * (2*Math.PI / 128); // in rad/s assuming 128 counts per single rotation of wheel.
//                        wRMeasured[indexCnt] = inputs.quadEncoders.getWheelSpeedL() * (2*Math.PI / 128);
//                        wPMeasured[indexCnt] = inputs.motionSensors.getThetaRadDot();
//                        tPMeasured[indexCnt] = inputs.motionSensors.getThetaRad();

                        stepCount++;
                        t++;
                    }
                    k++;
                }
                i++;
            }

//            AtomicInteger i = new AtomicInteger();
//
//            while (i.get() < cycles){
//
//                AtomicInteger k = new AtomicInteger();
//
//                while (k.get() < speeds.length){
//                    // Set wheel speed
//                    outputs.motion.setWheelOutput(speeds[k.get()], speeds[k.get()]);
//
//                    AtomicInteger t = new AtomicInteger();
//
//                    while (t.get() < timestepTtl){
////                         Wait t timesteps and record all variables for each step
//                        indexCnt = ((i.get() + 1) * (k.get() + 1) * (t.get() + 1)) - 1;
//                        indexTracker[indexCnt] = indexCnt;
//
//                        if (indexCnt > 0) {
//                            timestampCustom[indexCnt] = System.nanoTime();
//                            timestampDiff[indexCnt] = timestampCustom[indexCnt] - timestampCustom[indexCnt - 1];
//                        }
//                        else {
//                            timestampCustom[indexCnt] = System.nanoTime();
//                            timestampDiff[indexCnt] = timestampCustom[indexCnt];
//                        }
//
//                        wLSet[indexCnt] = speeds[k.get()] * (12.5/100); // Convert pulse with % to rad/s assuming max angular vel is 12.5 rad/s
//                        wRSet[indexCnt] = wLSet[indexCnt];
//                        wLMeasured[indexCnt] = inputs.quadEncoders.getWheelSpeedL() * (2*Math.PI / 128); // in rad/s assuming 128 counts per single rotation of wheel.
//                        wRMeasured[indexCnt] = inputs.quadEncoders.getWheelSpeedL() * (2*Math.PI / 128);
//                        wPMeasured[indexCnt] = inputs.motionSensors.getThetaRadDot();
//                        tPMeasured[indexCnt] = inputs.motionSensors.getThetaRad();
//
//                        stepCount.incrementAndGet();
//                        t.incrementAndGet();
//                    }
//                    k.incrementAndGet();
//                }
//                i.incrementAndGet();
//            }

            Log.d(TAG + "test", Arrays.toString(indexTracker));
            Log.d(TAG + "test", Arrays.toString(timestampCustom));
            Log.d(TAG + "test", Arrays.toString(timestampDiff));

            double diffAvg = 0;
            double diffMax = 0;
            double diffMin = 0;
            int minPos = 0;
            int maxPos = 0;
            for (int m = 2; m < timestampDiff.length; m++) {
                if (timestampDiff[m] < diffMin){
                    diffMin = timestampDiff[m];
                    minPos = m;
                }
                if (timestampDiff[m] > diffMax){
                    diffMax = timestampDiff[m];
                    maxPos = m;
                }
                diffAvg = diffAvg + timestampDiff[m];
            }
            diffAvg = (diffAvg / timestampDiff.length); //in ms
            diffMax = diffMax;
            diffMin = diffMin;
            Log.d(TAG + "test", "Avg TimeDiff = " + String.valueOf(diffAvg) + " ms" + ", Min TimeDiff = " + String.valueOf(diffMin) + " ms @Index:" + minPos + ", Max TimeDiff = " + String.valueOf(diffMax) + " ms @Index:" + maxPos);
            Log.d(TAG + "test", "Total Indicies=" + timestampDiff.length);

            int l = 0;
            while (l < timestampDiff.length){
                String data = String.valueOf(l) + ", " + String.valueOf(timestampCustom[l]) + ", " + String.valueOf(timestampDiff[l]) +
                        ", " + String.valueOf(wLSet[l]) + ", " + String.valueOf(wLMeasured[l]) +
                        ", " + String.valueOf(wRSet[l]) + ", " + String.valueOf(wRMeasured[l]) +
                        ", " + String.valueOf(wPMeasured[l]) + ", " + String.valueOf(tPMeasured[l]);
                writeToFile(data, true);
                l++;
            }

//            AtomicInteger p = new AtomicInteger();
//
//            while (p.get() < timestamp.length){
//                int l = p.get();
//                String data = String.valueOf(l) + ", " + String.valueOf(timestamp[l]) +
//                        ", " + String.valueOf(wLSet[l]) + ", " + String.valueOf(wLMeasured[l]) +
//                        ", " + String.valueOf(wRSet[l]) + ", " + String.valueOf(wRMeasured[l]) +
//                        ", " + String.valueOf(wPMeasured[l]) + ", " + String.valueOf(tPMeasured[l]);
//                writeToFile(data, true);
//                p.incrementAndGet();
//            }
        }

        private void writeToFile(String data, Boolean append) {
            try {
                String filename = getFilesDir() + "/physicsData.txt";
                PrintWriter writer = new PrintWriter( new BufferedWriter(new FileWriter(filename, append)));
                writer.println(data);
                writer.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
    }

}
