package jp.oist.abcvlib.basic;

import android.util.Log;

public class AbcvlibQuadEncoders {

    int buffer = 50;
    double dt = 0;

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
    private double[] speedRightWheelLP = new double[buffer];

    /**
     * Left Wheel Speed in quadrature encoder counts per second.
     */
    private double[] speedLeftWheel = new double[buffer];
    private double[] speedLeftWheelLP = new double[buffer];

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

    private double[] distanceLLP = new double[buffer];
    private double[] distanceRLP = new double[buffer];

    private double[] timeStamps = new double[buffer];

    private int indexCurrent = 5;
    private int indexPrevious = 0;
    private int indexFilterDelay = 0;
    private int quadCount = 5;
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

        indexCurrent = (quadCount) % buffer;
        indexPrevious = (quadCount - 1) % buffer;
        indexFilterDelay = (quadCount - 5) % buffer;

        encoderCountLeftWheel[indexCurrent] = countLeft;
        encoderCountRightWheel[indexCurrent] = countRight;
        timeStamps[indexCurrent] = time;
        dt = (timeStamps[indexCurrent] - timeStamps[indexPrevious]) / 1000000000f;
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

//        distanceLLP[indexCurrent] = runningAvg(distanceL, buffer);
        distanceLLP[indexCurrent] = lowpassFilter(distanceL);

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

//        distanceRLP[indexCurrent] = runningAvg(distanceR, buffer);
        distanceRLP[indexCurrent] = lowpassFilter(distanceR);


    }

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    private void setWheelSpeedL() {

        if (dt != 0) {
            // Calculate the speed of each wheel in mm/s.
            speedLeftWheel[indexCurrent] = (distanceLLP[indexCurrent] - distanceLLP[indexPrevious]) / dt;
            speedLeftWheelLP[indexCurrent] = lowpassFilterSpeed(speedLeftWheel);
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
            speedRightWheel[indexCurrent] = (distanceRLP[indexCurrent] - distanceRLP[indexPrevious]) / dt;
            speedRightWheelLP[indexCurrent] = lowpassFilterSpeed(speedRightWheel);
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
//
        // Compile distance values to push to separate adb tag
        String distanceLMsg = Double.toString(distanceL[indexFilterDelay]) + " " +  Double.toString(distanceLLP[indexCurrent]) + " " + Double.toString(distanceR[indexFilterDelay]) + " " +  Double.toString(distanceRLP[indexCurrent]);

        // Compile thetaDegVectorMsg values to push to separate adb tag
        String speedMsg = Double.toString(speedLeftWheelLP[indexCurrent]) + " " + Double.toString(speedRightWheelLP[indexCurrent]);
//
//        // Compile avg and dt values to push to separate adb tag
//        String dtMsg = Double.toString(dt);

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

    private double lowpassFilter(double[] x){
        double y = 0;
        int i1;
        int i2;
        int i3;
        int i4;
        int i5;

        for (int i = 0; i < x.length; i++){
            i1 = (quadCount + i) % buffer;
            i2 = (quadCount + i - 1) % buffer;
            i3 = (quadCount + i - 2) % buffer;
            i4 = (quadCount + i - 3) % buffer;
            i5 = (quadCount + i - 4) % buffer;
            y = (0.2 * x[i1] + 0.2 * x[i2] + 0.2 * x[i3] + 0.2 * x[i4] + 0.2 * x[i5]);
            i++;
        }
        return y;
    }

    private double lowpassFilterSpeed(double[] x){
        double y = 0;
        int i1;
        int i2;
        int i3;
        int i4;
        int i5;

        for (int i = 0; i < x.length; i++){
            i1 = (quadCount + i) % buffer;
            i2 = (quadCount + i - 1) % buffer;
            i3 = (quadCount + i - 2) % buffer;
            i4 = (quadCount + i - 3) % buffer;
            i5 = (quadCount + i - 4) % buffer;
            y = (0.2 * x[i1] + 0.2 * x[i2] + 0.2 * x[i3] + 0.2 * x[i4] + 0.2 * x[i5]);
            i++;
        }
        return y;
    }



}
