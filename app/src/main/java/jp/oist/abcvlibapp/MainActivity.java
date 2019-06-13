package jp.oist.abcvlibapp;

import android.content.Context;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

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
    private boolean loggerOn = false;
    /**
     * Enable/disable this to swap the polarity of the wheels such that the default forward
     * direction will be swapped (i.e. wheels will move cw vs ccw as forward).
     */
    private boolean wheelPolaritySwap = false;

    double k_p = 0;
    double k_i = 0;
    double k_d = 0;
    double setPoint = 0;
    private String[] controlParams = {"k_p", "k_i", "k_d", "setPoint", "wheelSpeedL", "wheelSpeedR"};

    private String androidData = "androidData";
    private String controlData = "controlData";
    private JSONObject inputs = initializeInputs();
    private JSONObject controls = initializeControls();
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

//        // Simple Test
//        SimpleTest simpleTest = new SimpleTest();
//        new Thread(simpleTest).start();

        // Python Socket Connection
        socketClient = new AbcvlibSocketClient("192.168.30.179", 65434, inputs, controls);
        new Thread(socketClient).start();

        //PID Controller
        PID pidThread = new PID();
        new Thread(pidThread).start();

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
        PythonControl pythonControl = new PythonControl(this);
        new Thread(pythonControl).start();

    }

    public class SimpleTest implements Runnable{

        public void run(){
            while(true){
                System.out.println("theta:" + abcvlibSensors.getThetaDeg() + "thetaDot:" + abcvlibSensors.getThetaDegDot());
            }
        }
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

        int output; //  u(t) of wikipedia

        double e_t = 0; // e(t) of wikipedia
        double int_e_t; // integral of e(t) from wikipedia. Discrete, so just a sum here.

        double maxAbsTilt = 10;
        double maxTiltAngle = setPoint + maxAbsTilt;
        double minTiltAngle = setPoint - maxAbsTilt;

        private int stuckCount = 0;

        public void run(){

            while(true) {

                thetaDeg = abcvlibSensors.getThetaDeg();
                thetaDegDot = abcvlibSensors.getThetaDegDot();
                wheelCountL = abcvlibQuadEncoders.getWheelCountL();
                wheelCountR = abcvlibQuadEncoders.getWheelCountR();
                distanceL = abcvlibQuadEncoders.getDistanceL();
                distanceR = abcvlibQuadEncoders.getDistanceR();
                speedL = abcvlibQuadEncoders.getWheelSpeedL();
                speedR = abcvlibQuadEncoders.getWheelSpeedR();
                maxTiltAngle = setPoint + maxAbsTilt;
                minTiltAngle = setPoint - maxAbsTilt;

                // if tilt angle is within minTiltAngle and maxTiltAngle, use PD controller, else use bouncing non-linear controller
                if(thetaDeg < maxTiltAngle && thetaDeg > minTiltAngle){

                    linearController();
                    stuckCount = 0;
                }
                else if(stuckCount < 500){
                    linearController();
                    stuckCount = stuckCount + 1;
                }
                else{
                    output = 0;
                    stuckCount = 0;
                    abcvlibMotion.setWheelSpeed(output, output);
                    try {
                        TimeUnit.MILLISECONDS.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                abcvlibMotion.setWheelSpeed(output, output);

            }
        }

        private void linearController(){

            try {
                setPoint = Double.parseDouble(controls.get("setPoint").toString());
                maxAbsTilt = Double.parseDouble(controls.get("maxAbsTilt").toString());
                k_p = Double.parseDouble(controls.get("k_p").toString());
                k_i = Double.parseDouble(controls.get("k_i").toString());
                k_d = Double.parseDouble(controls.get("k_d").toString());
            } catch (JSONException e){
                e.printStackTrace();
            }

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
        Context context;
        double currentTime = 0.0;
        double prevTime = 0.0;


        public PythonControl(Context context){
            this.context = context;
        }

        public void run(){

            while(!socketClient.ready) {
                continue;
            }
            while (true){

                readControlData();
//                System.out.println(controls);
                writeAndroidData();
//                System.out.println(inputs);

            }
        }

        private void readControlData(){
            controls = socketClient.getControlsFromServer();
            try {
                currentTime = Double.parseDouble(controls.get("timeServer").toString());
                double dt = currentTime - prevTime;
//                System.out.println(dt);
//                System.out.println("wheelSpeedL:" + controls.get("wheelSpeedL"));
                prevTime = currentTime;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void writeAndroidData(){

            try {
                inputs.put("timeAndroid", System.nanoTime() / 1000000000.0);
                inputs.put("theta", abcvlibSensors.getThetaDeg());
                inputs.put("thetaDot", abcvlibSensors.getThetaDegDot());
                inputs.put("wheelCountL", abcvlibQuadEncoders.getWheelCountL());
                inputs.put("wheelCountR", abcvlibQuadEncoders.getWheelCountR());
                inputs.put("distanceL", abcvlibQuadEncoders.getDistanceL());
                inputs.put("distanceR", abcvlibQuadEncoders.getDistanceR());
                inputs.put("wheelSpeedL", abcvlibQuadEncoders.getWheelSpeedL());
                inputs.put("wheelSpeedR", abcvlibQuadEncoders.getWheelSpeedR());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            socketClient.writeInputsToServer(inputs);
        }
    }

    private JSONObject initializeInputs(){

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("timeAndroid", 0.0);
            jsonObject.put("theta", 0.0);
            jsonObject.put("thetaDot", 0.0);
            jsonObject.put("wheelCountL", 0.0);
            jsonObject.put("wheelCountR", 0.0);
            jsonObject.put("distanceL", 0.0);
            jsonObject.put("distanceR", 0.0);
            jsonObject.put("wheelSpeedL", 0.0);
            jsonObject.put("wheelSpeedR", 0.0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    private JSONObject initializeControls(){

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("k_p", 0.0);
            jsonObject.put("k_i", 0.0);
            jsonObject.put("k_d", 0.0);
            jsonObject.put("setPoint", 0.0);
            jsonObject.put("wheelSpeedLControl", 0.0);
            jsonObject.put("wheelSpeedRControl", 0.0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }
}
