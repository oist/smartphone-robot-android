package jp.oist.abcvlib.colorblobdetect;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.android.CameraBridgeViewBase;

import android.content.Context;
import android.os.Bundle;
import android.view.Window;

import jp.oist.abcvlib.basic.AbcvlibActivity;
import jp.oist.abcvlib.basic.AbcvlibSocketClient;

public class ColorBlobDetectionActivity extends AbcvlibActivity {


    private List<Point>          centroids;
    private JSONObject controls;
    private JSONObject inputs;
    double p_tilt = 0;
    double i_tilt = 0;
    double d_tilt = 0;
    double setPoint = 0;
    double p_wheel = 0;
    double p_phi = 0;
    private AbcvlibSocketClient socketClient = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.color_blob_detection_surface_view);
        super.mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        super.onCreate(savedInstanceState);
        super.loggerOn = false;
        super.wheelPolaritySwap = true;
        controls = initializeControls();
        inputs = initializeInputs();

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

    /**
     * This is the method that loops every camera frame within OpenCV. Override here to do what you want
     * with the inputFrame object.
     * @param inputFrame
     * @return
     */
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // Take the inputFrame and convert to an RGB matrix object.
        mRgba = inputFrame.rgba();

        // Only process blobs after color has been selected.
        if (mIsColorSelected) {
            mDetector.process(mRgba);
            contours = mDetector.getContours();
            super.onCameraFrame(inputFrame);
            centroids = vision.Centroids(contours);
            vision.centerBlob(centroids, CENTER_ROW, CENTER_THRESHOLD);
        }

        return mRgba;
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

        double error_theta = 0; // e(t) of wikipedia
        double error_wheelSpeed = 0; // error betweeen actual and desired wheel speed (default 0)

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

        double error_wheelSpeedL;
        double error_wheelSpeedR;
        double error_phi = 0;

        double p_outL;
        double i_outL;
        double d_outL;
        double p_outR;
        double i_outR;
        double d_outR;

        int outputL;
        int outputR;

        public void run(){

            while(true) {

                thetaDiff = thetaDeg - abcvlibSensors.getThetaDeg();

                if (thetaDiff !=0){
                    updateTimeStep = PIDTimer[0]- lastUpdateTime;
                    lastUpdateTime = PIDTimer[0];
                }

                thetaDeg = abcvlibSensors.getThetaDeg();
                thetaDegDot = abcvlibSensors.getThetaDegDot();
                wheelCountL = abcvlibQuadEncoders.getWheelCountL();
                wheelCountR = abcvlibQuadEncoders.getWheelCountR();
                distanceL = abcvlibQuadEncoders.getDistanceL();
                distanceR = abcvlibQuadEncoders.getDistanceR();
                speedL = abcvlibQuadEncoders.getWheelSpeedL();
                speedR = abcvlibQuadEncoders.getWheelSpeedR();

                linearController();
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
                p_phi = Double.parseDouble(controls.get("p_phi").toString());

            } catch (JSONException e){
                e.printStackTrace();
            }

            // TODO this needs to account for length of time on each interval, or overall time length. Here this just assumes a width of 1 for all intervals.
            int_e_t = int_e_t + error_theta;
            error_theta = setPoint - thetaDeg;

            error_wheelSpeedL = 0.0 - speedL;
            error_wheelSpeedR = 0.0 - speedR;
            //TODO this polarity may be wrong.
            if (centroids != null && centroids.size() > 0){
                error_phi = 0.0 - vision.getPhi(centroids);
            }

            p_outL = (p_tilt * error_theta) + (p_wheel * error_wheelSpeedL) + (p_phi * error_phi);
            i_outL = i_tilt * int_e_t;
            d_outL = d_tilt * thetaDegDot;
            p_outR = (p_tilt * error_theta) + (p_wheel * error_wheelSpeedR) - (p_phi * error_phi);
            i_outR = i_tilt * int_e_t;
            d_outR = d_tilt * thetaDegDot;

            outputL = (int)(p_outL + d_outL);
            outputR = (int)(p_outR + d_outR);

            abcvlibMotion.setWheelSpeed(outputL, outputR);

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
            jsonObject.put("wheelSpeedL", 0.0);
            jsonObject.put("wheelSpeedR", 0.0);
            jsonObject.put("setPoint", 0.0);
            jsonObject.put("maxAbsTilt", 10.0);
            jsonObject.put("bounceFreq", 0.0);
            jsonObject.put("p_tilt", 0.0);
            jsonObject.put("i_tilt", 0.0);
            jsonObject.put("d_tilt", 0.0);
            jsonObject.put("p_wheel", 0.0);
            jsonObject.put("p_phi", 0.0);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }


}
