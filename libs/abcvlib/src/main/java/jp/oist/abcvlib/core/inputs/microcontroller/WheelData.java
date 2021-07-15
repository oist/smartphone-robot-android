package jp.oist.abcvlib.core.inputs.microcontroller;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;

import static jp.oist.abcvlib.util.DSP.exponentialAvg;

public class WheelData implements AbcvlibInput {

    private TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;

    //----------------------------------- Wheel speed metrics --------------------------------------
    private final SingleWheelData rightWheel;
    private final SingleWheelData leftWheel;

    private WheelDataListener wheelDataListener = null;
    private final Handler handler;

    public WheelData(TimeStepDataBuffer timeStepDataBuffer, int bufferLength, double expWeight){
        this.timeStepDataBuffer = timeStepDataBuffer;
        rightWheel = new SingleWheelData(bufferLength, expWeight);
        leftWheel = new SingleWheelData(bufferLength, expWeight);
        HandlerThread mHandlerThread = new HandlerThread("wheelDataThread");
        mHandlerThread.start();
        handler = new Handler(mHandlerThread.getLooper());
    }

    public WheelData(TimeStepDataBuffer timeStepDataBuffer){
        this(timeStepDataBuffer, 50, 0.01);
    }

    public static class Builder{
        TimeStepDataBuffer timeStepDataBuffer;
        int bufferLength = 50;
        double expWeight = 0.01;

        public Builder(){}

        public WheelData build(){
            return new WheelData(timeStepDataBuffer, bufferLength, expWeight);
        }
        public Builder setTimeStepDataBuffer(TimeStepDataBuffer timeStepDataBuffer){
            this.timeStepDataBuffer = timeStepDataBuffer;
            return this;
        }
        public Builder setBufferLength(int bufferLength){
            this.bufferLength = bufferLength;
            return this;
        }
        public Builder setExpWeight(double expWeight){
            this.expWeight = expWeight;
            return this;
        }
    }

