package jp.oist.abcvlib.basic;

import android.util.Log;

public class AbcvlibQuadEncoders {


    private int windowLength = 5;
    private int indexCurrent = windowLength;
    private int indexPrevious = 0;
    private int indexFilterDelay = 0;
    private int indexOldest = 0;
    private int quadCount = windowLength;
    private boolean loggerOn;
    double dt_sample = 0;
    double dt_window = 0;

    //----------------------------------- Wheel speed metrics --------------------------------------
    /**
     * Compounding negative wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private double encoderCountRightWheel = 0;
    /**
     * Compounding positive wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private double encoderCountLeftWheel = 0;
    /**
     * Right Wheel Speed in quadrature encoder counts per second.
     */
    private double speedRightWheel = 0;
    private double speedRightWheelLP = 0;

    /**
     * Left Wheel Speed in quadrature encoder counts per second.
     */
    private double speedLeftWheel = 0;
    private double speedLeftWheelLP = 0;

    /**
     * distance in mm that the right wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double distanceR = 0;
    /**
     * distance in mm that the left wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double distanceL = 0;

    private double distanceLLP = 0;
    private double distanceLLPPrevious = 0;
    private double distanceRLP = 0;
    private double distanceRLPPrevious = 0;

    private double[] timeStamps = new double[windowLength];


    // Constructor to pass other module objects in. Default loggerOn value to true
    public AbcvlibQuadEncoders(boolean loggerOn){

        this.loggerOn = loggerOn;

    }

    /**
     * @return Current encoder count for the right wheel
     */
    public double getWheelCountR(){ return encoderCountRightWheel; }

    /**
     * @return Current encoder count for the left wheel
     */
    public double getWheelCountL(){ return encoderCountLeftWheel; }

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
    void setQuadVars(double countLeft, double countRight, int indexCurrentLopper, int indexPreviousLooper, double time) {

        indexCurrent = (quadCount) % windowLength;
        indexPrevious = (quadCount - 1) % windowLength;
        indexOldest = (quadCount + 1) % windowLength;


        encoderCountLeftWheel = countLeft;
        encoderCountRightWheel = countRight;
        timeStamps[indexCurrent] = time;
        dt_sample = (timeStamps[indexCurrent] - timeStamps[indexPrevious]) / 1000000000f;
        dt_window = (timeStamps[indexCurrent] - timeStamps[indexOldest]) / 1000000000f;
        setDistanceL();
        setDistanceR();
        setWheelSpeedL();
        setWheelSpeedR();

        if (loggerOn){
            sendToLog();
        }

        quadCount++;
    }

    /**
     * Get distances traveled by left wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceL in mm
     */
    private void setDistanceL(){

        double mmPerCount = (2 * Math.PI * 30) / 128;

        distanceLLPPrevious = distanceLLP;

        distanceL = encoderCountLeftWheel * mmPerCount;

//        distanceLLP[indexCurrent] = runningAvg(distanceL, buffer);
        distanceLLP = exponentialAvg(distanceL, distanceLLP, dt_sample, dt_window);

    }

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceR in mm
     */
    private void setDistanceR(){

        double mmPerCount = (2 * Math.PI * 30) / 128;

        distanceRLPPrevious = distanceRLP;

        distanceR = encoderCountRightWheel * mmPerCount;

//        distanceRLP[indexCurrent] = runningAvg(distanceR, buffer);
//        distanceRLP[indexCurrent] = lowpassFilter(distanceR);
        distanceRLP = exponentialAvg(distanceR, distanceRLP, dt_sample, dt_window);



    }

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    private void setWheelSpeedL() {

        if (dt_sample != 0) {
            // Calculate the speed of each wheel in mm/s.
            speedLeftWheel = (distanceLLP - distanceLLPPrevious) / dt_sample;
            speedLeftWheelLP = exponentialAvg(speedLeftWheel, speedLeftWheelLP, dt_sample, dt_window);
        }
        else{
            Log.i("sensorDebugging", "dt_sample == 0");
        }

    }

    /**
     * @return Current speed of right wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    private void setWheelSpeedR() {

        if (dt_sample != 0) {
            // Calculate the speed of each wheel in mm/s.
            speedRightWheel = (distanceRLP - distanceRLPPrevious) / dt_sample;
            speedRightWheelLP = exponentialAvg(speedRightWheel, speedRightWheelLP, dt_sample, dt_window);
        }
        else{
            Log.i("sensorDebugging", "dt_sample == 0");
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
        String encoderCountMsg =  Double.toString(encoderCountLeftWheel) + " " +
                Double.toString(encoderCountRightWheel);
//
        // Compile distance values to push to separate adb tag
        String distanceLMsg = Double.toString(distanceL) + " " +  Double.toString(distanceLLP) + " " + Double.toString(distanceR) + " " +  Double.toString(distanceRLP);

        // Compile thetaDegVectorMsg values to push to separate adb tag
        String speedMsg = Double.toString(speedLeftWheelLP) + " " + Double.toString(speedRightWheelLP);
//
//        // Compile avg and dt_sample values to push to separate adb tag
//        String dtMsg = Double.toString(dt_sample);

        Log.i("encoderCountMsg", encoderCountMsg);
        Log.i("distanceLMsg", distanceLMsg);
        Log.i("speedMsg", speedMsg);
//        Log.i("dtMsg", dtMsg);

    }

    private double abcSum(double[] x){
        double y = 0;
        for (int i = 0; i < x.length; i++){
            y = y + x[i];
        }
        return y;
    }

    private double runningAvg(double[] x, double len){
        double y = 0;
        for (int i = 0; i < x.length; i++){
            y = y + x[i];
        }
        y = y / len;
        return y;
    }

    private double exponentialAvg(double sample, double expAvg, double dt_sample, double dt_window){

        double weighting = Math.exp(-1.0 * dt_sample / dt_window);
        expAvg = (1.0 - weighting) * sample + (weighting * expAvg);

        return expAvg;
    }

}
