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
 * @author Jiexin Wang https://github.com/ha5ha6
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class AbcvlibLooper extends BaseIOIOLooper {

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
     *  Not sure why initial PWM_FREQ is 1000, but assume this can be modified as necessary.
     *  This may depend on the motor or microcontroller requirements/specs.
     */
    private final int PWM_FREQ = 1000;

    private AbcvlibSensors abcvlibSensors;
    private AbcvlibMotion abcvlibMotion;

    private final int MIN_PULSE_WIDTH = 0;
    private final int MAX_PULSE_WIDTH = PWM_FREQ;

    private boolean encoderARightWheelState;
    private boolean encoderBRightWheelState;
    private boolean encoderALeftWheelState;
    private boolean encoderBLeftWheelState;

    // The IN1 and IN2 IO determining Hubee Wheel direction.
    // See input1RightWheelController doc for control table
    private boolean input1RightWheelState;
    private boolean input2RightWheelState;
    private int pulseWidthRightWheelNew;
    private boolean input1LeftWheelState;
    private boolean input2LeftWheelState;
    private int pulseWidthLeftWheelNew;


    // Constructor to pass other module objects in.
    public AbcvlibLooper(AbcvlibSensors abcvlibSensors, AbcvlibMotion abcvlibMotion){
        this.abcvlibMotion = abcvlibMotion;
        this.abcvlibSensors = abcvlibSensors;
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

        // pulseWidths are given in microseconds
        int pulseWidthRightWheelOld = abcvlibMotion.getPwRight();
        int pulseWidthLeftWheelOld = abcvlibMotion.getPwLeft();

        boolean[] stateReturn;

        // Update IN1 and IN2 for each wheel based on sign of pulseWidth (+/-)
        stateReturn = setIn1In2(pulseWidthRightWheelOld);
        input1RightWheelState = stateReturn[0];
        input2RightWheelState = stateReturn[1];

        stateReturn = setIn1In2(pulseWidthLeftWheelOld);
        input1LeftWheelState = stateReturn[0];
        input2LeftWheelState = stateReturn[1];

        pulseWidthRightWheelNew = truncatePulseWidth(pulseWidthRightWheelOld);
        pulseWidthLeftWheelNew = truncatePulseWidth(pulseWidthLeftWheelOld);

        writeIoUpdates();

        readIoUpdates();

        encoderUpdates();

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

    /**
     *
     * Tests the sign of pulseWidth then determines how to set the input variables (IN1 and IN2)
     * to control the Hubee Wheel direction. See input1RightWheelController doc for control table.
     * If you wanted to inverse polarity, just reverse the > signs to < in each if statement.
     *
     * @param pulseWidth PWM pulse width of one wheel signal
     * @return input[0] --> IN1 and input[1] --> IN2
     */
    private boolean[] setIn1In2(Integer pulseWidth){

        boolean[] input = new boolean[2];

        if(pulseWidth >= MIN_PULSE_WIDTH){
            input[0] = false;
            input[1] = true;
        }else{
            input[0] = true;
            input[1] = false;
        }

        return input;

    }

    private int truncatePulseWidth(Integer pulseWidthOld){
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
    }

    private void readIoUpdates() throws ConnectionLostException{

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

    private void encoderUpdates() {

        int encoderCountRightWheelOld = abcvlibSensors.getWheelCountR();
        int encoderCountLeftWheelOld = abcvlibSensors.getWheelCountL();

        // Right is negative and left is positive since the wheels are physically mirrored so
        // while moving forward one wheel is moving ccw while the other is rotating cw.
        int encoderCountRightWheelNew = encoderCountRightWheelOld -
                encoderAddSubtractCount(input1RightWheelState, input2RightWheelState,
                        pulseWidthRightWheelNew, pulseWidthLeftWheelNew, encoderARightWheelState,
                        encoderBRightWheelState, encoderARightWheelStatePrevious,
                        encoderBRightWheelStatePrevious);

        int encoderCountLeftWheelNew = encoderCountLeftWheelOld +
                encoderAddSubtractCount(input1LeftWheelState, input2LeftWheelState,
                        pulseWidthRightWheelNew, pulseWidthLeftWheelNew, encoderALeftWheelState,
                        encoderBLeftWheelState, encoderALeftWheelStatePrevious,
                        encoderBLeftWheelStatePrevious);

        abcvlibSensors.setWheelR(encoderCountRightWheelNew);
        abcvlibSensors.setWheelL(encoderCountLeftWheelNew);

        encoderARightWheelStatePrevious = encoderARightWheelState;
        encoderBRightWheelStatePrevious = encoderBRightWheelState;
        encoderALeftWheelStatePrevious = encoderALeftWheelState;
        encoderBLeftWheelStatePrevious = encoderBLeftWheelState;
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
                    Log.w("Abcvlib", "Quadrature encoders read H/H or L/L when they should have read H/L or L/H");
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
                    Log.w("Abcvlib", "Quadrature encoders read H/L or L/H when they should have read H/H or L/L");
                }
            }
            // Previous Encoder A LOW, B LOW
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
                    Log.w("Abcvlib", "Quadrature encoders read H/H or L/L when they should have read H/L or L/H");
                }
            }
            // Previous Encoder A HIGH, B LOW.
            // You could make this into an else statement rather than else if to remove the compiler
            // warning, but I think it makes the readability better like this.
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
                    Log.w("Abcvlib", "Quadrature encoders read H/L or L/H when they should have read H/H or L/L");
                }
            }
        }

        return wheelCounts;
    }

    private void sendToLogPwm(Integer pulseWidthRightWheelOld, Integer pulseWidthRightWheelOld) {


        pulseWidthRightWheelOld;
        pulseWidthRightWheelOld;
        pulseWidthRightWheelNew
        pulseWidthLeftWheelNew;

        // Compile raw acceleration data to push to adb log
        String rawAccelerationMsg = Float.toString(accelerationX) + " " +
                Float.toString(accelerationY) + " " +
                Float.toString(accelerationZ);

        Log.i("rawAccelerationMsg", rawAccelerationMsg);

    }

    private void sendToLogEncoder(Integer encoderCountRightWheelNew, Integer encoderCountLeftWheelNew) {

        encoderARightWheelState;
        encoderBRightWheelState;
        encoderALeftWheelState;
        encoderBLeftWheelState;
        pulseWidthRightWheelNew;
        pulseWidthLeftWheelNew;
        encoderCountRightWheelNew;
        encoderCountLeftWheelNew;

        // Compile raw acceleration data to push to adb log
        String rawAccelerationMsg = Float.toString(accelerationX) + " " +
                Float.toString(accelerationY) + " " +
                Float.toString(accelerationZ);

        Log.i("rawAccelerationMsg", rawAccelerationMsg);

    }

}
