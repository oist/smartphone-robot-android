package jp.oist.abcvlib.basic;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * AbcvlibSensors reads and processes the data from the Android phone gryoscope and
 * accelerometer. The two main goals of this class are:
 *
 * 1.) To estimate the tilt angle of the phone (thetaRad) by combining the input from the
 * gyroscope and accelerometer
 * 2.) To calculate the speed of each wheel (speedRightWheel and speedLeftWheel) by using
 * existing and past quadrature encoder states
 *
 * @author Jiexin Wang https://github.com/ha5ha6
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class AbcvlibSensors implements SensorEventListener {

    //------------------------------------ Tilt angle metrics --------------------------------------
    /**
     * Phone tilt angle in radians calculated via complementary filter combining both accelerometer and gyro inputs.
     */
    private float thetaRad = 0;
    /**
     * thetaRad converted to degrees.
     */
    private float thetaDeg;
    /**
     * thetaRadDotGyro converted to degrees per second.
     */
    private float thetaDegDot;
    /**
     * thetaRad from previous calculation (n-1).
     */
    private float thetaRadPrevious = 0;
    /**
     * Raw tilt velocity calculated by gyroscope alone.
     */
    private float thetaRadDotGyro;
    /**
     * Raw tilt angle of phone in radians calculated via accelerometer data alone.
     */
    private float thetaAccelerometer;
    //----------------------------------------------------------------------------------------------

    //----------------------------------------- Counters -------------------------------------------
    /**
     * Keeps track of current history index.
     */
    private int indexHistoryCurrent = 0;
    /**
     * Length of past timestamps and encoder values you keep in memory. 15 is not significant,
     * just what was deemed appropriate previously.
     */
    private int historyLength = 15;
    /**
     * Total number of times the sensors have changed data
     */
    private int sensorChangeCount = 0;
    //----------------------------------------------------------------------------------------------

    //----------------------------------- Wheel speed metrics --------------------------------------
    /**
     * Compounding negative wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private int encoderCountRightWheel[] = new int[historyLength];
    /**
     * Compounding positive wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private int encoderCountLeftWheel[] = new int[historyLength];
    /**
     * Right Wheel Speed in quadrature encoder counts per second.
     */
    private float speedRightWheel;
    /**
     * Left Wheel Speed in quadrature encoder counts per second.
     */
    private float speedLeftWheel;
    //----------------------------------------------------------------------------------------------

    // Android Sensor objects
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    //----------------------------------------------------------------------------------------------

    // Variables to calculate linear acceleration from combined gravity + linear acceleration data.
    /**
     * Calculated gravity portion of accelerometer data in x,y,z axes
     */
    private float[] gravity = new float[3];
    /**
     * Calculated acceleration without gravity of accelerometer data in x,y,z axes
     */
    private float[] linearAcceleration = new float[3];
    /**
     * Raw accelerometer signal along x-axis
     */
    private float accelerationX = 0;
    /**
     * Raw accelerometer signal along y-axis
     */
    private float accelerationY = 0;
    /**
     * Raw accelerometer signal along z-axis
     */
    private float accelerationZ = 0;
    //----------------------------------------------------------------------------------------------

    //----------------------------------------- Timestamps -----------------------------------------
    /**
     * Most recent timestamp provided by gyro
     */
    private long gyroTime = 0;
    /**
     * Keeps track of both gyro and accelerometer sensor timestamps
     */
    private long timeStamps[] = new long[historyLength];
    //----------------------------------------------------------------------------------------------

    /**
     * Constructor that sets up Android Sensor Service and creates Sensor objects for both
     * accelerometer and gyroscope. Then registers both sensors such that their onSensorChanged
     * events will call the onSensorChanged method within this class.
     * @param context Context object passed up from Android Main Activity
     */
    public AbcvlibSensors(Context context){
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
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

        long timeDeltaOldestNewest; // Difference in timestamps between most current and oldest in history.
        int indexHistoryOldest; // Keeps track of oldest history index.
        long accelerometerTime; // Most recent timestamp provided by accelerometer
        long gyroTimeDelta; // Elapsed time between current and last gyro readings

        timeStamps[indexHistoryCurrent] = event.timestamp;

        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            if (gyroTime != 0) {

                /*
                timestamps given in nanoseconds from some seemingly random start time (this varies
                with Android version apparently, so only depend on time differences, not any
                absolute time. Dividing by 1000000000 to get time in seconds.
                */
                gyroTimeDelta = (event.timestamp - gyroTime) / 1000000000;
                /*
                event.values[0] for the gyroscope is the angular acceleration around the x axis
                (Left to Right axis if looking at portrait phone)
                 */
                thetaRadDotGyro = -event.values[0];
                // Calculate tilt angle based on previous tilt angle plus angular veclocity * timestep
                thetaRad += thetaRadDotGyro * gyroTimeDelta;

            }

            gyroTime = event.timestamp;
            Log.i("SensorTimestamps", "Gyroscope = " + String.valueOf(gyroTime) );

        }

        /*
        SensorEvent.values[] for TYPE_ACCELEROMETER
        All values are in SI units (m/s^2)
        values[0]: Acceleration minus Gx on the x-axis (Left to Right axis if looking at portrait phone)
        values[1]: Acceleration minus Gy on the y-axis (Bottom to Top axis if looking at portrait phone)
        values[2]: Acceleration minus Gz on the z-axis (Back to Front axis if looking at portrait phone)
         */
        else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            // alpha is calculated as t / (t + dT)
            // with t, the low-pass filter's time-constant
            // and dT, the event delivery rate

            final float alpha = 0.8f;

            accelerationX = event.values[0];
            accelerationY = event.values[1];
            accelerationZ = event.values[2];

            gravity[0] = alpha * gravity[0] + (1 - alpha) * accelerationX;
            gravity[1] = alpha * gravity[1] + (1 - alpha) * accelerationX;
            gravity[2] = alpha * gravity[2] + (1 - alpha) * accelerationZ;

            linearAcceleration[0] = event.values[0] - gravity[0];
            linearAcceleration[1] = event.values[1] - gravity[1];
            linearAcceleration[2] = event.values[2] - gravity[2];

            // Calculate tilt angle based on accelerometer readings.
            thetaAccelerometer = (float)(Math.atan2(accelerationZ, accelerationY));

            /*
            This logic was to remove the effects of linear acceleration or bumping of the phone which
            caused the accelerometer data to be unreliable. Paavo recommended comparing the variances of
            the gyro and accelerometer instead at some point. Reducing the weighting on the accelerometer
            as the variance increases.
            */
            // TODO replace this logic with a comparison of variance on the acc and gyro.
            if ((thetaRad - thetaRadPrevious) * (thetaRad - thetaRadPrevious) < 0.0025) {

                // TODO Are these the best weights for the Complementary filter?
                thetaRad = 0.99f * thetaRad + 0.01f * thetaAccelerometer;

            }

            accelerometerTime = event.timestamp;
            Log.i("SensorTimestamps", "Accelerometer = " + String.valueOf(accelerometerTime) );
        }

        thetaRadPrevious = thetaRad;
        thetaRad = wrapAngle(thetaRad);

        // Convert radians to degrees
        thetaDeg = (float) ((thetaRad * (180 / Math.PI)));
        thetaDegDot = (float) (thetaRadDotGyro * (180 / Math.PI));

        /*
        indexHistoryCurrent calculates the correct index within the time history arrays in order to
        continuously loop through and rewrite the encoderCountHistoryLength indexes.
        E.g. if sensorChangeCount is 15 and encoderCountHistoryLength is 15, then indexHistoryCurrent
        will resolve to 0 and the values for each history array will be updated from index 0 until
        sensorChangeCount exceeds 30 at which point it will loop to 0 again, so on and so forth.
         */
        indexHistoryCurrent = sensorChangeCount % historyLength;
        /*
        indexHistoryOldest calculates the index for the oldest encoder count still within
        the history. Using the most recent historical point could lead to speed calculations of zero
        in the event the quadrature encoders slip/skip and read the same wheel count two times in a
        row even if the wheel is moving with a non-zero speed.
         */
        indexHistoryOldest = (sensorChangeCount + 1) % historyLength;

        // Calculate the time difference between the most current timestamp and the oldest one in the history arrays.
        timeDeltaOldestNewest = timeStamps[indexHistoryCurrent] - timeStamps[indexHistoryOldest];

        /*
         The if statement waits for the full history length to fill. Otherwise you would be dividing
         by zero since timeDeltaOldestNewest is zero until the array fills with timeStamps from the
         sensors.
          */
        if (timeDeltaOldestNewest != 0) {
            // Calculate the speed of each wheel in encoder counts per second. TODO convert this to something more meaningful like rotations per second.
            speedRightWheel = (float) ((encoderCountRightWheel[indexHistoryCurrent] - encoderCountRightWheel[indexHistoryOldest]) / timeDeltaOldestNewest);
            speedLeftWheel = (float) ((encoderCountLeftWheel[indexHistoryCurrent] - encoderCountLeftWheel[indexHistoryOldest]) / timeDeltaOldestNewest);
        }
        else{
            Log.i("sensorDebugging", "timeDeltaOldestNewest == 0");
        }

        sensorChangeCount++;
        sendToLog(accelerationX, accelerationY, accelerationZ);
    }

    /**
     Registering sensorEventListeners for accelerometer and gyroscope only.
     */
    public void register(){
        // Check if accelerometer exists before trying to turn on the listener
        if (accelerometer != null){
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Log.e("SensorTesting", "No Default Accelerometer Available.");
        }
        // Check if gyroscope exists before trying to turn on the listener
        if (gyroscope != null){
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Log.e("SensorTesting", "No Default Gyroscope Available.");
        }
    }

    /**
     * Check if accelerometer and gyroscope objects still exist before trying to unregister them.
     * This prevents null pointer exceptions.
     */
    public void unregister(){
        // Check if accelerometer exists before trying to turn off the listener
        if (accelerometer != null){
            sensorManager.unregisterListener(this, accelerometer);
        }
        // Check if gyroscope exists before trying to turn off the listener
        if (gyroscope != null){
            sensorManager.unregisterListener(this, gyroscope);
        }
    }

    /**
     * @return Phone tilt angle in radians
     */
    public float getThetaRad(){ return thetaRad; }

    /**
     * @return Phone tilt angle in degrees
     */
    public float getThetaDeg(){
        return thetaDeg;
    }

    /**
     * @return Phone tilt speed (angular velocity) in radians per second
     */
    float getThetaRadDot(){ return thetaRadDotGyro; }

    /**
     * @return Phone tilt speed (angular velocity) in degrees per second
     */
    float getThetaDegDot(){ return thetaDegDot;}

    /**
     * @return Current encoder count for the right wheel
     */
    int getWheelCountR(){ return encoderCountRightWheel[indexHistoryCurrent]; }

    /**
     * @return Current encoder count for the left wheel
     */
    int getWheelCountL(){ return encoderCountLeftWheel[indexHistoryCurrent]; }

    /**
     * @return Current speed of right wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    float getWheelDotR() { return speedRightWheel; }

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    float getWheelDotL() { return speedLeftWheel; }

    /**
     * @return Total combined count for how many times the accelerometer and gyroscope have provided
     * data.
     */
    int getSensorChangeCount() {return sensorChangeCount;}

    // TODO for all set methods, handle likely errors or miscalculations. (May need to add recalculations of some variables outside of the direct one set)
    /**
     * Sets the tilt angle of the phone in radians. Not sure why this would ever be necessary as it
     * would be overwritten at the very next sensor event.
     * @param thetaRad phone tilt angle in radians. Zero being the phone placed vertically,
     *                 perpendicular to the ground, in portrait mode.
     */
    void setThetaRad(float thetaRad) { this.thetaRad = thetaRad; }

    /**
     * Sets the tilt angle of the phone in degrees. Not sure why this would ever be necessary as it
     * would be overwritten at the very next sensor event.
     * @param thetaRadDot phone tilt angle in degrees. Zero being the phone placed vertically,
     *                 perpendicular to the ground, in portrait mode.
     */
    void setThetaRadDot(float thetaRadDot) { this.thetaRadDotGyro = thetaRadDot; }

    /**
     * Sets the encoder count for the right wheel. This shouldn't normally be used by the user. This
     * method exists for the AbcvlibLooper class to get/set encoder counts as the AbcvlibLooper
     * class is responsible for constantly reading the encoder values from the IOIOBoard.
     * @param count encoder count (number of times the quadrature encoder has passed between
     *              H and L)
     */
    void setWheelR(int count) { encoderCountRightWheel[indexHistoryCurrent] = count; }

    /**
     * Sets the encoder count for the left wheel. This shouldn't normally be used by the user. This
     * method exists for the AbcvlibLooper class to get/set encoder counts as the AbcvlibLooper
     * class is responsible for constantly reading the encoder values from the IOIOBoard.
     * @param count encoder count (number of times the quadrature encoder has passed between
     *              H and L)
     */
    void setWheelL(int count) { encoderCountLeftWheel[indexHistoryCurrent] = count; }

    /**
     * Sets the wheel speed for the right wheel. This shouldn't normally be used by the user. Not
     * sure why this would ever be necessary as it would be overwritten at the very next sensor
     * event. See {@link #onSensorChanged(SensorEvent) onSensorChanged} method.
     * @param speed encoder speed in encoder counts per second.
     */
    void setWheeldotR(float speed) { speedRightWheel = speed; }

    /**
     * Sets the wheel speed for the left wheel. This shouldn't normally be used by the user. Not
     * sure why this would ever be necessary as it would be overwritten at the very next sensor
     * event. See {@link #onSensorChanged(SensorEvent) onSensorChanged} method.
     * @param speed encoder speed in encoder counts per second.
     */
    void setWheeldotL(float speed) { speedLeftWheel = speed; }

    /**
     * Send accelerometer and gyroscope data, along with calculated tilt angles, speeds, etc. such
     * that they can be read by the sensor data graphing utility.
     * @param accelerationX Raw acceleration data from accelerometer along x-axis
     * @param accelerationY Raw acceleration data from accelerometer along y-axis
     * @param accelerationZ Raw acceleration data from accelerometer along z-axis
     */
    private void sendToLog(float accelerationX, float accelerationY, float accelerationZ) {

        // Compile raw acceleration data to push to adb log
        String rawAccelerationMsg = Float.toString(accelerationX) + " " +
                Float.toString(accelerationY) + " " +
                Float.toString(accelerationZ);

        // Compile gravity values to push to separate adb tag
        String gravitiesMsg = Float.toString(gravity[0]) + " " +
                Float.toString(gravity[1]) + " " +
                Float.toString(gravity[2]);

        // Compile linear acceleration values to push to separate adb tag
        String linearAccelerationMsg = Float.toString(linearAcceleration[0]) + " " +
                Float.toString(linearAcceleration[1]) + " " +
                Float.toString(linearAcceleration[2]);

        // Compile thetaRad values to push to separate adb tag
        String anglesMsg = Float.toString(thetaAccelerometer) + " " +
                Float.toString(thetaRad);

        // Compile linear acceleration values to push to separate adb tag
        String angleDotMsg = Float.toString(thetaRadDotGyro);

        // Compile thetaDegMsg values to push to separate adb tag
        String thetaDegMsg = Float.toString(thetaDeg);

        Log.i("rawAccelerationMsg", rawAccelerationMsg);
        Log.i("gravitiesMsg", gravitiesMsg);
        Log.i("linearAccelerationMsg", linearAccelerationMsg);
        Log.i("anglesMsg", anglesMsg);
        Log.i("angleDotMsg", angleDotMsg);
        Log.i("thetaDegMsg", thetaDegMsg);
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
