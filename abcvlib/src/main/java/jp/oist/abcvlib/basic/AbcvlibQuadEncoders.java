package jp.oist.abcvlib.basic;

import android.util.Log;

public class AbcvlibQuadEncoders {

    int buffer = 15;
    double dt = 0;

    //----------------------------------- Wheel speed metrics --------------------------------------
    /**
     * Compounding negative wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private int encoderCountRightWheel;
    /**
     * Compounding positive wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private int encoderCountLeftWheel;
    /**
     * Right Wheel Speed in quadrature encoder counts per second.
     */
    private double speedRightWheel;
    /**
     * Left Wheel Speed in quadrature encoder counts per second.
     */
    private double speedLeftWheel;
    /**
     * distance in mm that the right wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double distanceR;
    private double distanceRPrevious;
    /**
     * distance in mm that the left wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double distanceL;
    private double distanceLPrevious;

    /**
     * @return Current encoder count for the right wheel
     */
    public int getWheelCountR(){ return encoderCountRightWheel; }

    /**
     * @return Current encoder count for the left wheel
     */
    public int getWheelCountL(){ return encoderCountLeftWheel; }

    /**
     * Get distances traveled by left wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceL in mm
     */
    public double getDistanceL(){

        return distanceL;
    }

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceR in mm
     */
    public double getDistanceR(){

        return distanceR;

    }

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    public double getWheelSpeedL() {

        return speedLeftWheel;
    }

    /**
     * @return Current speed of right wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    public double getWheelSpeedR() {

        return speedRightWheel;
    }

    /**
     * Sets the encoder count for the right wheel. This shouldn't normally be used by the user. This
     * method exists for the AbcvlibLooper class to get/set encoder counts as the AbcvlibLooper
     * class is responsible for constantly reading the encoder values from the IOIOBoard.
     */
    void setQuadVars(int countLeft, int countRight, int indexCurrentLopper, int indexPreviousLooper, double time) {

        encoderCountLeftWheel = countLeft;
        encoderCountRightWheel = countRight;
        dt = time;
        setDistanceL();
        setDistanceR();
        setWheelSpeedL();
        setWheelSpeedR();
    }

    /**
     * Get distances traveled by left wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceL in mm
     */
    private void setDistanceL(){

        distanceLPrevious = distanceL;

        double mmPerCount = (2 * Math.PI * 30) / 128;

        distanceL = encoderCountLeftWheel* mmPerCount;

    }

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceR in mm
     */
    private void setDistanceR(){

        distanceRPrevious = distanceR;

        double mmPerCount = (2 * Math.PI * 30) / 128;

        distanceR = encoderCountRightWheel * mmPerCount;

    }

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    private void setWheelSpeedL() {

        if (dt != 0) {
            // Calculate the speed of each wheel in mm/s.
            speedLeftWheel = (distanceL - distanceLPrevious) / dt;
//            speedLeftWheel = lowpassFilter(speedLeftWheel, dt, 1000f);
            sendToLog();
        }
        else{
            Log.i("sensorDebugging", "dt == 0");
        }

    }

    /**
     * @return Current speed of right wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    private void setWheelSpeedR() {

        if (dt != 0) {
            // Calculate the speed of each wheel in mm/s.
            speedRightWheel = (distanceR - distanceRPrevious) / dt;
//            speedRightWheel = lowpassFilter(speedRightWheel, dt, 100f);
            sendToLog();
        }
        else{
            Log.i("sensorDebugging", "dt == 0");
        }

    }


    private double calcDistance(int count){

        double distance;
        double mmPerCount = (2 * Math.PI * 30) / 128;

        distance = count * mmPerCount;

        return distance;
    }

    private void sendToLog() {

        // Compile Encoder count data to push to adb log
        String encoderCountMsg =  Float.toString(encoderCountLeftWheel) + " " +
                Float.toString(encoderCountRightWheel);

        // Compile distance values to push to separate adb tag
        String distanceMsg = Double.toString(distanceL) + " " + Double.toString(distanceR);

        // Compile thetaDegVectorMsg values to push to separate adb tag
        String speedMsg = Double.toString(speedLeftWheel) + " " + Double.toString(speedRightWheel);

        Log.i("encoderCountMsg", encoderCountMsg);
        Log.i("distanceMsg", distanceMsg);
        Log.i("speedMsg", speedMsg);

    }



}
