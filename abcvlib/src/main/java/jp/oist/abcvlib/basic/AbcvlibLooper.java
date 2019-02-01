package jp.oist.abcvlib.basic;

import android.util.Log;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOConnectionManager;

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
 * This thread updates every 1 ms.
 *
 * @author Jiexin Wang https://github.com/ha5ha6
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class AbcvlibLooper extends BaseIOIOLooper {

    /**
     * Boolean to switch on and off logger functions. On by default but can be set to false via
     */
    private boolean loggerOn;

    /**
     * Enable/disable this to swap the polarity of the wheels such that the default forward
     * direction will be swapped (i.e. wheels will move cw vs ccw as forward).
     */
    private boolean wheelPolaritySwap;
    //      --------------Quadrature AbcvlibSensors----------------
    /**
     Creates IOIO Board object that read the quadrature encoders of the Hubee Wheels.
     Using the encoderARightWheel.read() returns a boolean of either high or low telling whether the
     quadrature abcvlibSensors is on a black or white mark at the rime of read.<br><br>

     The encoderARightWheel.waitForValue(true) can also be used to wait for the value read by the quadrature
     abcvlibSensors to change to a given value (true in this case).<br><br>

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

    //     --------------Pulse Width Modulation (PWM)----------------
    /**
     PwmOutput objects like pwmControllerRightWheel have methods like
    <ul>
     <li>openPwmOutput(pinNum, freq) to start the PWM on pinNum at freq</li>

     <li>pwm.setPulseWidth(pw) to change the freq directly by modifying the pulse width</li>
     </ul>

     More info <a href="https://github.com/ytai/ioio/wiki/PWM-Output">here</a>
    */
    private PwmOutput pwmControllerRightWheel;
    /**
     * @see #pwmControllerRightWheel
     */
    private PwmOutput pwmControllerLeftWheel;
    /**
     * PWM frequency. Do not modify locally. Modify at AbcvlibActivity level if necessary.
     */
    private int PWM_FREQ;

    private AbcvlibSensors abcvlibSensors;
    private AbcvlibMotion abcvlibMotion;

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
     * PWM pulse width tracking variable. Values range from 0 to PWM_FREQ
     */
    private int pulseWidthRightWheelNew;
    /**
     * @see #pulseWidthRightWheelNew
     */
    private int pulseWidthRightWheelCurrent;
    /**
     * @see #pulseWidthRightWheelNew
     */
    private int pulseWidthLeftWheelNew;
    /**
     * @see #pulseWidthRightWheelNew
     */
    private int pulseWidthLeftWheelCurrent;

    // Encoder counters
    /**
     * Note encoderCountRightWheel accumulates counts at the AbcvlibLooper thread loop rate of 1ms
     * and then fills in the encoder counter value within
     * AbcvlibSensors.encoderCountRightWheel[indexHistoryCurrent]. The value of indexHistoryCurrent
     * changes only every 5ms or so, so the single
     * AbcvlibSensors.encoderCountRightWheel[indexHistoryCurrent] value represents a summation of
     * approximately 5 loops of counting through the AbcvlibLooper thread loop. The timing of the
     * threads may vary a bit, but this doesn't undermine the general idea of summing up counts
     * then dumping them into the array within AbcvlibSensors. This timing depends on the sleep time
     * provided at the end of the loop method within this class and the value of SENSOR_DELAY_FASTEST
     * in the AbcvlibSensors.register method.
     *
     * @see AbcvlibSensors#encoderCountRightWheel
     * @see #loop()
     * @see AbcvlibSensors#register()
     */
    private int encoderCountRightWheel;
    /**
     * @see #encoderCountRightWheel
     */
    private int encoderCountLeftWheel;

    // Constructor to pass other module objects in. Default loggerOn value to true
    public AbcvlibLooper(AbcvlibSensors abcvlibSensors, AbcvlibMotion abcvlibMotion,
                         Integer PWM_FREQ){

        this(abcvlibSensors, abcvlibMotion, PWM_FREQ, true, false);

    }

    // Constructor to pass other module objects in. No default loggerOn. Needs to remain public
    // despite what Android Studio says
    public AbcvlibLooper(AbcvlibSensors abcvlibSensors, AbcvlibMotion abcvlibMotion,
                         Integer PWM_FREQ, Boolean loggerOn, Boolean wheelPolaritySwap){

        this.abcvlibMotion = abcvlibMotion;
        this.abcvlibSensors = abcvlibSensors;
        this.PWM_FREQ = PWM_FREQ;
        this.loggerOn = loggerOn;
        this.wheelPolaritySwap = wheelPolaritySwap;
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
        final int INPUT1_RIGHT_WHEEL_PIN = 5;
        final int INPUT2_RIGHT_WHEEL_PIN = 6;
        final int INPUT1_LEFT_WHEEL_PIN = 10;
        final int INPUT2_LEFT_WHEEL_PIN = 11;
        final int PWM_RIGHT_WHEEL_PIN = 7;
        final int PWM_LEFT_WHEEL_PIN = 12;
        final int ENCODER_A_RIGHT_WHEEL_PIN=34;
        final int ENCODER_B_RIGHT_WHEEL_PIN=35;
        final int ENCODER_A_LEFT_WHEEL_PIN=36;
        final int ENCODER_B_LEFT_WHEEL_PIN=37;

        /* Initializing all wheel controller values to low would result in both wheels being in
         the "Stop-NoBrake" mode according to the Hubee control table. Not sure if this state
         is required for some reason or just what was defaulted to. **/
        input1RightWheelController = ioio_.openDigitalOutput(INPUT1_RIGHT_WHEEL_PIN,false);
        input2RightWheelController = ioio_.openDigitalOutput(INPUT2_RIGHT_WHEEL_PIN,false);
        input1LeftWheelController = ioio_.openDigitalOutput(INPUT1_LEFT_WHEEL_PIN,false);
        input2LeftWheelController = ioio_.openDigitalOutput(INPUT2_LEFT_WHEEL_PIN,false);

        // This try-catch statement should likely be refined to handle common errors/exceptions
        try{
            pwmControllerRightWheel = ioio_.openPwmOutput(PWM_RIGHT_WHEEL_PIN,PWM_FREQ);
            pwmControllerLeftWheel = ioio_.openPwmOutput(PWM_LEFT_WHEEL_PIN,PWM_FREQ);

            /* Note openDigitalInput() can also accept DigitalInput.Spec.Mode.OPEN_DRAIN if motor
            circuit requires */
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
    }

    /**
     * Called repetitively while the IOIO is connected.
     *
     * @throws ConnectionLostException
     *             When IOIO connection is lost.
     * @throws InterruptedException
     * 				When the IOIO thread has been interrupted.
     *
     * @see ioio.lib.util.IOIOLooper#loop()
     */
    @Override
    public void loop() throws ConnectionLostException, InterruptedException{

        getPwm();

        getIn1In2();

        getEncoderStates();

        getEncoderCounts();

        writeIoUpdates();

        if (loggerOn) {
            sendToLog();
        }

        try {
            // TODO Not sure if this is necessary. Seems like better thread wait/notify would be better?
            IOIOConnectionManager.Thread.sleep(1);
        } catch (InterruptedException e) {
            Log.i("abcvlib", "AbcvlibLooper.loop threw an InteruptedException");
            throw e;
        }
    }

    /**
     * Called when the IOIO is disconnected.
     *
     * @see ioio.lib.util.IOIOLooper#disconnected()
     */
    @Override
    public void disconnected() {
        abcvlibSensors.unregister();
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

    private void getPwm() {

        // pulseWidths are given in microseconds
        pulseWidthRightWheelCurrent = abcvlibMotion.getPwRight();
        pulseWidthLeftWheelCurrent = abcvlibMotion.getPwLeft();

        pulseWidthRightWheelNew = pulseWidthLimiter(pulseWidthRightWheelCurrent);
        pulseWidthLeftWheelNew = pulseWidthLimiter(pulseWidthLeftWheelCurrent);

    }

    /**
     * Tests the sign of pulseWidth then determines how to set the input variables (IN1 and IN2)
     * to control the Hubee Wheel direction. See input1RightWheelController doc for control table.
     * If you wanted to inverse polarity, just reverse the > signs to < in each if statement.
     */
    private void getIn1In2(){

        if (wheelPolaritySwap) {
            if(pulseWidthRightWheelCurrent >= 0){
                input1RightWheelState = false;
                input2RightWheelState = true;
            }else{
                input1RightWheelState = true;
                input2RightWheelState = false;
            }

            if(pulseWidthLeftWheelCurrent >= 0){
                input1LeftWheelState = false;
                input2LeftWheelState = true;
            }else{
                input1LeftWheelState = true;
                input2LeftWheelState = false;
            }
        }
        else {
            if(pulseWidthRightWheelCurrent <= 0){
                input1RightWheelState = false;
                input2RightWheelState = true;
            }else{
                input1RightWheelState = true;
                input2RightWheelState = false;
            }

            if(pulseWidthLeftWheelCurrent <= 0){
                input1LeftWheelState = false;
                input2LeftWheelState = true;
            }else{
                input1LeftWheelState = true;
                input2LeftWheelState = false;
            }
        }

    }

    private void getEncoderStates() throws ConnectionLostException{

        try {
            // Read all encoder values from IOIO Board
            encoderARightWheelState = encoderARightWheel.read();
            encoderBRightWheelState = encoderBRightWheel.read();
            encoderALeftWheelState = encoderALeftWheel.read();
            encoderBLeftWheelState = encoderBLeftWheel.read();

            // Intentional empty catch block?
        } catch (InterruptedException e) {
            Log.i("abcvlib", "AbcvlibLooper.loop threw an InteruptedException");
        } catch (ConnectionLostException e){
            Log.i("abcvlib", "AbcvlibLooper.loop threw an ConnectionLostException");
            throw e;
        }
    }

    private void getEncoderCounts(){

        // Right is negative and left is positive since the wheels are physically mirrored so
        // while moving forward one wheel is moving ccw while the other is rotating cw.
        encoderCountRightWheel = encoderCountRightWheel -
                encoderAddSubtractCount(input1RightWheelState, input2RightWheelState,
                        pulseWidthRightWheelNew, pulseWidthLeftWheelNew, encoderARightWheelState,
                        encoderBRightWheelState, encoderARightWheelStatePrevious,
                        encoderBRightWheelStatePrevious);

        encoderCountLeftWheel = encoderCountLeftWheel +
                encoderAddSubtractCount(input1LeftWheelState, input2LeftWheelState,
                        pulseWidthRightWheelNew, pulseWidthLeftWheelNew, encoderALeftWheelState,
                        encoderBLeftWheelState, encoderALeftWheelStatePrevious,
                        encoderBLeftWheelStatePrevious);
    }

    private void writeIoUpdates() throws ConnectionLostException{

        try {
            // Write all calculated values to the IOIO Board pins
            input1RightWheelController.write(input1RightWheelState);
            input2RightWheelController.write(input2RightWheelState);
            pwmControllerRightWheel.setPulseWidth(pulseWidthRightWheelNew);
            input1LeftWheelController.write(input1LeftWheelState);
            input2LeftWheelController.write(input2LeftWheelState);
            pwmControllerLeftWheel.setPulseWidth(pulseWidthLeftWheelNew);

        } catch (ConnectionLostException e){
            Log.i("abcvlib", "AbcvlibLooper.loop threw an ConnectionLostException");
            throw e;
        }

        abcvlibSensors.setWheelR(encoderCountRightWheel);
        abcvlibSensors.setWheelL(encoderCountLeftWheel);

        encoderARightWheelStatePrevious = encoderARightWheelState;
        encoderBRightWheelStatePrevious = encoderBRightWheelState;
        encoderALeftWheelStatePrevious = encoderALeftWheelState;
        encoderBLeftWheelStatePrevious = encoderBLeftWheelState;
    }

    private int pulseWidthLimiter(Integer pulseWidthOld){

        final int MAX_PULSE_WIDTH = PWM_FREQ;

        /*
        The following two logical statements simply hard limit the pulseWidth to be less than
        MAX_PULSE_WIDTH which represents the highest value it can be.
         */
        int pulseWidthNew;

        if(pulseWidthOld < MAX_PULSE_WIDTH){
            pulseWidthNew = Math.abs(pulseWidthOld);
        }else{
            pulseWidthNew = MAX_PULSE_WIDTH;
        }

        return pulseWidthNew;
    }

    /**
     Input all IO values from Hubee Wheel and output either +1, or -1 to add or subtract one wheel
     count.

     The combined values of input1WheelStateIo and input2WheelStateIo control the direction of the
     Hubee wheels.

     encoderAWheelState and encoderBWheelState are the direct current IO reading (high or low) of
     the quadrature encoders on the Hubee wheels. See Hubee wheel documentation regarding which IO
     corresponds to the A and B IO.

     <br><br>
     <img src="../../../../../../../../../../media/images/hubeeWheel.gif" />
     <br><br>

     encoderAWheelStatePrevious and encoderBWheelStatePrevious are previous state of their
     corresponding variables.

     IN1  IN2 PWM Standby Result
     H    H   H/L H   Stop-Brake
     L    H   H   H   Turn Forwards
     L    H   L   H   Stop-Brake
     H    L   H   H   Turn Backwards
     H    L   L   H   Stop-Brake
     L    L   H/L H   Stop-NoBrake
     H/L  H/L H/L L   Standby

     * @return wheelCounts
     */
    private int encoderAddSubtractCount(Boolean input1WheelStateIo, Boolean input2WheelStateIo, Integer pulseWidthRightWheelNew,
                                Integer pulseWidthLeftWheelNew, Boolean encoderAWheelState, Boolean encoderBWheelState,
                                Boolean encoderAWheelStatePrevious, Boolean encoderBWheelStatePrevious){

        int wheelCounts = 0;
        /*
        Java exclusive OR logic ^. I.e. only calculate if one and only one WheelState is true (H).
        Additionally both PWM and Standby must be H, but Standby is always H by default in its
        unconnected state. This ensures that you are only modifying the wheel counts when the wheel
        is moving either forward or backward (as opposed to being stopped/braked). Additionally
        the PWM pin will alter between H and L during normal operation, so checking if the
        pulseWidthRightWheelNew values are above 0 will ensure only moving wheels are counted.
        The else statements can then differentiate between a stopped wheel and
        a misread from the quadrature encoders. This allows you to calculate the drift/error of the
        quadrature sensors to some degree, though you wouldn't be able to tell whether the drift is
        positive or negative or evens out over time. I guess you could use this to calculate an
        appropriate sampling frequency for the IOIOBoard. If you are getting too many of these errors
        maybe decreasing the sampling rate will remove the number of times the encoders are read
        precisely at the wrong moment (both H or both L). Will this happen?
         */
        if((input1WheelStateIo ^ input2WheelStateIo) && pulseWidthRightWheelNew > 0 && pulseWidthLeftWheelNew > 0){
            // Previous Encoder A HIGH, B HIGH
            if(encoderAWheelStatePrevious && encoderBWheelStatePrevious){
                // Current Encoder A LOW, B HIGH
                if(!encoderAWheelState && encoderBWheelState){
                    wheelCounts++;
                }
                // Current Encoder A HIGH, B LOW
                else if(encoderAWheelState && !encoderBWheelState){
                    wheelCounts--;
                }
                else{
                    Log.w("abcvlibEncoder", "Quadrature encoders read H/H or L/L when they " +
                            "should have read H/L or L/H");
                }
            }
            // Previous Encoder A LOW, B HIGH
            else if(!encoderAWheelStatePrevious && encoderBWheelStatePrevious){
                // Current Encoder A LOW, B LOW
                if(!encoderAWheelState && !encoderBWheelState){
                    wheelCounts++;
                }
                // Current Encoder A HIGH, B HIGH
                else if(encoderAWheelState && encoderBWheelState){
                    wheelCounts--;
                }
                else{
                    Log.w("abcvlibEncoder", "Quadrature encoders read H/L or L/H when they " +
                            "should have read H/H or L/L");
                }
            }
            // Previous Encoder A LOW, B LOW. Leave "always true" warning from Android Studio for
            // readability.
            else if(!encoderAWheelStatePrevious && !encoderBWheelStatePrevious){
                // Current Encoder A HIGH, B LOW
                if(encoderAWheelState && !encoderBWheelState){
                    wheelCounts++;
                }
                // Current Encoder A LOW, B HIGH
                else if(!encoderAWheelState && encoderBWheelState){
                    wheelCounts--;
                }
                else{
                    Log.w("abcvlibEncoder", "Quadrature encoders read H/H or L/L when they " +
                            "should have read H/L or L/H");
                }
            }
            // Previous Encoder A HIGH, B LOW. Leave "always true" warning from Android Studio for
            // readability.
            else if(encoderAWheelStatePrevious &&! encoderBWheelStatePrevious){
                // Current Encoder A HIGH, B HIGH
                if(encoderAWheelState && encoderBWheelState){
                    wheelCounts++;
                }
                // Current Encoder A LOW, B LOW
                else if(!encoderAWheelState && !encoderBWheelState){
                    wheelCounts--;
                }
                else{
                    Log.w("abcvlibEncoder", "Quadrature encoders read H/L or L/H when they " +
                            "should have read H/H or L/L");
                }
            }
        }

        return wheelCounts;
    }

    private void sendToLog() {



        // Compile Encoder state data to push to adb log
        String encoderStateMsg = Integer.toString((encoderARightWheelState) ? 1 : 0) + " " +
                Integer.toString((encoderBRightWheelState) ? 1 : 0) + " " +
                Integer.toString((encoderALeftWheelState) ? 1 : 0) + " " +
                Integer.toString((encoderBLeftWheelState) ? 1 : 0);

        // Compile Encoder count data to push to adb log
        String encoderCountMsg = Float.toString(encoderCountRightWheel) + " " +
                Float.toString(encoderCountLeftWheel);

        // Compile PWM data to push to adb log
        String pwmMsg = Float.toString(pulseWidthRightWheelCurrent) + " " +
                Float.toString(pulseWidthLeftWheelCurrent) + " " +
                Float.toString(pulseWidthRightWheelNew) + " " +
                Float.toString(pulseWidthLeftWheelNew);

        Log.i("encoderStateMsg", encoderStateMsg);
        Log.i("encoderCountMsg", encoderCountMsg);
        Log.i("pwmMsg", pwmMsg);

    }

}
