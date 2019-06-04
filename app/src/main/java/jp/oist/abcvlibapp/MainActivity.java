package jp.oist.abcvlibapp;

import android.content.Context;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

import jp.oist.abcvlib.basic.AbcvlibActivity;
import jp.oist.abcvlib.basic.AbcvlibSocketClient;

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

    double k_p = 0;
    double k_i = 0;
    double k_d = 0;
    double setPoint = 0;

    private String androidData = "androidData";
    private String controlData = "controlData";
    private HashMap<String, Double> inputs = initializeInputs();
    private HashMap<String, Double> controls = initializeControls();
    private AbcvlibSocketClient socketClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
        super.loggerOn = loggerOn;
        super.wheelPolaritySwap = wheelPolaritySwap;

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        // Python Socket Connection
        socketClient = new AbcvlibSocketClient("192.168.30.179", 65435, inputs, controls);
        new Thread(socketClient).start();

//        // PID Controller
//        PID pidThread = new PID(androidData, controlData);
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
        PythonControl pythonControl = new PythonControl(this, inputs, controls);
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

        double e_t = 0; // e(t) of wikipedia
        double int_e_t; // integral of e(t) from wikipedia. Discrete, so just a sum here.

        double maxTiltAngle = setPoint + 50;
        double minTiltAngle = setPoint - 50;

        private int stuckCount = 0;

        String androidDataDir;
        String controlDataDir;

        public PID(String androidDataDir, String controlDataDir){
            this.controlDataDir = controlDataDir;
            this.androidDataDir = androidDataDir;
        }

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

        int speedLSet; // speed of left wheel set by python code
        int speedRSet; // speed of right wheel set by python code
        double timeStampRemote;
        double[] androidData = new double[8];
        HashMap<String, Double> androidDataDictionary;
        HashMap<String, Double> controlDataDictionary;
        Context context;

        public PythonControl(Context context, HashMap androidData, HashMap controlData){
            this.context = context;
            this.controlDataDictionary = controlData;
            this.androidDataDictionary = androidData;
        }

        public void run(){
            while(true){

                readControlData();
                writeAndroidData();

//                abcvlibMotion.setWheelSpeed(speedLSet, speedRSet);

            }
        }

        private void readControlData(){
            controlDataDictionary = socketClient.getControlsSocket();
            if (controlDataDictionary != null){
                k_p = controlDataDictionary.get("k_p");
                k_i = controlDataDictionary.get("k_i");
                k_d = controlDataDictionary.get("k_d");
                setPoint = controlDataDictionary.get("setPoint");
            }
        }

        private void writeAndroidData(){

            androidDataDictionary.put("theta", abcvlibSensors.getThetaDeg());
            androidDataDictionary.put("thetaDot", abcvlibSensors.getThetaDegDot());
            androidDataDictionary.put("wheelCountL", abcvlibQuadEncoders.getWheelCountL());
            androidDataDictionary.put("wheelCountR", abcvlibQuadEncoders.getWheelCountR());
            androidDataDictionary.put("distanceL", abcvlibQuadEncoders.getDistanceL());
            androidDataDictionary.put("distanceR", abcvlibQuadEncoders.getDistanceR());
            androidDataDictionary.put("wheelSpeedL", abcvlibQuadEncoders.getWheelSpeedL());
            androidDataDictionary.put("wheelSpeedR", abcvlibQuadEncoders.getWheelSpeedR());

            socketClient.setInputsSocket(androidDataDictionary);
        }
    }

    private HashMap<String, Double> initializeInputs(){

        HashMap<String, Double> dictionary = new HashMap<String, Double>();

        dictionary.put("theta", 0.0);
        dictionary.put("thetaDot", 0.0);
        dictionary.put("wheelCountL", 0.0);
        dictionary.put("wheelCountR", 0.0);
        dictionary.put("distanceL", 0.0);
        dictionary.put("distanceR", 0.0);
        dictionary.put("wheelSpeedL", 0.0);
        dictionary.put("wheelSpeedR", 0.0);

        return dictionary;
    }

    private HashMap<String, Double> initializeControls(){

        HashMap<String, Double> dictionary = new HashMap<String, Double>();

        dictionary.put("k_p", 0.0);
        dictionary.put("k_i", 0.0);
        dictionary.put("k_d", 0.0);
        dictionary.put("setPoint", 0.0);
        dictionary.put("wheelSpeedLControl", 0.0);
        dictionary.put("wheelSpeedRControl", 0.0);

        return dictionary;
    }
}