    /**
     * Listens for updates on the ioio pins monitoring the quadrature encoders.
     * Note these updates are not interrupts, so they do not necessarily represent changes in
     * value, simply a loop that regularly checks the status of the pin (high/low). This method is
     * called from the publisher located in {@link AbcvlibLooper#loop()}. <br><br>
     *
     * After receiving data this then calculates various metrics like encoderCounts, distance,
     * and speed of each wheel. As the quadrature encoder pin states is updated FAR more frequently
     * than their the frequency in which they change value, the speed calculation will result in
     * values of mostly zero if calculated between single updates. Therefore the calculation of speed
     * provides three different calculations for speed. <br><br>
     * 1.) {@link SingleWheelData#speedInstantaneous} is the speed between single timesteps
     *     (and therefore usually zero in value)<br><br>
     * 2.) {@link SingleWheelData#speedBuffered} is the speed as measured from the beginning to the end
     * of a fixed length buffer. The default length is 50, but this can be set via the
     * {@link WheelData#WheelData(TimeStepDataBuffer, int, double)} constructor or via the
     * {@link WheelData.Builder#setBufferLength(int)} builder method when creating an instance of
     * WheelData.<br><br>
     * 3.) {@link SingleWheelData#speedExponentialAvg} is a running exponential average of
     * {@link SingleWheelData#speedBuffered}. The default weight of the average is 0.01, but this
     * can be set via the {@link WheelData#WheelData(TimeStepDataBuffer, int, double)} constructor or via the
     * {@link WheelData.Builder#setExpWeight(double)} builder method when creating an instance of
     * WheelData.<br><br>
     *
     * Finally, this method then acts as
     * a publisher to any subscribers/listeners that implement the {@link WheelDataListener}
     * interface and passes this quadrature encoder pin state to <br><br>
     * See the jp.oist.abcvlib.basicsubscriber.MainActivity for an example of this subscription
     * framework
     */
    public void onWheelDataUpdate(long timestamp, boolean encoderARightWheelState,
                                  boolean encoderBRightWheelState, boolean encoderALeftWheelState,
                                  boolean encoderBLeftWheelState) {
        handler.post(() -> {
            rightWheel.update(timestamp, encoderARightWheelState, encoderBRightWheelState);
            leftWheel.update(timestamp, encoderALeftWheelState, encoderBLeftWheelState);

            if (isRecording){
                timeStepDataBuffer.getWriteData().getWheelData().getLeft().put(timestamp,
                        leftWheel.getLatestEncoderCount(), leftWheel.getLatestDistance(),
                        leftWheel.getSpeedInstantaneous(), leftWheel.getSpeedBuffered(),
                        leftWheel.getSpeedExponentialAvg());
                timeStepDataBuffer.getWriteData().getWheelData().getRight().put(timestamp,
                        -rightWheel.getLatestEncoderCount(), -rightWheel.getLatestDistance(),
                        -rightWheel.getSpeedInstantaneous(), -rightWheel.getSpeedBuffered(),
                        -rightWheel.getSpeedExponentialAvg());
            }
            if (wheelDataListener != null){
                wheelDataListener.onWheelDataUpdate(timestamp, leftWheel.getLatestEncoderCount(),
                        -rightWheel.getLatestEncoderCount(), leftWheel.getLatestDistance(),
                        -rightWheel.getLatestDistance(), leftWheel.getSpeedInstantaneous(),
                        -rightWheel.getSpeedInstantaneous(), leftWheel.getSpeedBuffered(),
                        -rightWheel.getSpeedBuffered(), leftWheel.getSpeedExponentialAvg(),
                        -rightWheel.getSpeedExponentialAvg());
            }
            rightWheel.updateIndex();
            leftWheel.updateIndex();
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
     * Holds all wheel metrics such as quadrature encoder state, quadrature encoder counts,
     * distance traveled, and current speed. All metrics other than quadrature encoder state are
     * stored in circular buffers in order to avoid rapid shifts in speed measurements due to
     * several identical readings even when moving at full speed. This is due to a very fast sampling
     * rate compared to the rate of change on the quadrature encoders.
     */
    private static class SingleWheelData {
        // High/Low state of pins monitoring quadrature encoders
        private boolean encoderAStatePrevious;
        private boolean encoderBStatePrevious;
        private int bufferLength;
        private int idxHead;
        private int idxHeadPrev;
        private int idxTail = 0;
        // Total number of counts since start of activity
        private final int[] encoderCount;
        // distance in mm that the wheel has traveled from start point. his assumes no slippage/lifting/etc.
        private final double[] distance;
        // speed in mm/s that the wheel is currently traveling at. Calculated by taking the difference between the first and last index in the distance buffer over the difference in timestamps
        private double speedBuffered = 0;
        // speed as measured between two consecutive quadrature code samples (VERY NOISY due to reading zero for any repeated quadrature encoder readings which happen VERY often)
        private double speedInstantaneous = 0;
        // running exponential average of speedBuffered.
        private double speedExponentialAvg = 0;
        private final long[] timestamps;
        private double expWeight;
        double mmPerCount = (2 * Math.PI * 30) / 128;

        public SingleWheelData(int bufferLength, double expWeight){
            this.bufferLength = bufferLength;
            this.expWeight = expWeight;
            idxHead = bufferLength - 1;
            idxHeadPrev = idxHead - 1;
            encoderCount = new int[bufferLength];
            distance = new double[bufferLength];
            timestamps = new long[bufferLength];
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
         */
        private synchronized void update(long timestamp, Boolean encoderAState, Boolean encoderBState){
            updateCount(encoderAState, encoderBState);
            updateDistance();
            updateWheelSpeed(timestamp);
        }

        private synchronized void updateCount(Boolean encoderAState, Boolean encoderBState){

            // Channel A goes from Low to High
            if (!encoderAStatePrevious && encoderAState){
                // Channel B is Low = Clockwise
                if (!encoderBState){
                    encoderCount[idxHead] = encoderCount[idxHeadPrev] + 1;
                }
                // Channel B is High = CounterClockwise
                else {
                    encoderCount[idxHead] = encoderCount[idxHeadPrev] - 1;
                }
            }

            // Channel A goes from High to Low
            else if (encoderAStatePrevious && !encoderAState){
                // Channel B is Low = CounterClockwise
                if (!encoderBState){
                    encoderCount[idxHead] = encoderCount[idxHeadPrev] - 1;
                }
                // Channel B is High = Clockwise
                else {
                    encoderCount[idxHead] = encoderCount[idxHeadPrev] + 1;
                }
            }

            // Channel B goes from Low to High
            else if (!encoderBStatePrevious && encoderBState){
                // Channel A is Low = CounterClockwise
                if (!encoderAState){
                    encoderCount[idxHead] = encoderCount[idxHeadPrev] - 1;
                }
                // Channel A is High = Clockwise
                else {
                    encoderCount[idxHead] = encoderCount[idxHeadPrev] + 1;
                }
            }

            // Channel B goes from High to Low
            else if (encoderBStatePrevious && !encoderBState){
                // Channel A is Low = Clockwise
                if (!encoderAState){
                    encoderCount[idxHead] = encoderCount[idxHeadPrev] + 1;
                }
                // Channel A is High = CounterClockwise
                else {
                    encoderCount[idxHead] = encoderCount[idxHeadPrev] - 1;
                }
            }

            // Else both the current and previous state of A is HIGH or LOW, meaning no transition has
            // occurred thus no need to add or subtract from wheelCounts
            else {
                encoderCount[idxHead] = encoderCount[idxHeadPrev];
            }

            encoderAStatePrevious = encoderAState;
            encoderBStatePrevious = encoderBState;
        }

        private synchronized void updateDistance(){
            distance[idxHead] = encoderCount[idxHead] * mmPerCount;
        }

        private synchronized void updateWheelSpeed(long timestamp) {
            timestamps[idxHead] = timestamp;
            double dt_buffer = (timestamps[idxHead] - timestamps[idxTail]) / 1000000000f;

            if (dt_buffer != 0) {
                // Calculate the speed of each wheel in mm/s.
                speedInstantaneous = (distance[idxHead] - distance[idxHeadPrev]) / 1000000000f;
                speedBuffered = (distance[idxHead] - distance[idxTail]) / dt_buffer;
                speedExponentialAvg = exponentialAvg(speedBuffered, speedExponentialAvg, expWeight);
            }
            else{
                Log.i("sensorDebugging", "dt_buffer == 0");
            }
        }

        private synchronized void updateIndex(){
            idxHeadPrev = idxHead;
            idxHead++;
            idxTail++;
            idxHead = idxHead % bufferLength;
            idxTail = idxTail % bufferLength;
        }

        public synchronized int getLatestEncoderCount() {
            return encoderCount[idxHead];
        }

        public synchronized double getLatestDistance() {
            return distance[idxHead];
        }

        public synchronized double getSpeedBuffered() {
            return speedBuffered;
        }

        public synchronized double getSpeedExponentialAvg() {
            return speedExponentialAvg;
        }

        public synchronized double getSpeedInstantaneous() {
            return speedInstantaneous;
        }

        public synchronized void setExpWeight(double expWeight){this.expWeight = expWeight;}
    }
}
