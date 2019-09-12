package jp.oist.abcvlib.localpid;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.R;
import jp.oist.abcvlib.basic.AbcvlibActivity;
import jp.oist.abcvlib.basic.AbcvlibSocketClient;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
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
    private boolean wheelPolaritySwap = true;

    double p_tilt = 0;
    double i_tilt = 0;
    double d_tilt = 0;
    double setPoint = 0;
    double p_wheel = 0;
    private String[] controlParams = {"p_tilt", "i_tilt", "d_tilt", "setPoint", "p_wheel", "wheelSpeedL", "wheelSpeedR"};

    private String androidData = "androidData";
    private String controlData = "controlData";
    private JSONObject inputs = initializeInputs();
    private JSONObject controls = initializeControls();
    private AbcvlibSocketClient socketClient = null;

    private int avgCount = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
        super.loggerOn = loggerOn;
        super.wheelPolaritySwap = wheelPolaritySwap;

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(jp.oist.abcvlib.localpid.R.layout.activity_main);

        // Python Socket Connection
        socketClient = new AbcvlibSocketClient("192.168.24.217", 65434, inputs, controls);
        new Thread(socketClient).start();

        //PID Controller
        PID pidThread = new PID();
        new Thread(pidThread).start();

        // PythonControl
        PythonControl pythonControl = new PythonControl(this);
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

        int output; //  u(t) of wikipedia

        double e_t = 0; // e(t) of wikipedia
        double e_w = 0; // error betweeen actual and desired wheel speed (default 0)

        double int_e_t; // integral of e(t) from wikipedia. Discrete, so just a sum here.

        double maxAbsTilt = 10; // in Degrees
        double maxTiltAngle = setPoint + maxAbsTilt; // in Degrees
        double minTiltAngle = setPoint - maxAbsTilt; // in Degrees

        private int stuckCount = 0;

        long[] PIDTimer = new long[3];
        long[] PIDTimeSteps = new long[4];
        int timerCount = 1;
        double thetaDiff;
        long lastUpdateTime;
        long updateTimeStep;

        public void run(){

            while(true) {

                PIDTimer[0] = System.nanoTime();

                thetaDiff = thetaDeg - abcvlibSensors.getThetaDeg();

                if (thetaDiff !=0){
                    updateTimeStep = PIDTimer[0]- lastUpdateTime;
                    lastUpdateTime = PIDTimer[0];
//                    Log.i("theta", "theta was updated in " + (updateTimeStep / 1000000) + " ms");
                }

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

                PIDTimer[1] = System.nanoTime();

                // if tilt angle is within minTiltAngle and maxTiltAngle, use PD controller, else use bouncing non-linear controller
                if(thetaDeg < maxTiltAngle && thetaDeg > minTiltAngle){

                    linearController();
                    stuckCount = 0;
                }
                else if(stuckCount < 4000){
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

                PIDTimer[2] = System.nanoTime();

                abcvlibMotion.setWheelSpeed(output, output);

                PIDTimeSteps[3] += System.nanoTime() - PIDTimer[2];
                PIDTimeSteps[2] += PIDTimer[2] - PIDTimer[1];
                PIDTimeSteps[1] += PIDTimer[1] - PIDTimer[0];
                PIDTimeSteps[0] = PIDTimer[0];


                // Take basic stats of every 1000 time step lengths rather than pushing all.
                if (timerCount % avgCount == 0){

                    for (int i=1; i < PIDTimeSteps.length; i++){

                        PIDTimeSteps[i] = (PIDTimeSteps[i] / avgCount) / 1000000;

                    }

//                    Log.i("timers", "PIDTimer Averages = " + Arrays.toString(PIDTimeSteps));
                }

                timerCount ++;

            }
        }

        private void linearController(){

            try {
                setPoint = Double.parseDouble(controls.get("setPoint").toString());
                maxAbsTilt = Double.parseDouble(controls.get("maxAbsTilt").toString());
                p_tilt = Double.parseDouble(controls.get("p_tilt").toString());
                i_tilt = Double.parseDouble(controls.get("i_tilt").toString());
                d_tilt = Double.parseDouble(controls.get("d_tilt").toString());
                p_wheel = Double.parseDouble(controls.get("p_wheel").toString());

            } catch (JSONException e){
                e.printStackTrace();
            }

            // TODO this needs to account for length of time on each interval, or overall time length. Here this just assumes a width of 1 for all intervals.
            int_e_t = int_e_t + e_t;
            e_t = setPoint - thetaDeg;
            e_w = 0.0 - speedL;

            double p_out = (p_tilt * e_t) + (p_wheel * e_w);
            double i_out = i_tilt * int_e_t;
            double d_out = d_tilt * thetaDegDot;

            output = (int)(p_out + i_out + d_out);

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

        float[] pythonControlTimer = new float[3];
        float[] pythonControlTimeSteps = new float[3];

        int timerCount = 1;

        public PythonControl(Context context){
            this.context = context;
        }

        public void run(){

            while(!socketClient.ready) {
                continue;
            }
            while (true){

                pythonControlTimer[0] = System.nanoTime();
                readControlData();
//                System.out.println(controls);
                pythonControlTimer[1] = System.nanoTime();
                writeAndroidData();
//                System.out.println(inputs);
                pythonControlTimeSteps[2] += System.nanoTime() - pythonControlTimer[1];
                pythonControlTimeSteps[1] += pythonControlTimer[1] - pythonControlTimer[0];
                pythonControlTimeSteps[0] = pythonControlTimer[0];


                // Take basic stats of every 1000 time step lengths rather than pushing all.
                if (timerCount % avgCount == 0){

                    for (int i=1; i < pythonControlTimeSteps.length; i++){

                        pythonControlTimeSteps[i] = (pythonControlTimeSteps[i] / avgCount) / 1000000;

                    }

                    Log.i("timers", "PythonControlTimer Averages = " + Arrays.toString(pythonControlTimeSteps) + "(ms)");

                }

                timerCount ++;


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
            jsonObject.put("wheelSpeedL", 0.0);
            jsonObject.put("wheelSpeedR", 0.0);
            jsonObject.put("setPoint", 0.0);
            jsonObject.put("maxAbsTilt", 10.0);
            jsonObject.put("bounceFreq", 0.0);
            jsonObject.put("p_tilt", 0.0);
            jsonObject.put("i_tilt", 0.0);
            jsonObject.put("d_tilt", 0.0);
            jsonObject.put("p_wheel", 0.0);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }
}
