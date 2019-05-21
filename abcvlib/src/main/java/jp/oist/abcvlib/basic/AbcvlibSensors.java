package jp.oist.abcvlib.basic;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * AbcvlibSensors reads and processes the data from the Android phone gryoscope and
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
public class AbcvlibSensors implements SensorEventListener {

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
    private int historyLength = 3;
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

    //----------------------------------------------------------------------------------------------
    /**
     * orientation vector See link below for android doc
     * https://developer.android.com/reference/android/hardware/SensorManager.html#getOrientation(float%5B%5D,%2520float%5B%5D)
     */
    private float[] orientation = new float[3];
    /**
     * thetaRad calculated from rotation vector
     */
    private double thetaRad = 0;
    /**
     * thetaRad converted to degrees.
     */
    private double thetaDeg = 0;
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
    private double angularVelocityRad = 0;
    /**
     * angularVelocityRad converted to degrees.
     */
    private double angularVelocityDeg = 0;
    private double thetaDotGyro = 0;
    private double[] timeStampsGyro = new double[historyLength];
    private double thetaDotGyroDeg = 0;
    private double dtGyro = 0;


    //----------------------------------------------------------------------------------------------

    //----------------------------------------- Timestamps -----------------------------------------
    /**
     * Keeps track of both gyro and accelerometer sensor timestamps
     */
    private long timeStamps[] = new long[historyLength];
    /**
    indexHistoryOldest calculates the index for the oldest encoder count still within
    the history. Using the most recent historical point could lead to speed calculations of zero
    in the event the quadrature encoders slip/skip and read the same wheel count two times in a
    row even if the wheel is moving with a non-zero speed.
     */
    int indexHistoryOldest = 0; // Keeps track of oldest history index.
    double dt = 0;

    private boolean loggerOn;

    //----------------------------------------------------------------------------------------------

    /**
     * Constructor that sets up Android Sensor Service and creates Sensor objects for both
     * accelerometer and gyroscope. Then registers both sensors such that their onSensorChanged
     * events will call the onSensorChanged method within this class.
     * @param context Context object passed up from Android Main Activity
     */
    public AbcvlibSensors(Context context, boolean loggerOn){
        this.loggerOn = loggerOn;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotation_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        register();
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

        if(sensor.getType()==Sensor.TYPE_GYROSCOPE){

            indexCurrentGyro = sensorChangeCountGyro % historyLength;
            indexPreviousGyro = (sensorChangeCountGyro - 1) % historyLength;
            // Rotation around x-axis
            // See https://developer.android.com/reference/android/hardware/SensorEvent.html
            thetaDotGyro = event.values[0];
            thetaDotGyroDeg = (thetaDotGyro * (180 / Math.PI));
            timeStampsGyro[indexCurrentGyro] = event.timestamp;
            dtGyro = (timeStampsGyro[indexCurrentGyro] - timeStampsGyro[indexPreviousGyro]) / 1000000000f;
            sensorChangeCountGyro++;
            if (loggerOn){
                sendToLog();
            }

        }
        else if(sensor.getType()==Sensor.TYPE_ROTATION_VECTOR){

            indexCurrentRotation = sensorChangeCountRotation % historyLength;
            indexPreviousRotation = (sensorChangeCountRotation - 1) % historyLength;
            indexHistoryOldest = (sensorChangeCountRotation + 1) % historyLength;
            timeStamps[indexCurrentRotation] = event.timestamp;
            dt = (timeStamps[indexCurrentRotation] - timeStamps[indexPreviousRotation]) / 1000000000f;

            SensorManager.getRotationMatrixFromVector(rotationMatrix , event.values);
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrixRemap);
            SensorManager.getOrientation(rotationMatrixRemap, orientation);
            thetaRad = orientation[1]; //Pitch
//            thetaRad = lowpassFilter(thetaRad, dt_sample, lp_freq_theta);

//            angularVelocityRad[indexCurrentRotation] = (thetaRad[indexCurrentRotation] - thetaRad[indexPreviousRotation]) / 0.005;
//            angularVelocityRad = lowpassFilter(angularVelocityRad, 0.005, lp_freq_thetaDot);
//            if (sensorChangeCountRotation > historyLength){
//                angularVelocityRad[indexCurrentRotation] = runningAvg(angularVelocityRad, 3);
//            }

            thetaDeg = (thetaRad * (180 / Math.PI));
//            angularVelocityDeg = (angularVelocityRad[indexCurrentRotation] * (180 / Math.PI));

            // Update all previous variables with current ones
            sensorChangeCountRotation++;
            if (loggerOn){
                sendToLog();
            }
        }
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
    }

    /**
     * @return Phone tilt angle in radians
     */
    public double getThetaRad(){ return thetaRad; }

    /**
     * @return Phone tilt angle in degrees
     */
    public double getThetaDeg(){
        return thetaDeg;
    }

    /**
     * @return Phone tilt speed (angular velocity) in radians per second
     */
    public double getThetaRadDot(){ return thetaDotGyro; }

    /**
     * @return Phone tilt speed (angular velocity) in degrees per second
     */
    public double getThetaDegDot(){
        return thetaDotGyroDeg;
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
    public void setHistoryLength(int len) {historyLength = len; }

    /**
     * Send accelerometer and gyroscope data, along with calculated tilt angles, speeds, etc. such
     * that they can be read by the sensor data graphing utility.
     */
    private void sendToLog() {

//        // Compile thetaDegVectorMsg values to push to separate adb tag
//        String thetaVectorMsg = Double.toString(thetaDeg);

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

//        Log.i("thetaVectorMsg", thetaVectorMsg);
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

    private double[] lowpassFilter(double[] x, double dt, double f_c){
        double[] y = new double[x.length];
        double alpha = (2 * Math.PI * dt * f_c) / ((2 * Math.PI * dt * f_c) + 1);
        y[0] = alpha * x[0] + ((1 - alpha) * y[x.length - 1]);
        for (int i = 1; i < y.length; i++){
            y[i] = (alpha * x[i]) + ((1 - alpha) * y[i - 1]);
        }
        return y;
    }

    private double runningAvg(double[] x, int samples){
        double y = 0;
        int idx;

        for (int i = 0; i < samples; i++){
            idx = (sensorChangeCountRotation - i) % historyLength;
            y = y + x[idx];
        }
        return y / samples;
    }

}
