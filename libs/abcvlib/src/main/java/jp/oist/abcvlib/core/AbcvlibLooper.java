package jp.oist.abcvlib.core;

import android.util.Log;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOConnectionManager;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;

/**
 * AbcvlibLooper provides the connection with the IOIOBoard by allowing access to the loop
 * function being called by the software on the IOIOBoard itself. All class variables and
 * contents of the setup() and loop() methods are passed upward to the respective parent classes
 * related to the core IOIOBoard operation by extending BaseIOIOLooper.
 *
 * AbcvlibLooper represents the "control" thread mentioned in the git wiki. It sets up the IOIO
 * Board pin connections, reads the encoder values, and writes out the total encoder counts for
 * wheel speed calculations elsewhere.
 *
 * @author Jiexin Wang https://github.com/ha5ha6
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class AbcvlibLooper extends BaseIOIOLooper {

    private final String TAG = this.getClass().getName();

    private int indexCurrent = 1;
    private int indexPrevious = 0;
    private int loopCount = 1;
    private final int buffer = 5;
    private final long[] timeStamp = new long[buffer];

    //      --------------Quadrature Encoders----------------
    /**
     Creates IOIO Board object that read the quadrature encoders of the Hubee Wheels.
     Using the encoderARightWheel.read() returns a boolean of either high or low telling whether the
     quadrature encoder is on a black or white mark at the rime of read.<br><br>

     The encoderARightWheel.waitForValue(true) can also be used to wait for the value read by the quadrature
     encoder to change to a given value (true in this case).<br><br>

     encoderARightWheelStatePrevious is just the previous reading of encoderARightWheel<br><br>

     For more on quadrature encoders see
     <a href="http://www.creative-robotics.com/bmdsresources">here</a> OR
     <a href="https://en.wikipedia.org/wiki/Rotary_encoder#Incremental_rotary_encoder">here</a><br><br>

     For more on IOIO Board DigitalInput objects see:
     <a href="https://github.com/ytai/ioio/wiki/Digital-IO">here</a><br><br>
    */
    private DigitalInput encoderARightWheel;
    /**
     * @see #encoderARightWheel
     */
    private DigitalInput encoderBRightWheel;
    /**
     * @see #encoderARightWheel
     */
    private DigitalInput encoderALeftWheel;
    /**
     * @see #encoderARightWheel
     */
    private DigitalInput encoderBLeftWheel;
    /**
     * @see #encoderARightWheel
     */
    private boolean encoderARightWheelStatePrevious;
    /**
     * @see #encoderARightWheel
     */
    private boolean encoderBRightWheelStatePrevious;
    /**
     * @see #encoderARightWheel
     */
    private boolean encoderALeftWheelStatePrevious;
    /**
     * @see #encoderARightWheel
     */
    private boolean encoderBLeftWheelStatePrevious;

    //     --------------Wheel Direction Controllers----------------
    /**
     The values set by input1RightWheelController.write() control the direction of the Hubee wheels.
     See <a hred="http://www.creative-robotics.com/bmdsresources">here</a> for the source of the
     control value table copied below:<br><br>

     Table below refers to a single wheel
     (e.g. setting input1RightWheelController to H and input2RightWheelController to L
     with PWM H and Standby H would result in the right wheel turning backwards)<br><br>

     Setting input1RightWheelController to H is done via input1RightWheelController.write(true)<br><br>

     <table style="width:100%">
        <tr>
            <th>IN1</th>
            <th>IN2</th>
            <th>PWM</th>
            <th>Standby</th>
            <th>Result</th>
        </tr>
        <tr>
            <th>H</th>
            <th>H</th>
            <th>H/L</th>
            <th>H</th>
            <th>Stop-Brake</th>
        </tr>
        <tr>
            <th>L</th>
            <th>H</th>
            <th>H</th>
            <th>H</th>
            <th>Turn Forwards</th>
        </tr>
        <tr>
            <th>L</th>
            <th>H</th>
            <th>L</th>
            <th>H</th>
            <th>Stop-Brake</th>
        </tr>
        <tr>
            <th>H</th>
            <th>L</th>
            <th>H</th>
            <th>H</th>
            <th>Turn Backwards</th>
        </tr>
        <tr>
            <th>H</th>
            <th>L</th>
            <th>L</th>
            <th>H</th>
            <th>Stop-Brake</th>
        </tr>
        <tr>
            <th>L</th>
            <th>L</th>
            <th>H/L</th>
            <th>H</th>
            <th>Stop-NoBrake</th>
        </tr>
        <tr>
            <th>H/L</th>
            <th>H/L</th>
            <th>H/L</th>
            <th>L</th>
            <th>Standby</th>
        </tr>
     </table>
    */
    private DigitalOutput input1RightWheelController;
    /**
     * @see #input1RightWheelController
     */
    private DigitalOutput input2RightWheelController;
    /**
     * @see #input1RightWheelController
     */
    private DigitalOutput input1LeftWheelController;
    /**
     * @see #input1RightWheelController
     */
    private DigitalOutput input2LeftWheelController;
    /**
     * Monitors onboard battery voltage (note this is not the smartphone battery voltage. The
     * smartphone battery should always be fully charged as it will draw current from the onboard
     * battery until the onboard battery dies)
     */
    private AnalogInput batteryVoltageMonitor;
    /**
     * Monitors external charger (usb or wirelss coil) voltage. Use this to detect on charge puck or not. (H/L)
     */
    private AnalogInput chargerVoltageMonitor;
    /**
     * Monitors wireless receiver coil voltage at coil (not after Qi regulator). Use this to detect on charge puck or not. (H/L)
     */
    private AnalogInput coilVoltageMonitor;

    //     --------------Pulse Width Modulation (PWM)----------------
    /**
     PwmOutput objects like pwmControllerRightWheel have methods like
    <ul>
     <li>openPwmOutput(pinNum, freq) to start the PWM on pinNum at freq</li>

     <li>pwm.setDutyCycle(dutycycle) to change the freq directly by modifying the pulse width</li>
     </ul>

     More info <a href="https://github.com/ytai/ioio/wiki/PWM-Output">here</a>
    */
    private PwmOutput pwmControllerRightWheel;
    /**
     * @see #pwmControllerRightWheel
     */
    private PwmOutput pwmControllerLeftWheel;

    /**
     * Boolean representing the current state (H/L) of the ChA and ChB on the HubeeWheels
     */
    private boolean encoderARightWheelState;
    /**
     * @see #encoderARightWheelState
     */
    private boolean encoderBRightWheelState;
    /**
     * @see #encoderARightWheelState
     */
    private boolean encoderALeftWheelState;
    /**
     * @see #encoderARightWheelState
     */
    private boolean encoderBLeftWheelState;

    /**
     * The IN1 and IN2 IO determining Hubee Wheel direction. See input1RightWheelController doc for
     * control table
     *
     * @see #input1RightWheelController
     */
    private boolean input1RightWheelState;
    /**
     * @see #input1RightWheelState
     */
    private boolean input2RightWheelState;
    /**
     * @see #input1RightWheelState
     */
    private boolean input1LeftWheelState;
    /**
     * @see #input1RightWheelState
     */
    private boolean input2LeftWheelState;

    /**
     * Duty cycle of PWM pulse width tracking variable. Values range from 0 to 100
     */
    private float dutyCycleRightWheelNew;
    /**
     * @see #dutyCycleRightWheelNew
     */
    private float dutyCycleRightWheelCurrent;
    /**
     * @see #dutyCycleRightWheelNew
     */
    private float dutyCycleLeftWheelNew;
    /**
     * @see #dutyCycleRightWheelNew
     */
    private float dutyCycleLeftWheelCurrent;

    // Encoder counters
    /**
     * Note encoderCountRightWheel accumulates counts at the AbcvlibLooper thread loop rate of 1ms
     * and then fills in the encoder counter value within
     * QuadEncoders.encoderCountRightWheel[indexHistoryCurrent]. The value of indexHistoryCurrent
     * changes only every 5ms or so, so the single
     * QuadEncoders.encoderCountRightWheel[indexHistoryCurrent] value represents a summation of
     * approximately 5 loops of counting through the AbcvlibLooper thread loop. The timing of the
     * threads may vary a bit, but this doesn't undermine the general idea of summing up counts
     * then dumping them into the array within QuadEncoders.
     *
     * @see #loop()
     */
    private final int[] encoderCountRightWheel = new int[buffer];
    /**
     * @see #encoderCountRightWheel
     */
    private final int[] encoderCountLeftWheel = new int[buffer];

    private BatteryData batteryData = null;
    private WheelData wheelData = null;

    // Constructor to pass other module objects in. No default loggerOn. Needs to remain public
    // despite what Android Studio says
    public AbcvlibLooper(AbcvlibActivity abcvlibActivity){
        if (abcvlibActivity != null){
            this.batteryData = abcvlibActivity.getInputs().getBatteryData();
            this.wheelData = abcvlibActivity.getInputs().getWheelData();
        }
        Log.d("abcvlib", "AbcvlibLooper constructor finished");
    }

    /**
     * Called every time a connection with IOIO has been established.
     * Typically used to open pins.
     *
     * @throws ConnectionLostException
     *             When IOIO connection is lost.
     *
     * @see ioio.lib.util.IOIOLooper#setup(IOIO)
     */
    @Override
    public void setup() throws ConnectionLostException {

        /*
         --------------IOIO Board PIN References----------------
         Although several other pins would work, there are restrictions on which pins can be used to
         PWM and which pins can be used for analog/digital purposes. See back of IOIO Board for pin
         mapping.

         Note the INPUTX_XXXX pins were all placed on 5V tolerant pins, but Hubee wheel inputs
         operate from 3.3 to 5V, so any other pins would work just as well.

         Although the encoder pins were chosen to be on the IOIO board analog in pins, this is not
         necessary as the encoder objects only read digital high and low values.

         PWM pins are currently on pins with P (peripheral) and 5V tolerant pins. The P capability is
         necessary in order to properly use the PWM based methods (though not sure if these are even
         used). The 5V tolerant pins are not necessary as the IOIO Board PWM is a 3.3V peak signal.
         */

        Log.d("abcvlib", "AbcvlibLooper setup() started");

        final int INPUT1_RIGHT_WHEEL_PIN = 2;
        final int INPUT2_RIGHT_WHEEL_PIN = 3;
        final int PWM_RIGHT_WHEEL_PIN = 4;
        final int ENCODER_A_RIGHT_WHEEL_PIN = 6;
        final int ENCODER_B_RIGHT_WHEEL_PIN=7;

        final int INPUT1_LEFT_WHEEL_PIN = 11;
        final int INPUT2_LEFT_WHEEL_PIN = 12;
        final int PWM_LEFT_WHEEL_PIN = 13;
        final int ENCODER_A_LEFT_WHEEL_PIN=15;
        final int ENCODER_B_LEFT_WHEEL_PIN=16;
        
        final int CHARGER_VOLTAGE = 33;
        final int BATTERY_VOLTAGE = 34;
        final int COIL_VOLTAGE = 35;

        Log.v(TAG, "ioio_ state = " + ioio_.getState().toString());

        /*
        Initializing all wheel controller values to low would result in both wheels being in
        the "Stop-NoBrake" mode according to the Hubee control table. Not sure if this state
        is required for some reason or just what was defaulted to.
        */
        input1RightWheelController = ioio_.openDigitalOutput(INPUT1_RIGHT_WHEEL_PIN,false);
        input2RightWheelController = ioio_.openDigitalOutput(INPUT2_RIGHT_WHEEL_PIN,false);
        input1LeftWheelController = ioio_.openDigitalOutput(INPUT1_LEFT_WHEEL_PIN,false);
        input2LeftWheelController = ioio_.openDigitalOutput(INPUT2_LEFT_WHEEL_PIN,false);

        batteryVoltageMonitor = ioio_.openAnalogInput(BATTERY_VOLTAGE);
        chargerVoltageMonitor = ioio_.openAnalogInput(CHARGER_VOLTAGE);
        coilVoltageMonitor = ioio_.openAnalogInput(COIL_VOLTAGE);

        // This try-catch statement should likely be refined to handle common errors/exceptions
        try{
            /*
             * PWM frequency. Do not modify locally. Modify at AbcvlibActivity level if necessary
             *  Not sure why initial PWM_FREQ is 1000, but assume this can be modified as necessary.
             *  This may depend on the motor or microcontroller requirements/specs. <br><br>
             *
             *  If motor is just a DC motor, I guess this does not matter much, but for servos, this would
             *  be the control function, so would have to match the baud rate of the microcontroller. Note
             *  this library is not set up to control servos at this time. <br><br>
             *
             *  The microcontroller likely has a maximum frequency which it can turn ON/OFF the IO, so
             *  setting PWM_FREQ too high may cause issues for certain microcontrollers.
             */
            int PWM_FREQ = 1000;
            pwmControllerRightWheel = ioio_.openPwmOutput(PWM_RIGHT_WHEEL_PIN, PWM_FREQ);
            pwmControllerLeftWheel = ioio_.openPwmOutput(PWM_LEFT_WHEEL_PIN, PWM_FREQ);

            /*
            Note openDigitalInput() can also accept DigitalInput.Spec.Mode.OPEN_DRAIN if motor
            circuit requires
            */
            encoderARightWheel = ioio_.openDigitalInput(ENCODER_A_RIGHT_WHEEL_PIN,
                    DigitalInput.Spec.Mode.PULL_UP);
            encoderBRightWheel = ioio_.openDigitalInput(ENCODER_B_RIGHT_WHEEL_PIN,
                    DigitalInput.Spec.Mode.PULL_UP);
            encoderALeftWheel = ioio_.openDigitalInput(ENCODER_A_LEFT_WHEEL_PIN,
                    DigitalInput.Spec.Mode.PULL_UP);
            encoderBLeftWheel = ioio_.openDigitalInput(ENCODER_B_LEFT_WHEEL_PIN,
                    DigitalInput.Spec.Mode.PULL_UP);

        }catch (ConnectionLostException e){
            Log.e("abcvlib", "ConnectionLostException at AbcvlibLooper.setup()");
            throw e;
        }
        Log.d("abcvlib", "AbcvlibLooper setup() finished");
    }

    /**
     * Called repetitively while the IOIO is connected.
     *
     * @see ioio.lib.util.IOIOLooper#loop()
     */
    @Override
    public void loop() {

        try {
            timeStampUpdate();

            getDutyCycle();

            getIn1In2();

            getEncoderStates();

            getEncoderCounts();

            writeIoUpdates();

            updateQuadEncoders();

            updateBatteryVoltage();

            updateChargerVoltage();

            indexUpdate();
        }
        catch (ConnectionLostException e){
            Log.e("abcvlib", "connection lost in AbcvlibLooper.loop");
        }

        IOIOConnectionManager.Thread.yield();
    }

    /**
     * Called when the IOIO is disconnected.
     *
     * @see ioio.lib.util.IOIOLooper#disconnected()
     */
    @Override
    public void disconnected() {
        Log.d("abcvlib", "AbcvlibLooper disconnected");
    }

    /**
     * Called when the IOIO is connected, but has an incompatible firmware version.
     *
     * @see ioio.lib.util.IOIOLooper#incompatible(IOIO)
     */
    @Override
    public void incompatible() {
        Log.e("abcvlib", "Incompatible IOIO firmware version!");
    }

    public int getBuffer(){
        return buffer;
    }

    public long[] getTimeStamp() {
        return timeStamp;
    }

    /**
     * Call {@link System#nanoTime()}
     */
    private void timeStampUpdate(){
        timeStamp[indexCurrent] = System.nanoTime();
    }

    /**
     * Calculates dutyCycle{Right,Left}WheelNew by limiting the current value of
     * dutyCycle{Right,Left}WheelCurrent to the inclusive range of 0 and 1.
     */
    private void getDutyCycle() {
        dutyCycleRightWheelNew = dutyCycleLimiter(dutyCycleRightWheelCurrent);
        dutyCycleLeftWheelNew = dutyCycleLimiter(dutyCycleLeftWheelCurrent);
    }

    /**
     * Tests the sign of dutyCycle then determines how to set the input variables (IN1 and IN2)
     * to control the Hubee Wheel direction. See {@link #input1RightWheelController} doc for control table.
     * If you wanted to inverse polarity, just reverse the > signs to < in each if statement.
     */
    private void getIn1In2(){

        if(dutyCycleRightWheelCurrent >= 0){
            input1RightWheelState = false;
            input2RightWheelState = true;
        }else{
            input1RightWheelState = true;
            input2RightWheelState = false;
        }

        if(dutyCycleLeftWheelCurrent >= 0){
            input1LeftWheelState = false;
            input2LeftWheelState = true;
        }else{
            input1LeftWheelState = true;
            input2LeftWheelState = false;
        }

    }

    /**
     * Reads the high/low value of the quadrature encoders (two pairs) directly off the ioioboard
     * digital pins.
     * @throws ConnectionLostException called when connection lost while trying to read ioio pins.
     */
    private void getEncoderStates() throws ConnectionLostException{

        try {
            // Read all encoder values from IOIO Board
            encoderARightWheelState = encoderARightWheel.read();
            encoderBRightWheelState = encoderBRightWheel.read();
            encoderALeftWheelState = encoderALeftWheel.read();
            encoderBLeftWheelState = encoderBLeftWheel.read();

            // Intentional empty catch block?
        } catch (InterruptedException e) {
            Log.i("abcvlib", "AbcvlibLooper.loop threw an InteruptedException in getEncoderStates");
            Log.e(TAG,"Error", e);
        } catch (ConnectionLostException e){
            Log.i("abcvlib", "AbcvlibLooper.loop threw an ConnectionLostException");
            Log.e(TAG,"Error", e);
            throw e;
        }
    }

    /**
     * Adds or subtracts counts to/from encoderCountRightWheel[indexPrevious] and sets this as the
     * value for encoderCountRightWheel[indexCurrent]
     */
    private void getEncoderCounts(){
        // Right is negative and left is positive since the wheels are physically mirrored so
        // while moving forward one wheel is moving ccw while the other is rotating cw.
        encoderCountRightWheel[indexCurrent] = encoderCountRightWheel[indexPrevious] -
                encoderAddSubtractCount(
                        encoderARightWheelState,
                        encoderBRightWheelState, encoderARightWheelStatePrevious,
                        encoderBRightWheelStatePrevious
                );

        encoderCountLeftWheel[indexCurrent] = encoderCountLeftWheel[indexPrevious] +
                encoderAddSubtractCount(
                        encoderALeftWheelState,
                        encoderBLeftWheelState, encoderALeftWheelStatePrevious,
                        encoderBLeftWheelStatePrevious
                );
    }

    /**
     * Write the high/low values on the wheel In1In2 pins and set the duty cycle on the PWM pins.
     * This sets the wheel direction and speed. After this is done, set the  encoderARightWheelStatePrevious
     * and other "Previous" values to current values to prepare for next loop.
     * @throws ConnectionLostException called when connection lost while trying to read ioio pins.
     */
    private void writeIoUpdates() throws ConnectionLostException{

        try {
            // Write all calculated values to the IOIO Board pins
            input1RightWheelController.write(input1RightWheelState);
            input2RightWheelController.write(input2RightWheelState);
            pwmControllerRightWheel.setDutyCycle(dutyCycleRightWheelNew); //converting from duty cycle to pulse width
            input1LeftWheelController.write(input1LeftWheelState);
            input2LeftWheelController.write(input2LeftWheelState);
            pwmControllerLeftWheel.setDutyCycle(dutyCycleLeftWheelNew);//converting from duty cycle to pulse width
        } catch (ConnectionLostException e){
            Log.i("abcvlib", "AbcvlibLooper.loop threw an ConnectionLostException");
            throw e;
        }

        encoderARightWheelStatePrevious = encoderARightWheelState;
        encoderBRightWheelStatePrevious = encoderBRightWheelState;
        encoderALeftWheelStatePrevious = encoderALeftWheelState;
        encoderBLeftWheelStatePrevious = encoderBLeftWheelState;
    }

    /**
     * Returns a hard limited value for the dutyCycle to be within the inclusive range of [0,1].
     * @param dutyCycleOld un-limited duty cycle
     * @return limited duty cycle
     */
    private float dutyCycleLimiter(float dutyCycleOld){

        final int MAX_DUTY_CYCLE = 1;

        float dutyCycleNew;

        if(Math.abs(dutyCycleOld) < MAX_DUTY_CYCLE){
            dutyCycleNew = Math.abs(dutyCycleOld);
        }else{
            dutyCycleNew = MAX_DUTY_CYCLE;
        }

        return dutyCycleNew;
    }

    /**
     * Writes timestamps and counts of each wheel to an external listener interface called {@link #wheelData}
     */
    private void updateQuadEncoders(){
        wheelData.onWheelDataUpdate(timeStamp[indexCurrent], encoderCountLeftWheel[indexCurrent], encoderCountRightWheel[indexCurrent]);
    }

    /**
     * Reads analog voltage value off charger and coil monitoring pins then writes timestamp and
     * voltages to an external listener interface called {@link #batteryData}
     */
    private void updateChargerVoltage(){

        double chargerVoltage = 0;
        double coilVoltage = 0;

        try {
            chargerVoltage = chargerVoltageMonitor.getVoltage();
            coilVoltage = coilVoltageMonitor.getVoltage();
        } catch (InterruptedException | ConnectionLostException e) {
            Log.e(TAG,"Error", e);
        }
        batteryData.onChargerVoltageUpdate(chargerVoltage, coilVoltage, timeStamp[indexCurrent]);
    }

    /**
     * Reads analog voltage value off battery monitoring pin then writes timestamp and
     * voltage to an external listener interface called {@link #batteryData}
     */
    private void updateBatteryVoltage(){

        double batteryVoltage = 0;

        try {
            batteryVoltage = batteryVoltageMonitor.getVoltage();
        } catch (InterruptedException | ConnectionLostException e) {
            Log.e(TAG,"Error", e);
        }
        batteryData.onBatteryVoltageUpdate(batteryVoltage, timeStamp[indexCurrent]);
    }

    /**
     Input all IO values from Hubee Wheel and output either +1, or -1 to add or subtract one wheel
     count.<br><br>

     The combined values of input1WheelStateIo and input2WheelStateIo control the direction of the
     Hubee wheels.<br><br>

     encoderAWheelState and encoderBWheelState are the direct current IO reading (high or low) of
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
    private int encoderAddSubtractCount(Boolean encoderAWheelState,
                                        Boolean encoderBWheelState,
                                        Boolean encoderAWheelStatePrevious,
                                        Boolean encoderBWheelStatePrevious){
        int wheelCounts = 0;

        // Channel A goes from Low to High
        if (!encoderAWheelStatePrevious && encoderAWheelState){
            // Channel B is Low = Clockwise
            if (!encoderBWheelState){
                wheelCounts++;
            }
            // Channel B is High = CounterClockwise
            else {
                wheelCounts--;
            }
        }

        // Channel A goes from High to Low
        else if (encoderAWheelStatePrevious && !encoderAWheelState){
            // Channel B is Low = CounterClockwise
            if (!encoderBWheelState){
                wheelCounts--;
            }
            // Channel B is High = Clockwise
            else {
                wheelCounts++;
            }
        }

        // Channel B goes from Low to High
        else if (!encoderBWheelStatePrevious && encoderBWheelState){
            // Channel A is Low = CounterClockwise
            if (!encoderAWheelState){
                wheelCounts--;
            }
            // Channel A is High = Clockwise
            else {
                wheelCounts++;
            }
        }

        // Channel B goes from High to Low
        else if (encoderBWheelStatePrevious && !encoderBWheelState){
            // Channel A is Low = Clockwise
            if (!encoderAWheelState){
                wheelCounts++;
            }
            // Channel A is High = CounterClockwise
            else {
                wheelCounts--;
            }
        }

        // Else both the current and previous state of A is HIGH or LOW, meaning no transition has
        // occurred thus no need to add or subtract from wheelCounts

        return wheelCounts;
    }

    /**
     * Calculates and sets the {@link #indexCurrent} and {@link #indexPrevious} values based on
     * current value of {@link #loopCount} and {@link #buffer} size. After this it increments the
     * {@link #loopCount} by 1.
     */
    private void indexUpdate(){
        indexCurrent = loopCount % buffer;
        indexPrevious = (loopCount - 1) % buffer;
        loopCount++;
    }

    public void setDutyCycle(float left, float right) {
        dutyCycleLeftWheelCurrent = left;
        dutyCycleRightWheelCurrent = right;
    }
}
