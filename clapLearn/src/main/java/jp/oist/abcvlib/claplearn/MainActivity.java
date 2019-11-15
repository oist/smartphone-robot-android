package jp.oist.abcvlib.claplearn;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.style.TtsSpan;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import jp.oist.abcvlib.basic.AbcvlibActivity;
import jp.oist.abcvlib.basic.AbcvlibSocketClient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
import java.text.DecimalFormat;
import java.util.Timer;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;


/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements MicrophoneInputListener {

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
    private MediaPlayerThread mediaPlayerThread;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    private int avgCount = 1000;
    private boolean pidON = false;


    // Google SPL Meter code below
    MicrophoneInput micInput;  // The micInput object provides real time audio.
    TextView mdBTextView;
    TextView mdBFractionTextView;
    BarLevelDrawable mBarLevel;
    private TextView mGainTextView;

    double mOffsetdB = 10;  // Offset for bar, i.e. 0 lit LEDs at 10 dB.
    // The Google ASR input requirements state that audio input sensitivity
    // should be set such that 90 dB SPL at 1000 Hz yields RMS of 2500 for
    // 16-bit samples, i.e. 20 * log_10(2500 / mGain) = 90.
    double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
    // For displaying error in calibration.
    double mDifferenceFromNominal = 0.0;
    double mRmsSmoothed;  // Temporally filtered version of RMS.
    double mAlpha = 0.9;  // Coefficient of IIR smoothing filter for RMS.
    private int mSampleRate;  // The audio sampling rate to use.
    private int mAudioSource;  // The audio source to use.
    private double rmsdB; // rmsdB value each loop. Written by processAudioFrame read by separate thread in ActionSelector

    // Variables to monitor UI update and check for slow updates.
    private volatile boolean mDrawing;
    private volatile int mDrawingCollided;

    private static final String TAG = "LevelMeterActivity";

    final double alpha = 0.1; // learning rate
    final double beta = 3.0; // temperature
    double reward; // reward based on spl
    double rewardSum = 0;
    double rewardAvg = 0;
    int ellapsedLoops = 1; // For use in the moving average of the reward.
    double rewardScaleFactor = (100.0); // (mRmsSmoothed / rewardScaleFactor) - rewardOffset
    double rewardOffset = 1.5;
    int timeRemaining = 0;

    ActionDistribution aD = new ActionDistribution();

    DecimalFormat formatter = new DecimalFormat("#0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
        super.loggerOn = loggerOn;
        super.wheelPolaritySwap = wheelPolaritySwap;

        readPreferences();
        // Google Code Below
        // Here the micInput object is created for audio capture.
        // It is set up to call this object to handle real time audio frames of
        // PCM samples. The incoming frames will be handled by the
        // processAudioFrame method below.
        micInput = new MicrophoneInput(this);
        micInput.setSampleRate(mSampleRate);
        micInput.setAudioSource(mAudioSource);

        // Read the layout and construct.
        setContentView(R.layout.main);

        // Get a handle that will be used in async thread post to update the
        // display.
        mBarLevel = (BarLevelDrawable)findViewById(R.id.bar_level_drawable_view);
        mdBTextView = (TextView)findViewById(R.id.dBTextView);
        mdBFractionTextView = (TextView)findViewById(R.id.dBFractionTextView);

        micInput.start();

        mediaPlayerThread = new MediaPlayerThread();
        new Thread(mediaPlayerThread).start();
//        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
//        // ID within the R class
//        setContentView(jp.oist.abcvlib.claplearn.R.layout.main);

        // Python Socket Connection. Host IP:Port needs to be the same as python server.
        // Todo: automatically detect host server or set this to static IP:Port. Tried UDP Broadcast,
        //  but seems to be blocked by router. Could set up DNS and static hostname, but would
        //  require intervention with IT
        socketClient = new AbcvlibSocketClient("192.168.28.151", 65434, inputs, controls);
        new Thread(socketClient).start();

        //PID Controller
        PID pidThread = new PID();
        new Thread(pidThread).start();

        // PythonControl
        PythonControl pythonControl = new PythonControl(this);
        new Thread(pythonControl).start();

        // SPL Meter
        ActionSelector actionSelector = new ActionSelector();
        new Thread(actionSelector).start();

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

        double maxAbsTilt = 30; // in Degrees
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

                linearController();

                PIDTimer[2] = System.nanoTime();

                if (pidON) {
                    abcvlibMotion.setWheelSpeed(output, output);
                }

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

        JSONArray qvalueArray = new JSONArray();
        JSONArray weightArray = new JSONArray();
        int arrayIndex = 0;

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
                inputs.put("action", aD.get_selectedAction());
                inputs.put("reward", reward);
                inputs.put("rewardAvg", rewardAvg);
                inputs.put("timeRemaining", timeRemaining);

                Log.i("abcvlib", "writeAndroidData reward: " + reward);

                arrayIndex = 0;
                for (double qvalue : aD.qValues){
                    qvalueArray.put(arrayIndex, qvalue);
                    arrayIndex++;
                }
                arrayIndex = 0;
                for (double weight : aD.weights){
                    weightArray.put(arrayIndex, weight);
                    arrayIndex++;
                }

                inputs.put("weights", weightArray);
                inputs.put("qvalues", qvalueArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            socketClient.writeInputsToServer(inputs);
        }
    }

    /**
     * Method to read the sample rate and audio source preferences.
     */
    private void readPreferences() {
        SharedPreferences preferences = getSharedPreferences("LevelMeter",
                MODE_PRIVATE);
        Log.i("abcvlib", "mSampleRateBefore:" + mSampleRate);
        mSampleRate = preferences.getInt("SampleRate", 16000);
        Log.i("abcvlib", "mSampleRateAfter:" + mSampleRate);
        mAudioSource = preferences.getInt("AudioSource",
                MediaRecorder.AudioSource.VOICE_RECOGNITION);
    }

    /**
     *  This method gets called by the micInput object owned by this activity.
     *  It first computes the RMS value and then it sets up a bit of
     *  code/closure that runs on the UI thread that does the actual drawing.
     */
    @Override
    public void processAudioFrame(short[] audioFrame) {
        if (!mDrawing) {
            mDrawing = true;
            // Compute the RMS value. (Note that this does not remove DC).
            double rms = 0;
            for (int i = 0; i < audioFrame.length; i++) {
                rms += audioFrame[i] * audioFrame[i];
            }
            rms = Math.sqrt(rms / audioFrame.length);

            // Compute a smoothed version for less flickering of the display.
            mRmsSmoothed = (mRmsSmoothed * mAlpha) + (1 - mAlpha) * rms;
            rmsdB = 20 + (20.0 * Math.log10(mGain * mRmsSmoothed));

            if (rmsdB != 0) {
                reward = ((mRmsSmoothed / rewardScaleFactor) - rewardOffset);
                rewardSum = rewardSum + ((mRmsSmoothed / rewardScaleFactor) - rewardOffset);
                rewardAvg = rewardSum / ellapsedLoops;

//                Log.i("abcvlib", "AudioFrame reward=" + reward + "; " +
//                        "rewardSum=" + rewardSum + "; mRmsSmoothed=" + mRmsSmoothed +
//                        "; ellapsedLoops=" + ellapsedLoops);

                ellapsedLoops++;


                // Set up a method that runs on the UI thread to update of the LED bar
                // and numerical display.

                mBarLevel.post(new Runnable() {
                    @Override
                    public void run() {
                        // The bar has an input range of [0.0 ; 1.0] and 10 segments.
                        // Each LED corresponds to 6 dB.
                        mBarLevel.setLevel((mOffsetdB + rmsdB) / 60);

                        DecimalFormat df = new DecimalFormat("##");
                        mdBTextView.setText(df.format(rmsdB));

                        DecimalFormat df_fraction = new DecimalFormat("#");
                        int one_decimal = (int) (Math.round(Math.abs(rmsdB * 10))) % 10;
                        mdBFractionTextView.setText(Integer.toString(one_decimal));
                        mDrawing = false;
                    }
                });
            }
            else {
                Log.i("abcvlib", "rmsdB=0");
            }
        }
        else {
            mDrawingCollided++;
            Log.v(TAG, "Level bar update collision, i.e. update took longer " +
                        "than 20ms. Collision count" + Double.toString(mDrawingCollided));
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

    public class ActionSelector implements Runnable{

        int speed = 100; // Duty Cycle from 0 to 100.
        long actionCycle = 5000; // How long each action is carried out in ms
        int selectedAction;

        public void run() {
            while(true) {

                selectedAction = aD.actionSelect();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {

                            public void onTick(long millisUntilFinished) {
                                timeRemaining = Math.round(millisUntilFinished / 1000);
                            }

                            public void onFinish() {

                            }
                        }.start();
                    }

                });

                sendToGUI();

                switch (selectedAction){
                    case 0:
                        turnLeft();
                        break;
                    case 1:
                        turnRight();
                        break;
                    case 2:
                        balance();
                        break;
                    default:
                        Log.i("distribution", "default case selected with action = " + selectedAction);
                }

                // Update Q-value at the end of each.
                updateValues();

            }
        }

        private void sendToGUI(){
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                TextView actionView = findViewById(R.id.actionView);
                actionView.setText("A:" + String.valueOf(selectedAction + 1));
                TextView weightView = findViewById(R.id.weightView);
                weightView.setText("W:" + String.format("%.2f", aD.weights[selectedAction]));
                }
            });
        }

        private void turnLeft(){
            pidON = false;
            abcvlibMotion.setWheelSpeed(0, 0);
            try {
                // Turn left
                abcvlibMotion.setWheelSpeed(speed / 3, speed);
                Thread.sleep(actionCycle);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void turnRight(){
            pidON = false;
            abcvlibMotion.setWheelSpeed(0, 0);
            try {
                // Turn left
                abcvlibMotion.setWheelSpeed(speed, speed / 3);
                Thread.sleep(actionCycle);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void balance(){
            try {
                abcvlibMotion.setWheelSpeed(0, 0);
                pidON = true;
                Thread.sleep(actionCycle);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void updateValues(){
            // Cap rewardAvg to some maximum to prevent kids from being TOO loud
            if (rewardAvg >= 2){
                rewardAvg = 2;
            }
            // update qValues due to current level
            aD.qValues[aD.get_selectedAction()] = (alpha * rewardAvg) + ((1.0 - alpha) * aD.qValues[aD.get_selectedAction()]);
            // update weights from new qValues
            aD.getQValuesSum();
            for (int i=0; i < aD.weights.length; i++){
                aD.weights[i] = Math.exp(beta * aD.qValues[i]) / aD.qValuesSum;
            }
            reward = -rewardOffset;
            rewardAvg = 0;
            rewardSum = 0;
            rmsdB = 0;
            mRmsSmoothed = 0;
            ellapsedLoops = 1;
        }
    }

    // TODO weights and selectedAction should be private to this, then an instance of this class should be global to MainActivity
    private class ActionDistribution{

        // Initial weights
        private int[] actions = {0, 1, 2};
        public double[] weights = {0.34, 0.34, 0.34};
        public double[] qValues = {0.1,0.1,0.1};

        private  double qValuesSum; // allotting memory for this as no numpy type array calcs available?
        private int _selectedAction;
        private double randNum;
        private double selector;
        private int iterator;
        private double sumOfWeights;

        public int actionSelect(){

            sumOfWeights = 0;
            for (double weight:weights){
                sumOfWeights = sumOfWeights + weight;
            }
            randNum = Math.random() * sumOfWeights;
            selector = 0;
            iterator = -1;

            while (selector < randNum){
                try {
                    iterator++;
                    selector = selector + weights[iterator];
                }catch (ArrayIndexOutOfBoundsException e){
                    Log.e("abcvlib", "weight index bound exceeded. randNum was greater than the sum of all weights. This can happen if the sum of all weights is less than 1.");
                }
            }
            // Assigning this as a read-only value to pass between threads.
            this._selectedAction = iterator;

            // represents the action to be selected
            return iterator;
        }

        public double getQValuesSum(){
            qValuesSum = 0;
            for (double qValue : qValues) {
                qValuesSum += Math.exp(beta * qValue);
            }
            return qValuesSum;
        }

        public int get_selectedAction() {
            return _selectedAction;
        }
    }

    private class MediaPlayerThread implements Runnable{

        public void run(){

            audioManager = (AudioManager) MainActivity.this.getSystemService(MainActivity.this.AUDIO_SERVICE);
            mediaPlayer = new MediaPlayer();
            Uri loc = Uri.parse("android.resource://jp.oist.abcvlib.claplearn/" + R.raw.custommix);

            try {
                mediaPlayer.setDataSource(MainActivity.this, loc);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
            } catch (IllegalArgumentException | SecurityException| IllegalStateException | IOException e) {
                e.printStackTrace();
            }
            audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mediaPlayer.start();

        }
    }

}
