package jp.oist.abcvlib.physicstest;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
 * the interval and speed are set to a looping step function from zero->max->zero->-max. This data
 * is saved to a file for post processing where it can be used to determine the static friction and
 * damping of the joins for use in Pybullet simulations.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initalizes various objects in parent class.
        initialzer(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Toast.makeText(MainActivity.this, message.obj.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        // Linear Back and Forth every 10 mm
        Motion motionThread = new Motion();
        new Thread(motionThread).start();

    }

    public class Motion implements Runnable{

        int[] speeds = {100, 0, -100, 0}; // Duty cycle from 0 to 100.
        int timestepTtl = 1000;
        int cycles = 1;
        int stepCount = 0;
        int avgLength = 2850;
        int arrayLength = speeds.length * timestepTtl * cycles;

        ExecutorService executorSingle = Executors.newSingleThreadExecutor();
        ExecutorService executorMulti = Executors.newCachedThreadPool();
        String filename = getFilesDir() + "/physicsData.txt";
        PrintWriter writer;

        public void run(){

            String header = "stepCount" + ", " + "timestampAbsolute" + ", " + "timestampRelative" + ", " + "timestampDiff" + ", " + "wLSet" + ", " + "wLMeasured" + ", " + "wRSet" +
                    ", " + "wRMeasured" + ", " + "wPMeasured" + ", " + "tPMeasured" + ", " + "encdCountsL" + ", " + "encdCountsR";
            try {
                writer = new PrintWriter( new BufferedWriter(new FileWriter(filename, false)));
                writer.println(header);
                writer.flush();
                writer.close();
                writer = new PrintWriter( new BufferedWriter(new FileWriter(filename, true)));
            } catch (IOException e) {
                e.printStackTrace();
            }

            long startTime = System.nanoTime();
            int i = 0;

            while (i < cycles){

                int k = 0;

                while (k < speeds.length){
                    // Set wheel speed
                    outputs.motion.setWheelOutput(speeds[k], speeds[k]);

                    final int[] stepCountTracker = new int[timestepTtl];
                    final long[] timestamp = new long[timestepTtl];
                    final long[] timestampRelative = new long[timestepTtl];
                    final long[] timestampDiff = new long[timestepTtl];
                    final double[] wLSetAvg = new double[timestepTtl]; // Angular velocity on Left wheel Setpoint
                    final double[] wLMeasuredAvg = new double[timestepTtl]; // Angular velocity on Left wheel Measured
                    final double[] wLMeasuredAvgLP = new double[timestepTtl];
                    final double[] wRSetAvg = new double[timestepTtl]; // Angular velocity on Right wheel Setpoint
                    final double[] wRMeasuredAvg = new double[timestepTtl]; // Angular velocity on Right wheel Measured
                    final double[] wRMeasuredAvgLP = new double[timestepTtl];
                    final double[] wPMeasuredAvg = new double[timestepTtl]; // Angular velocity of Phone Measured
                    final double[] tPMeasuredAvg = new double[timestepTtl]; // theta tilt angle of Phone Measured
                    final double[] encdCountsL = new double[timestepTtl]; // Angular velocity of Phone Measured
                    final double[] encdCountsR = new double[timestepTtl]; // theta tilt angle of Phone Measured

                    int t = 0;

                    while (t < timestepTtl){
//                         Wait t timesteps and record all variables for each step

                        stepCountTracker[t] = stepCount;
                        timestamp[t] = System.nanoTime();
                        timestampRelative[t] = (timestamp[t] - startTime);
                        if (t - 1 < 0){
                            timestampDiff[t] = ((timestamp[t] - timestamp[timestamp.length - 1]));
                        }else {
                            timestampDiff[t] = ((timestamp[t] - timestamp[t - 1]));
                        }

                        wLSetAvg[t] = speeds[k] * (12.5/100); // Convert pulse with % to rad/s assuming max angular vel is 12.5 rad/s
                        wRSetAvg[t] = wLSetAvg[t];

                        int n = 0;
                        while (n < avgLength){

                            wLMeasuredAvg[t] += inputs.quadEncoders.getWheelSpeedL() * (2*Math.PI / 128); // in rad/s assuming 128 counts per single rotation of wheel.
                            wRMeasuredAvg[t] += inputs.quadEncoders.getWheelSpeedR() * (2*Math.PI / 128);
                            wLMeasuredAvgLP[t] += inputs.quadEncoders.getWheelSpeedL_LP() * (2*Math.PI / 128); // in rad/s assuming 128 counts per single rotation of wheel.
                            wRMeasuredAvgLP[t] += inputs.quadEncoders.getWheelSpeedR_LP() * (2*Math.PI / 128);
                            wPMeasuredAvg[t] += inputs.motionSensors.getThetaRadDot();
                            tPMeasuredAvg[t] += inputs.motionSensors.getThetaRad();
                            encdCountsL[t] += inputs.quadEncoders.getWheelCountL();
                            encdCountsR[t] += inputs.quadEncoders.getWheelCountR();

                            n++;
                        }

                        wLMeasuredAvg[t] /= -n; // input speed and measured were opposite...likely need to fix some sign. Distance may be negative
                        wRMeasuredAvg[t] /= -n;
                        wLMeasuredAvgLP[t] /= -n;
                        wRMeasuredAvgLP[t] /= -n;
                        wPMeasuredAvg[t] /= n;
                        tPMeasuredAvg[t] /= n;
                        encdCountsL[t] /= n;
                        encdCountsR[t] /= n;

                        stepCount++;
                        t++;
                    }

                    // for each event
                    executorSingle.submit(new Runnable() {
                        public void run(){
                            Log.d(TAG, "Generating Line:");
                            writeLines(stepCountTracker, timestamp, timestampRelative, timestampDiff,
                                    wLSetAvg, wLMeasuredAvg, wLMeasuredAvgLP, wRSetAvg, wRMeasuredAvg,
                                    wRMeasuredAvgLP, wPMeasuredAvg, tPMeasuredAvg, encdCountsL, encdCountsR);
                        }
                    });

                    k++;
                }
                i++;
            }

            executorSingle.shutdown();
            try {
                executorSingle.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interuption Exception Line 142");
            }

            writer.flush();
            writer.close();

            Message message = mHandler.obtainMessage(1, "Finished");
            message.sendToTarget();

            Log.d(TAG, "Finished Writing File");

        }

        private void writeLines(int[] stepCountTracker, long[] timestampCustom, long[] timestampRelative,
                                long[] timestampDiff, double[] wLSet, double[] wLMeasured, double[] wLMeasuredLP,
                                double[] wRSet, double[] wRMeasured, double[] wRMeasuredLP,
                                double[] wPMeasured, double[] tPMeasured, double[] encdCountsL, double[] encdCountsR){
            int l = 0;
            while (l < timestampDiff.length){
                double timestampCustom_ms = timestampCustom[l] / 1000000.0;
                double timestampRelative_ms = timestampRelative[l] / 1000000.0;
                double timestampDiff_ms = timestampDiff[l] / 1000000.0;
                String data = String.valueOf(stepCountTracker[l]) + ", " + String.valueOf(timestampCustom_ms) + ", " +
                        String.valueOf(timestampRelative_ms) + "," + String.valueOf(timestampDiff_ms) +
                        ", " + String.valueOf(wLSet[l]) + ", " + String.valueOf(wLMeasured[l]) + ", " + String.valueOf(wLMeasuredLP[l]) +
                        ", " + String.valueOf(wRSet[l]) + ", " + String.valueOf(wRMeasured[l]) + ", " + String.valueOf(wRMeasuredLP[l]) +
                        ", " + String.valueOf(wPMeasured[l]) + ", " + String.valueOf(tPMeasured[l]) +
                        ", " + String.valueOf(encdCountsL[l]) + ", " + String.valueOf(encdCountsR[l]);
                writer.println(data);
                l++;
            }
        }
    }

}
