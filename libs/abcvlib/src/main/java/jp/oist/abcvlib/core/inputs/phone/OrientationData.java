package jp.oist.abcvlib.core.inputs.phone;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;

/**
 * MotionSensors reads and processes the data from the Android phone gryoscope and
 * accelerometer. The three main goals of this class are:
 *
 * 1.) To estimate the tilt angle of the phone by combining the input from the
 * gyroscope and accelerometer
 * 2.) To calculate the speed of each wheel (speedRightWheel and speedLeftWheel) by using
 * existing and past quadrature encoder states
 * 3.) Provides the get() methods for all relevant sensory variables
 *
 * This thread typically updates every 5 ms, but this depends on the
 * SensorManager.SENSOR_DELAY_FASTEST value. This represents the fastest possible sampling rate for
 * the sensor. Note this value can change depending on the Android phone used and corresponding
 * internal sensor hardware.
 *
 * A counter is keep track of the number of sensor change events via sensorChangeCount.
 *
 * @author Jiexin Wang https://github.com/ha5ha6
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class OrientationData implements SensorEventListener, AbcvlibInput {
    private AbcvlibActivity abcvlibActivity;

    //----------------------------------------- Counters -------------------------------------------
    /**
     * Keeps track of current history index.
     * indexCurrent calculates the correct index within the time history arrays in order to
     * continuously loop through and rewrite the encoderCountHistoryLength indexes.
     * E.g. if sensorChangeCount is 15 and encoderCountHistoryLength is 15, then indexCurrent
     * will resolve to 0 and the values for each history array will be updated from index 0 until
     * sensorChangeCount exceeds 30 at which point it will loop to 0 again, so on and so forth.
     */
    private int indexCurrentRotation = 1;
    private int indexPreviousRotation = 0;
    /**
     * Keeps track of current history index.
     * indexCurrent calculates the correct index within the time history arrays in order to
     * continuously loop through and rewrite the encoderCountHistoryLength indexes.
     * E.g. if sensorChangeCount is 15 and encoderCountHistoryLength is 15, then indexCurrent
     * will resolve to 0 and the values for each history array will be updated from index 0 until
     * sensorChangeCount exceeds 30 at which point it will loop to 0 again, so on and so forth.
     */
    private int indexCurrentGyro = 1;
    private int indexPreviousGyro = 0;
    /**
     * Length of past timestamps and encoder values you keep in memory. 15 is not significant,
     * just what was deemed appropriate previously.
     */
    private int windowLength = 3;
    /**
     * Low Pass Filter cutoff freq
     */
    private double lp_freq_theta = 1000;
    private double lp_freq_thetaDot = 1000;
    /**
     * Total number of times the sensors have changed data
     */
    private int sensorChangeCountGyro = 1;
    private int sensorChangeCountRotation = 1;
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------

    // Android Sensor objects
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor rotation_sensor;
    private Sensor accelerometer;
    private Sensor accelerometer_uncalibrated;

    //----------------------------------------------------------------------------------------------
    /**
     * orientation vector See link below for android doc
     * https://developer.android.com/reference/android/hardware/SensorManager.html#getOrientation(float%5B%5D,%2520float%5B%5D)
     */
    private float[] orientation = new float[3];
    /**
     * thetaRad calculated from rotation vector
     */
    private double[] thetaRad = new double[windowLength];
    /**
     * thetaRad converted to degrees.
     */
    private double[] thetaDeg = new double[windowLength];
    /**
     * rotation matrix
     */
    private float[] rotationMatrix = new float[16];
    /**
     * rotation matrix remapped
     */
    private float[] rotationMatrixRemap = new float[16];
    /**
     * angularVelocity calculated from RotationMatrix.
     */
    private double[] angularVelocityRad = new double[windowLength];
    /**
     * angularVelocityRad converted to degrees.
     */
    private double[] angularVelocityDeg = new double[windowLength];
    private double[] thetaDotGyro = new double[windowLength];
    private double[] timeStampsGyro = new double[windowLength];
    private double[] thetaDotGyroDeg = new double[windowLength];
    private double dtGyro = 0;
    private long[] delayTimers = new long[5];
    private long[] delayTimeSteps = new long[5];
    private float[] theta_uncalibrated = new float[6];

    float[] pythonSensorTimer = new float[3];
    float[] pythonSensorTimeSteps = new float[3];
    int timerCount = 1;
    private int avgCount = 1000;
    private boolean isRecording = false;
    private TimeStepDataBuffer timeStepDataBuffer = null;

    //----------------------------------------------------------------------------------------------

    //----------------------------------------- Timestamps -----------------------------------------
    /**
     * Keeps track of both gyro and accelerometer sensor timestamps
     */
    private long timeStamps[] = new long[windowLength];
    /**
    indexHistoryOldest calculates the index for the oldest encoder count still within
    the history. Using the most recent historical point could lead to speed calculations of zero
    in the event the quadrature encoders slip/skip and read the same wheel count two times in a
    row even if the wheel is moving with a non-zero speed.
     */
    int indexHistoryOldest = 0; // Keeps track of oldest history index.
    double dt = 0;

    //----------------------------------------------------------------------------------------------

    /**
     * Constructor that sets up Android Sensor Service and creates Sensor objects for both
     * accelerometer and gyroscope. Then registers both sensors such that their onSensorChanged
     * events will call the onSensorChanged method within this class.
     */
    public OrientationData(AbcvlibActivity abcvlibActivity){
        this.abcvlibActivity = abcvlibActivity;
        this.timeStepDataBuffer = abcvlibActivity.getTimeStepDataAssembler().getTimeStepDataBuffer();
        sensorManager = (SensorManager) abcvlibActivity.getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotation_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            accelerometer_uncalibrated = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);
        }
        register();
    }

    public void setTimeStepDataBuffer(TimeStepDataBuffer timeStepDataBuffer) {
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }

    /**
     * Assume this is only used for sensors that have the ability to change accuracy (e.g. GPS)
     * @param sensor Sensor object that has changed its accuracy
     * @param accuracy Accuracy. See SensorEvent on Android Dev documentation for details
     */
    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy){
        // Not sure if we need to worry about this. I think this is more for more variable sensors like GPS but could be wrong.
    }

    /**
     * This is called every time a registered sensor provides data. Sensor must be registered before
     * it will fire the event which calls this method. If statements handle differentiating between
     * accelerometer and gyrscope events.
     * @param event SensorEvent object that has updated its output
     */
    @Override
    public void onSensorChanged(SensorEvent event){

        Sensor sensor = event.sensor;

        // Timer for how often ANY sensor changes
        pythonSensorTimeSteps[0] += System.nanoTime() - pythonSensorTimer[0];


//        if(sensor.getType()==Sensor.TYPE_GYROSCOPE){
//
//            indexCurrentGyro = sensorChangeCountGyro % windowLength;
//            indexPreviousGyro = (sensorChangeCountGyro - 1) % windowLength;
//            // Rotation around x-axis
//            // See https://developer.android.com/reference/android/hardware/SensorEvent.html
//            thetaDotGyro = event.values[0];
//            thetaDotGyroDeg = (thetaDotGyro * (180 / Math.PI));
//            timeStampsGyro[indexCurrentGyro] = event.timestamp;
//            dtGyro = (timeStampsGyro[indexCurrentGyro] - timeStampsGyro[indexPreviousGyro]) / 1000000000f;
//            sensorChangeCountGyro++;
//            if (loggerOn){
//                sendToLog();
//            }
//
//        }
        if(sensor.getType()==Sensor.TYPE_ROTATION_VECTOR){

            // Timer for only TYPE_ROTATION_VECTOR sensor change
            pythonSensorTimeSteps[1] += System.nanoTime() - pythonSensorTimer[1];

            indexCurrentRotation = sensorChangeCountRotation % windowLength;
            indexPreviousRotation = (sensorChangeCountRotation - 1) % windowLength;
            indexHistoryOldest = (sensorChangeCountRotation + 1) % windowLength;
            timeStamps[indexCurrentRotation] = event.timestamp;
            dt = (timeStamps[indexCurrentRotation] - timeStamps[indexPreviousRotation]) / 1000000000f;

            SensorManager.getRotationMatrixFromVector(rotationMatrix , event.values);
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_Z, rotationMatrixRemap);
            SensorManager.getOrientation(rotationMatrixRemap, orientation);
            thetaRad[indexCurrentRotation] = orientation[1]; //Pitch
//            thetaRad = lowpassFilter(thetaRad, dt_sample, lp_freq_theta);

            angularVelocityRad[indexCurrentRotation] = (thetaRad[indexCurrentRotation] - thetaRad[indexPreviousRotation]) / dt;
//            angularVelocityRad = lowpassFilter(angularVelocityRad, 0.005, lp_freq_thetaDot);
//            if (sensorChangeCountRotation > windowLength){
//                angularVelocityRad[indexCurrentRotation] = runningAvg(angularVelocityRad, 3);
//            }

            thetaDeg[indexCurrentRotation] = (thetaRad[indexCurrentRotation] * (180 / Math.PI));
            angularVelocityDeg[indexCurrentRotation] = (thetaDeg[indexCurrentRotation] - thetaDeg[indexPreviousRotation]) / dt;

            // Update all previous variables with current ones
            sensorChangeCountRotation++;
            if (abcvlibActivity.switches.loggerOn){
                sendToLog();
            }

            pythonSensorTimer[1] = System.nanoTime();
        }

        else if (sensor.getType()==Sensor.TYPE_ACCELEROMETER){

            // Timer for only TYPE_ACCELEROMETER sensor change
            pythonSensorTimeSteps[2] += System.nanoTime() - pythonSensorTimer[2];

            delayTimeSteps[3] = (System.nanoTime() - delayTimers[3]) / 1000000;
            delayTimers[3] = System.nanoTime();

            pythonSensorTimer[2] = System.nanoTime();

        }

        pythonSensorTimer[0] = System.nanoTime();

        // Take basic stats of every 1000 time step lengths rather than pushing all.
        if (timerCount % avgCount == 0){

            for (int i=0; i < pythonSensorTimeSteps.length; i++){

                pythonSensorTimeSteps[i] = (pythonSensorTimeSteps[i] / avgCount) / 1000000;

            }

//            Log.i("timers", "PythonSensorTimer Averages = " + Arrays.toString(pythonSensorTimeSteps) + "(ms)");

        }

        timerCount ++;

        if(isRecording){
            timeStepDataBuffer.getWriteData().getOrientationData().put(timeStamps[indexCurrentRotation],
                    thetaRad[indexCurrentRotation],
                    angularVelocityRad[indexCurrentRotation]);
        }

        // Todo: does this sleep the main thread or is this running on something else at this point?
        Thread.yield();


