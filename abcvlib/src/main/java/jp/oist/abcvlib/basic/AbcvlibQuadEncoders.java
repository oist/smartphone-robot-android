package jp.oist.abcvlib.basic;

import android.util.Log;

public class AbcvlibQuadEncoders {

    int buffer = 10000;
    double dt = 0;
    double dt_lp = 0;
    double lp_freq = 0.000001;
    int speed_sample_length = 50;

    //----------------------------------- Wheel speed metrics --------------------------------------
    /**
     * Compounding negative wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private double[] encoderCountRightWheel = new double[buffer];
    /**
     * Compounding positive wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private double[] encoderCountLeftWheel = new double[buffer];
    /**
     * Right Wheel Speed in quadrature encoder counts per second.
     */
    private double[] speedRightWheel = new double[buffer];
    /**
     * Left Wheel Speed in quadrature encoder counts per second.
     */
    private double[] speedLeftWheel = new double[buffer];
    /**
     * distance in mm that the right wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double[] distanceR = new double[buffer];
    /**
     * distance in mm that the left wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double[] distanceL = new double[buffer];

    private double[] distanceLAvg = new double[buffer];
    private double[] distanceRAvg = new double[buffer];

    private double[] timeStamps = new double[buffer];

    private int indexCurrent = speed_sample_length;
    private int indexSpeed = 0;
    private int indexOldest = 0;
    private int quadCount = speed_sample_length;
    private boolean loggerOn;

    // Constructor to pass other module objects in. Default loggerOn value to true
    public AbcvlibQuadEncoders(boolean loggerOn){

        this.loggerOn = loggerOn;

    }

    /**
     * @return Current encoder count for the right wheel
     */
    public double getWheelCountR(){ return encoderCountRightWheel[indexCurrent]; }

    /**
     * @return Current encoder count for the left wheel
     */
    public double getWheelCountL(){ return encoderCountLeftWheel[indexCurrent]; }

    /**
     * Get distances traveled by left wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceL in mm
     */
    public double getDistanceL(){

        return distanceL[indexCurrent];
    }

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceR in mm
     */
    public double getDistanceR(){

        return distanceR[indexCurrent];

    }

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    public double getWheelSpeedL() {

        return speedLeftWheel[indexCurrent];
    }

    /**
     * @return Current speed of right wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    public double getWheelSpeedR() {

        return speedRightWheel[indexCurrent];
    }

    /**
     * Sets the encoder count for the right wheel. This shouldn't normally be used by the user. This
     * method exists for the AbcvlibLooper class to get/set encoder counts as the AbcvlibLooper
     * class is responsible for constantly reading the encoder values from the IOIOBoard.
     */
    void setQuadVars(double countLeft, double countRight, int indexCurrentLopper, int indexPreviousLooper, double time) {

        indexCurrent = (quadCount - 1) % buffer;
        indexSpeed = (quadCount - speed_sample_length) % buffer;
        indexOldest = (quadCount) % buffer;

        encoderCountLeftWheel[indexCurrent] = countLeft;
        encoderCountRightWheel[indexCurrent] = countRight;
        timeStamps[indexCurrent] = time;
        dt = (timeStamps[indexCurrent] - timeStamps[indexSpeed]) / 1000000000f;
        dt_lp = (timeStamps[indexCurrent] - timeStamps[indexOldest]) / 1000000000f;
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

        distanceL[indexCurrent] = encoderCountLeftWheel[indexCurrent] * mmPerCount;

//        distanceLAvg[indexCurrent] = runningAvg(distanceL, buffer);
        distanceLAvg = lowpassFilter(distanceL, dt_lp, lp_freq);


    }

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceR in mm
     */
    private void setDistanceR(){

        double mmPerCount = (2 * Math.PI * 30) / 128;

        distanceR[indexCurrent] = encoderCountRightWheel[indexCurrent] * mmPerCount;

//        distanceRAvg[indexCurrent] = runningAvg(distanceR, buffer);
        distanceRAvg = lowpassFilter(distanceR, dt_lp, lp_freq);


    }

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    private void setWheelSpeedL() {

        if (dt != 0) {
            // Calculate the speed of each wheel in mm/s.
            speedLeftWheel[indexCurrent] = (distanceLAvg[indexCurrent] - distanceLAvg[indexSpeed]) / dt;
//            speedLeftWheel = lowpassFilter(speedLeftWheel, dt, 1000f);
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
            speedRightWheel[indexCurrent] = (distanceRAvg[indexCurrent] - distanceRAvg[indexSpeed]) / dt;
//            speedRightWheel = lowpassFilter(speedRightWheel, dt, 100f);
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
        String encoderCountMsg =  Double.toString(encoderCountLeftWheel[indexCurrent]) + " " +
                Double.toString(encoderCountRightWheel[indexCurrent]);

        // Compile distance values to push to separate adb tag
        String distanceLMsg = Double.toString(distanceL[indexCurrent]) + " " +  Double.toString(distanceLAvg[indexCurrent]) + " " + Double.toString(distanceR[indexCurrent]) + " " +  Double.toString(distanceRAvg[indexCurrent]);

        // Compile thetaDegVectorMsg values to push to separate adb tag
        String speedMsg = Double.toString(speedLeftWheel[indexCurrent]) + " " + Double.toString(speedRightWheel[indexCurrent]);

        // Compile avg and dt values to push to separate adb tag
        String dtMsg = Double.toString(dt);

        Log.i("encoderCountMsg", encoderCountMsg);
        Log.i("distanceLMsg", distanceLMsg);
        Log.i("speedMsg", speedMsg);
        Log.i("dtMsg", dtMsg);

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
