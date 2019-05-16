package jp.oist.abcvlib.basic;

import android.util.Log;

public class AbcvlibQuadEncoders {

    int buffer = 15;
    int indexCurrent = 1;
    int indexPrevious = 0;
    long dt = 0;

    //----------------------------------- Wheel speed metrics --------------------------------------
    /**
     * Compounding negative wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private int encoderCountRightWheel[] = new int[buffer];
    /**
     * Compounding positive wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private int encoderCountLeftWheel[] = new int[buffer];
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
    /**
     * distance in mm that the left wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double distanceL;

    /**
     * @return Current encoder count for the right wheel
     */
    public int getWheelCountR(){ return encoderCountRightWheel[indexCurrent]; }

    /**
     * @return Current encoder count for the left wheel
     */
    public int getWheelCountL(){ return encoderCountLeftWheel[indexCurrent]; }

    /**
     * Get distances traveled by left wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceL in mm
     */
    public double getDistanceL(){

        double mmPerCount = (2 * Math.PI * 30) / 128;

        distanceL = encoderCountLeftWheel[indexCurrent] * mmPerCount;

        return distanceL;
    }

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceR in mm
     */
    public double getDistanceR(){

        double mmPerCount = (2 * Math.PI * 30) / 128;

        distanceR = encoderCountRightWheel[indexCurrent] * mmPerCount;

        return distanceR;
    }

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    public double getWheelSpeedL() {

        if (dt != 0) {
            distanceL = getDistanceL();
            // Calculate the speed of each wheel in mm/s.
            speedLeftWheel = (calcDistance(encoderCountLeftWheel[indexCurrent]) - calcDistance(encoderCountLeftWheel[indexPrevious])) / dt;
        }
        else{
            Log.i("sensorDebugging", "dt == 0");
        }

        return speedLeftWheel;
    }

    /**
     * @return Current speed of right wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    public double getWheelSpeedR() {

        if (dt != 0) {
            distanceR = getDistanceR();
            // Calculate the speed of each wheel in mm/s.
            speedRightWheel = (calcDistance(encoderCountRightWheel[indexCurrent]) - calcDistance(encoderCountRightWheel[indexPrevious])) / dt;
        }
        else{
            Log.i("sensorDebugging", "dt == 0");
        }

        return speedRightWheel;
    }

    /**
     * Sets the encoder count for the right wheel. This shouldn't normally be used by the user. This
     * method exists for the AbcvlibLooper class to get/set encoder counts as the AbcvlibLooper
     * class is responsible for constantly reading the encoder values from the IOIOBoard.
     * @param count encoder count (number of times the quadrature encoder has passed between
     *              H and L)
     */
    void setWheelR(int count) { encoderCountRightWheel[indexCurrent] = count; }

    /**
     * Sets the encoder count for the left wheel. This shouldn't normally be used by the user. This
     * method exists for the AbcvlibLooper class to get/set encoder counts as the AbcvlibLooper
     * class is responsible for constantly reading the encoder values from the IOIOBoard.
     * @param count encoder count (number of times the quadrature encoder has passed between
     *              H and L)
     */
    void setWheelL(int count) { encoderCountLeftWheel[indexCurrent] = count; }

    void setDt(long time){
        dt = time;
    }

    private double calcDistance(int count){

        double distance;
        double mmPerCount = (2 * Math.PI * 30) / 128;

        distance = count * mmPerCount;

        return distance;
    }



}
