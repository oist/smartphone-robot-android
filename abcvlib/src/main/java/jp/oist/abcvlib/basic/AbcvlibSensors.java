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
     * indexHistoryCurrent calculates the correct index within the time history arrays in order to
     * continuously loop through and rewrite the encoderCountHistoryLength indexes.
     * E.g. if sensorChangeCount is 15 and encoderCountHistoryLength is 15, then indexHistoryCurrent
     * will resolve to 0 and the values for each history array will be updated from index 0 until
     * sensorChangeCount exceeds 30 at which point it will loop to 0 again, so on and so forth.
     */
    private int indexHistoryCurrent = 0;
    private int indexHistoryCurrentPrevious = 0;
    /**
     * Length of past timestamps and encoder values you keep in memory. 15 is not significant,
     * just what was deemed appropriate previously.
     */
    private int historyLength = 1000;
    /**
     * Total number of times the sensors have changed data
     */
    private int sensorChangeCount = 0;
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------

    // Android Sensor objects
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor rotation_sensor;

    //----------------------------------------------------------------------------------------------
    /**
     * orientation vector See link below for android doc
     * https://developer.android.com/reference/android/hardware/SensorManager.html#getOrientation(float%5B%5D,%2520float%5B%5D)
     */
    private float[] orientation = new float[3];
    /**
     * theta calculated from rotation vector
     */
    private double[] thetaRotationVector = new double[historyLength];
    /**
     * thetaRotationVector converted to degrees.
     */
    private double thetaDegVector = 0;
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
    private double[] angularVelocityRotationVector = new double[historyLength];
    /**
     * angularVelocityRotationVector converted to degrees.
     */
    private float angularVelocityRotationVectorDeg = 0;


    //----------------------------------------------------------------------------------------------

    //----------------------------------------- Timestamps -----------------------------------------
    /**
     * Keeps track of both gyro and accelerometer sensor timestamps
     */
    private long timeStamps[] = new long[historyLength];
    int indexHistoryOldest = 0; // Keeps track of oldest history index.
    private double lp_freq = 10000;

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
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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

        double dt;
        indexHistoryCurrent = sensorChangeCount % historyLength;
        indexHistoryCurrentPrevious = (sensorChangeCount - 1) % historyLength;
        /*
        indexHistoryOldest calculates the index for the oldest encoder count still within
        the history. Using the most recent historical point could lead to speed calculations of zero
        in the event the quadrature encoders slip/skip and read the same wheel count two times in a
        row even if the wheel is moving with a non-zero speed.
         */
        indexHistoryOldest = (sensorChangeCount + 1) % historyLength;
        timeStamps[indexHistoryCurrent] = event.timestamp;
        //Calculate the time difference between the most current timestamp and the oldest one in the history arrays.
        dt = (timeStamps[indexHistoryCurrent] - timeStamps[indexHistoryOldest]) / 1000000000;

        if (indexHistoryCurrentPrevious >= 0){
            if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                SensorManager.getRotationMatrixFromVector(rotationMatrix , event.values);
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrixRemap);
                SensorManager.getOrientation(rotationMatrixRemap, orientation);
                thetaRotationVector[indexHistoryCurrent] = orientation[1]; //Pitch

                /*
                timestamps given in nanoseconds from some seemingly random start time (this varies
                with Android version apparently, so only depend on time differences, not any
                absolute time. Dividing by 1000000000 to get time in seconds.
                */
                thetaRotationVector = lowpassFilter(thetaRotationVector, dt, lp_freq);
                angularVelocityRotationVector[indexHistoryCurrent] = (thetaRotationVector[indexHistoryCurrent] - thetaRotationVector[indexHistoryCurrentPrevious]) / dt;
                angularVelocityRotationVector = lowpassFilter(angularVelocityRotationVector, dt, lp_freq);
            }
        }

        thetaDegVector = (float) ((thetaRotationVector[indexHistoryCurrent] * (180 / Math.PI)));
        angularVelocityRotationVectorDeg = (float) ((angularVelocityRotationVector[indexHistoryCurrent] * (180 / Math.PI)));

        // Update all previous variables with current ones
        sensorChangeCount++;
        if (loggerOn){
            sendToLog();
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
    }

    /**
     * @return Phone tilt angle in radians
     */
    public double getThetaRad(){ return thetaRotationVector[indexHistoryCurrent]; }

    /**
     * @return Phone tilt angle in degrees
     */
    public double getThetaDeg(){
        return thetaDegVector;
    }

    /**
     * @return Phone tilt speed (angular velocity) in radians per second
     */
    public double getThetaRadDot(){ return angularVelocityRotationVector[indexHistoryCurrent]; }

    /**
     * @return Phone tilt speed (angular velocity) in degrees per second
     */
    public double getThetaDegDot(){
        return angularVelocityRotationVectorDeg;
    }

    /**
     * @return Total combined count for how many times the accelerometer and gyroscope have provided
     * data.
     */
    int getSensorChangeCount() {return sensorChangeCount;}

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

        // Compile thetaDegVectorMsg values to push to separate adb tag
        String thetaVectorMsg = Double.toString(thetaDegVector);

        // Compile thetaDegVectorMsg values to push to separate adb tag
        String thetaVectorVelMsg = Float.toString(angularVelocityRotationVectorDeg);

        Log.i("thetaVectorMsg", thetaVectorMsg);
        Log.i("thetaVectorVelMsg", thetaVectorVelMsg);

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
        y[0] = alpha * x[0];
        for (int i = 1; i < y.length; i++){
            y[i] = (alpha * x[i]) + ((1 - alpha) * y[i - 1]);
        }
        return y;
    }

}