//        Log.i("Sensor Delay Timer", "Time between last sensor changes: " + Arrays.toString(delayTimeSteps));
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    /**
     Registering sensorEventListeners for accelerometer and gyroscope only.
     */
    public void register(){
        // Check if rotation_sensor exists before trying to turn on the listener
        if (rotation_sensor != null){
            sensorManager.registerListener(this, rotation_sensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Log.e("SensorTesting", "No Default rotation_sensor Available.");
        }
        // Check if gyro exists before trying to turn on the listener
        if (gyroscope != null){
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Log.e("SensorTesting", "No Default gyroscope Available.");
        }
        // Check if rotation_sensor exists before trying to turn on the listener
        if (accelerometer != null){
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Log.e("SensorTesting", "No Default accelerometer Available.");
        }
        // Check if rotation_sensor exists before trying to turn on the listener
        if (accelerometer_uncalibrated != null){
            sensorManager.registerListener(this, accelerometer_uncalibrated, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Log.e("SensorTesting", "No Default accelerometer_uncalibrated Available.");
        }
    }

    /**
     * Check if accelerometer and gyroscope objects still exist before trying to unregister them.
     * This prevents null pointer exceptions.
     */
    public void unregister(){
        // Check if rotation_sensor exists before trying to turn off the listener
        if (rotation_sensor != null){
            sensorManager.unregisterListener(this, rotation_sensor);
        }
        // Check if gyro exists before trying to turn off the listener
        if (gyroscope != null){
            sensorManager.unregisterListener(this, gyroscope);
        }
        // Check if rotation_sensor exists before trying to turn off the listener
        if (accelerometer != null){
            sensorManager.unregisterListener(this, accelerometer);
        }
        // Check if gyro exists before trying to turn off the listener
        if (accelerometer_uncalibrated != null){
            sensorManager.unregisterListener(this, accelerometer_uncalibrated);
        }

    }

    /**
     * @return Phone tilt angle in radians
     */
    public double getThetaRad(){ return thetaRad[indexCurrentRotation]; }

    /**
     * @return Phone tilt angle in degrees
     */
    public double getThetaDeg(){
        return thetaDeg[indexCurrentRotation];
    }

    /**
     * @return Phone tilt speed (angular velocity) in radians per second
     */
    public double getThetaRadDot(){ return angularVelocityRad[indexCurrentRotation]; }

    /**
     * @return Phone tilt speed (angular velocity) in degrees per second
     */
    public double getThetaDegDot(){
        return angularVelocityDeg[indexCurrentRotation];
    }

    /**
     * @return Total combined count for how many times the accelerometer and gyroscope have provided
     * data.
     */
    int getSensorChangeCount() {return sensorChangeCountRotation;}

    /**
     * Sets the history length for which to base the derivative functions off of (angular velocity,
     * linear velocity).
     * @param len length of array for keeping history
     */
    public void setWindowLength(int len) {
        windowLength = len; }

    /**
     * Send accelerometer and gyroscope data, along with calculated tilt angles, speeds, etc. such
     * that they can be read by the sensor data graphing utility.
     */
    private void sendToLog() {

        // Compile thetaDegVectorMsg values to push to separate adb tag
        String thetaMsg = Double.toString(thetaDeg[indexCurrentRotation]);

        // Compile thetaDegVectorMsg values to push to separate adb tag
        String angularVelocityRadMsg = Double.toString(angularVelocityRad[indexCurrentRotation]);

//        // Compile thetaDegVectorMsg values to push to separate adb tag
//        String thetaVectorVelMsg = Double.toString(angularVelocityDeg);
//
//        // Compile dt_sample values to push to separate adb tag
//        String dtRotationMsg = Double.toString(dt_sample);
//
//        // Compile dt_sample values to push to separate adb tag
//        String thetaDotGyroMsg = Double.toString(thetaDotGyro);
//
//        // Compile dt_sample values to push to separate adb tag
//        String dtGyroMsg = Double.toString(dtGyro);

//        Log.i("thetaMsg", thetaMsg);
//        Log.i("angularVelocityRadMsg", angularVelocityRadMsg);
//        Log.i("thetaVectorVelMsg", thetaVectorVelMsg);
//        Log.i("dtRotation", dtRotationMsg);
//        Log.i("thetaDotGyroMsg", thetaDotGyroMsg);
//        Log.i("dtGyroMsg", dtGyroMsg);

    }

    /**
     * This seems to be a very convoluted way to do this, but it seems to work just fine
     * @param angle Tilt angle in radians
     * @return Wrapped angle in radians from -Pi to Pi
     */
    private float wrapAngle(float angle){
        while(angle<-Math.PI)
            angle+=2*Math.PI;
        while(angle>Math.PI)
            angle-=2*Math.PI;
        return angle;
    }

}
