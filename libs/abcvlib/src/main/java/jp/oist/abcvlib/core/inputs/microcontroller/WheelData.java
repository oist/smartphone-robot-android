package jp.oist.abcvlib.core.inputs.microcontroller;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;

public class WheelData implements AbcvlibInput {

    private TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;

    private final int windowLength = 5;
    private int quadCount = windowLength;
    private double dt_sample = 0;
    private double expWeight = 0.1;

    //----------------------------------- Wheel speed metrics --------------------------------------
    /**
     * Compounding negative wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private int encoderCountRightWheel = 0;
    /**
     * Compounding positive wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private int encoderCountLeftWheel = 0;
    /**
     * distance in mm that the right wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double distanceR = 0;
    private double distanceRPrevious = 0;
    /**
     * distance in mm that the left wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double distanceL = 0;
    private double distanceLPrevious = 0;

    private double speedRightWheelLP = 0;
    private double speedLeftWheelLP = 0;

    private final long[] timeStamps = new long[windowLength];
    private WheelDataListener wheelDataListener = null;
    private final Handler handler;

    public WheelData(TimeStepDataBuffer timeStepDataBuffer){
        this.timeStepDataBuffer = timeStepDataBuffer;
        HandlerThread mHandlerThread = new HandlerThread("wheelDataThread");
        mHandlerThread.start();
        handler = new Handler(mHandlerThread.getLooper());
    }

    /**
     * Sets the encoder count for the right wheel. This shouldn't normally be used by the user. This
     * method exists for the AbcvlibLooper class to get/set encoder counts as the AbcvlibLooper
     * class is responsible for constantly reading the encoder values from the IOIOBoard.
     */
    public void onWheelDataUpdate(long timestamp, int countLeft, int countRight) {
        WheelData wheelData = this;
        handler.post(() -> {
            int indexCurrent = (quadCount) % windowLength;
            int indexPrevious = (quadCount - 1) % windowLength;

            encoderCountLeftWheel = countLeft;
            encoderCountRightWheel = countRight;
            timeStamps[indexCurrent] = timestamp;
            dt_sample = (timeStamps[indexCurrent] - timeStamps[indexPrevious]) / 1000000000f;
            setDistanceL();
            setDistanceR();
            setWheelSpeedL();
            setWheelSpeedR();

            if (isRecording){
                timeStepDataBuffer.getWriteData().getWheelCounts().put(timestamp, countLeft, countRight);
            }
            if (wheelDataListener != null){
                wheelDataListener.onWheelDataUpdate(timestamp, countLeft, countRight,
                distanceL, distanceR, speedLeftWheelLP, speedRightWheelLP);
            }

            quadCount++;
        });
    }

    public void setWheelDataListener(WheelDataListener wheelDataListener) {
        this.wheelDataListener = wheelDataListener;
    }

    public void setTimeStepDataBuffer(TimeStepDataBuffer timeStepDataBuffer) {
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    /**
     * @return Current encoder count for the right wheel
     */
    public int getWheelCountR(){ return encoderCountRightWheel; }

    /**
     * @return Current encoder count for the left wheel
     */
    public int getWheelCountL(){ return encoderCountLeftWheel; }

    public double getDistanceL() {
        return distanceL;
    }

    public double getDistanceR() {
        return distanceR;
    }

    /**
     * @return Current speed of left wheel in encoder counts per second with a Low Pass filter.
     * May want to convert to rotations per second if the encoder resolution (counts per revolution)
     * is known.
     */
    public double getWheelSpeedL_LP() {return speedLeftWheelLP;}

    /**
     * @return Current speed of left wheel in encoder counts per second with a Low Pass filter.
     * May want to convert to rotations per second if the encoder resolution (counts per revolution)
     * is known.
     */
    public double getWheelSpeedR_LP() { return speedRightWheelLP;}

    /**
     * Get distances traveled by left wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     */
    private void setDistanceL(){
        double mmPerCount = (2 * Math.PI * 30) / 128;
        distanceLPrevious = distanceL;
        distanceL = encoderCountLeftWheel * mmPerCount;

    }

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     */
    private void setDistanceR(){
        double mmPerCount = (2 * Math.PI * 30) / 128;
        distanceRPrevious = distanceR;
        distanceR = encoderCountRightWheel * mmPerCount;
    }

    private void setWheelSpeedL() {
        if (dt_sample != 0) {
            // Calculate the speed of each wheel in mm/s.
            double speedLeftWheel = (distanceL - distanceLPrevious) / dt_sample;
            speedLeftWheelLP = exponentialAvg(speedLeftWheel, speedLeftWheelLP, expWeight);
        }
        else{
            Log.i("sensorDebugging", "dt_sample == 0");
        }
    }

    private void setWheelSpeedR() {
        if (dt_sample != 0) {
            // Calculate the speed of each wheel in mm/s.
            double speedRightWheel = (distanceR - distanceRPrevious) / dt_sample;
            speedRightWheelLP = exponentialAvg(speedRightWheel, speedRightWheelLP, expWeight);
        }
        else{
            Log.i("sensorDebugging", "dt_sample == 0");
        }
    }

    public void setExpWeight(double weight){
        expWeight = weight;
    }

    public static double exponentialAvg(double sample, double expAvg, double weighting){
        expAvg = (1.0 - weighting) * expAvg + (weighting * sample);
        return expAvg;
    }

    /**
     * Convert quadrature encoder counts to distance traveled by wheel from start point.
     * This does not account for slippage/lifting/etc. so use with a grain of salt
     * @return distance in mm
     */
    public static double counts2Distance(int count){
        double distance;
        double mmPerCount = (2 * Math.PI * 30) / 128;
        distance = count * mmPerCount;
        return distance;
    }
}
