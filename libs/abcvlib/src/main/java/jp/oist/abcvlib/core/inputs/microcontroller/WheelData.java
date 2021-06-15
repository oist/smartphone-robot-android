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
    private EncoderCounter encoderCountRightWheel = new EncoderCounter();
    /**
     * Compounding positive wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private EncoderCounter encoderCountLeftWheel = new EncoderCounter();
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
    public void onWheelDataUpdate(long timestamp, boolean encoderARightWheelState,
                                  boolean encoderBRightWheelState, boolean encoderALeftWheelState,
                                  boolean encoderBLeftWheelState) {
        WheelData wheelData = this;
        handler.post(() -> {

            /*
            Right is negative and left is positive since the wheels are physically mirrored so
            while moving forward one wheel is moving ccw while the other is rotating cw.
            */
            encoderCountRightWheel.updateCount(encoderARightWheelState, encoderBRightWheelState);
            encoderCountLeftWheel.updateCount(encoderALeftWheelState, encoderBLeftWheelState);

            int indexCurrent = (quadCount) % windowLength;
            int indexPrevious = (quadCount - 1) % windowLength;

            timeStamps[indexCurrent] = timestamp;
            dt_sample = (timeStamps[indexCurrent] - timeStamps[indexPrevious]) / 1000000000f;
            setDistanceL();
            setDistanceR();
            setWheelSpeedL();
            setWheelSpeedR();

            if (isRecording){
                timeStepDataBuffer.getWriteData().getWheelData().getLeft().put(timestamp, encoderCountLeftWheel.getCount(), distanceL, speedLeftWheelLP);
                timeStepDataBuffer.getWriteData().getWheelData().getRight().put(timestamp, encoderCountRightWheel.getCount(), distanceR, speedRightWheelLP);
            }
            if (wheelDataListener != null){
                wheelDataListener.onWheelDataUpdate(timestamp, encoderCountLeftWheel.getCount(), encoderCountRightWheel.getCount(),
                distanceL, distanceR, speedLeftWheelLP, speedRightWheelLP);
            }

            quadCount++;
        });
    }

    private static class EncoderCounter {
        boolean encoderAStatePrevious;
        boolean encoderBStatePrevious;
        int count = 0;

        public EncoderCounter(){
        }

        /**
         Input all IO values from Hubee Wheel and output either +1, or -1 to add or subtract one wheel
         count.<br><br>

         The combined values of input1WheelStateIo and input2WheelStateIo control the direction of the
         Hubee wheels.<br><br>

         encoderAState and encoderBState are the direct current IO reading (high or low) of
         the quadrature encoders on the Hubee wheels. See Hubee wheel documentation regarding which IO
         corresponds to the A and B IO.<br><br>

         <img src="../../../../../../../../../../media/images/hubeeWheel.gif" />
         <br><br>

         encoderAWheelStatePrevious and encoderBWheelStatePrevious are previous state of their
         corresponding variables.<br><br>

         IN1  IN2 PWM Standby Result<br>
         H    H   H/L H   Stop-Brake<br>
         L    H   H   H   Turn Forwards<br>
         L    H   L   H   Stop-Brake<br>
         H    L   H   H   Turn Backwards<br>
         H    L   L   H   Stop-Brake<br>
         L    L   H/L H   Stop-NoBrake<br>
         H/L  H/L H/L L   Standby<br><br>

         See: <a href="http://www.creative-robotics.com/quadrature-intro">http://www.creative-robotics.com/quadrature-intro</a>

         * @return wheelCounts
         */
        private void updateCount(Boolean encoderAState, Boolean encoderBState){
            // Channel A goes from Low to High
            if (!encoderAStatePrevious && encoderAState){
                // Channel B is Low = Clockwise
                if (!encoderBState){
                    count++;
                }
                // Channel B is High = CounterClockwise
                else {
                    count--;
                }
            }

            // Channel A goes from High to Low
            else if (encoderAStatePrevious && !encoderAState){
                // Channel B is Low = CounterClockwise
                if (!encoderBState){
                    count--;
                }
                // Channel B is High = Clockwise
                else {
                    count++;
                }
            }

            // Channel B goes from Low to High
            else if (!encoderBStatePrevious && encoderBState){
                // Channel A is Low = CounterClockwise
                if (!encoderAState){
                    count--;
                }
                // Channel A is High = Clockwise
                else {
                    count++;
                }
            }

            // Channel B goes from High to Low
            else if (encoderBStatePrevious && !encoderBState){
                // Channel A is Low = Clockwise
                if (!encoderAState){
                    count++;
                }
                // Channel A is High = CounterClockwise
                else {
                    count--;
                }
            }

            // Else both the current and previous state of A is HIGH or LOW, meaning no transition has
            // occurred thus no need to add or subtract from wheelCounts

            encoderAStatePrevious = encoderAState;
            encoderBStatePrevious = encoderBState;
        }

        public int getCount() {
            return count;
        }
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
    public int getWheelCountR(){ return encoderCountRightWheel.getCount(); }

    /**
     * @return Current encoder count for the left wheel
     */
    public int getWheelCountL(){ return encoderCountLeftWheel.getCount(); }

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
        distanceL = encoderCountLeftWheel.getCount() * mmPerCount;

    }

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     */
    private void setDistanceR(){
        double mmPerCount = (2 * Math.PI * 30) / 128;
        distanceRPrevious = distanceR;
        distanceR = encoderCountRightWheel.getCount() * mmPerCount;
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
