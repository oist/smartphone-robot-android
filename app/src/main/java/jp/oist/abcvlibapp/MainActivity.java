package jp.oist.abcvlibapp;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jp.oist.abcvlib.basic.AbcvlibActivity;

/**
 * Most basic Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    /**
     * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
     * memory/disk space on the phone and may result in memory failure if run for a long time
     * such as any learning tasks.
     */
    private boolean loggerOn = true;
    /**
     * Enable/disable this to swap the polarity of the wheels such that the default forward
     * direction will be swapped (i.e. wheels will move cw vs ccw as forward).
     */
    private boolean wheelPolaritySwap = false;

    private String csvFileString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
        super.loggerOn = loggerOn;
        super.wheelPolaritySwap = wheelPolaritySwap;

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        csvFileString = Environment.getExternalStorageDirectory().toString() + "/params.csv";

//        // PID Controller
//        PID pidThread = new PID();
//        new Thread(pidThread).start();


//        // Linear Back and Forth every 10 mm
//        BackAndForth backAndForthThread = new BackAndForth();
//        new Thread(backAndForthThread).start();

//        // Rotate Back and Forth every 180 deg
//        TurnBackAndForth turnBackAndForthThread = new TurnBackAndForth();
//        new Thread(turnBackAndForthThread).start();

//        // SetPoint Calibration
//        SetPointCalibration setPointCalibration = new SetPointCalibration();
//        new Thread(setPointCalibration).start();

        // PythonControl
        PythonControl pythonControl = new PythonControl();
        new Thread(pythonControl).start();

    }

    public class PID implements Runnable{

        // PID Setup
        double thetaDeg; // tilt of phone with vertical being 0.
        double thetaDegDot; // derivative of tilt (angular velocity)
        double wheelCountL; // encoder count on left wheel
        double wheelCountR; // encoder count on right wheel
        double distanceL; // distances traveled by left wheel from start point (mm)
        double distanceR; // distances traveled by right wheel from start point (mm)
        double speedL; // Current speed on left wheel in mm/s
        double speedR; // Current speed on right wheel in mm/s
        double[] params = new double[3];


        int output; //  u(t) of wikipedia
        float zeroOffset = -13.25f;
        float calibrationHangAngle = -12f; // Set this after hanging robot upside down.
        float setPoint = (2.0f * zeroOffset) - calibrationHangAngle; // SP of wikipedia
        double e_t = 0; // e(t) of wikipedia
        double int_e_t; // integral of e(t) from wikipedia. Discrete, so just a sum here.

        double k_p = 300;
//        float k_i = 0.0003f;
        double k_i = 0;
        double k_d = 1f;
//        float k_d = 0;

        float maxTiltAngle = setPoint + 50;
        float minTiltAngle = setPoint - 50;

        private int stuckCount = 0;

        public void run(){

            while(true) {

                /*
                Option 1: This simply directly sets a static value of 500 as the pulse width on
                each wheel
                 */
//                MainActivity.super.abcvlibMotion.setWheelSpeed(-500,500);

                /*
                Option 2: This attempts to set the tilt angle to zero via a simple PID controller
                */

                thetaDeg = abcvlibSensors.getThetaDeg();
                thetaDegDot = abcvlibSensors.getThetaDegDot();
                wheelCountL = abcvlibQuadEncoders.getWheelCountL();
                wheelCountR = abcvlibQuadEncoders.getWheelCountR();
                distanceL = abcvlibQuadEncoders.getDistanceL();
                distanceR = abcvlibQuadEncoders.getDistanceR();
                speedL = abcvlibQuadEncoders.getWheelSpeedL();
                speedR = abcvlibQuadEncoders.getWheelSpeedR();
                params = abcvlibSaveData.readData(csvFileString);

                k_p = params[0];
                k_i = params[1];
                k_d = params[2];

                // if tilt angle is within minTiltAngle and maxTiltAngle, use PD controller, else use bouncing non-linear controller
                if(thetaDeg < maxTiltAngle && thetaDeg > minTiltAngle){

                    linearController();
                    stuckCount = 0;
                }
                else if(stuckCount < 500000){
                    linearController();
                    stuckCount = stuckCount + 1;
                }
                else{
                    if (output == 1000){
                        output = -1000;
                    }
                    else{
                        output = 1000;
                    }
                }

                abcvlibMotion.setWheelSpeed(output, output);

            }
        }

        private void linearController(){
            int_e_t = int_e_t + e_t;
            e_t = setPoint - thetaDeg;

            double p_out = k_p * e_t;
            double i_out = k_i * int_e_t;
            double d_out = k_d * thetaDegDot;

            output = (int) Math.round(p_out + i_out + d_out);
        }
    }

    public class BackAndForth implements Runnable{

        // PID Setup
        double distanceL; // distances traveled by left wheel from start point (mm)
        double distanceR; // distances traveled by right wheel from start point (mm)

        int speed = 300; // PWM from 0 to 1000.

        public void run(){

            // Set Initial Speed
            abcvlibMotion.setWheelSpeed(speed, speed);

            while(true) {

                distanceL = abcvlibQuadEncoders.getDistanceL();
                distanceR = abcvlibQuadEncoders.getDistanceR();

                if (distanceR >= 10){
                    abcvlibMotion.setWheelSpeed(speed, speed);
                }
                else if (distanceR <= 10){
                    abcvlibMotion.setWheelSpeed(-speed, -speed);
                }

            }
        }
    }

    public class TurnBackAndForth implements Runnable{

        // PID Setup
        double distanceL; // distances traveled by left wheel from start point (mm)
        double distanceR; // distances traveled by right wheel from start point (mm)

        int speed = 600; // PWM from 0 to 1000.

        public void run(){

            // Set Initial Speed
            abcvlibMotion.setWheelSpeed(speed, -speed);

            while(true) {

                distanceL = abcvlibQuadEncoders.getDistanceL();
                distanceR = abcvlibQuadEncoders.getDistanceR();

                if (distanceR >= distanceL){
                    abcvlibMotion.setWheelSpeed(speed, -speed);
                }
                else if (distanceR <= distanceL){
                    abcvlibMotion.setWheelSpeed(-speed, speed);
                }

            }
        }
    }

    public class SetPointCalibration implements Runnable{
        public void run(){

        }
    }

    public class PythonControl implements Runnable{
        public void run(){
            while(true){
                double[] params = new double[3];
                params = abcvlibSaveData.readData(csvFileString);
                int speed = (int) params[0];
                abcvlibMotion.setWheelSpeed(speed, speed);
            }
        }
    }
}
