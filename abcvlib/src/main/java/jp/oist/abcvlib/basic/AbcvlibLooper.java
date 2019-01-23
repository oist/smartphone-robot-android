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
    public void loop() throws ConnectionLostException {

        // The IN1 and IN2 IO determining Hubee Wheel direction.
        // See input1RightWheelController doc for control table
        boolean input1RightWheelState;
        boolean input2RightWheelState;
        boolean input1LeftWheelState;
        boolean input2LeftWheelState;

        final int MIN_PULSE_WIDTH = 0;
        final int MAX_PULSE_WIDTH = PWM_FREQ;

        // pulseWidths are given in microseconds
        int pulseWidthRightWheel;
        int pulseWidthLeftWheel;

        /*
        The logical tests for whether the pulseWidthRightWheel is positive or negative determines
        how to set the input variables to control the Hubee Wheel direction. See
        input1RightWheelController doc for control table. If you wanted to inverse polarity, just
        reverse the > signs to < in each if statement.
        */
        if(abcvlibMotion.pulseWidthRightWheel >= MIN_PULSE_WIDTH){
            input1RightWheelState=false;
            input2RightWheelState=true;
        }else{
            input1RightWheelState=true;
            input2RightWheelState=false;
        }

        if(abcvlibMotion.pulseWidthLeftWheel >= MIN_PULSE_WIDTH){
            input1LeftWheelState=true;
            input2LeftWheelState=false;
        }else{
            input1LeftWheelState=false;
            input2LeftWheelState=true;
        }

        /*
        The following two logical statements simply hard limit the pulseWidth to be less than
        MAX_PULSE_WIDTH which represents the highest value it can be.
         */
        if(abcvlibMotion.pulseWidthRightWheel < MAX_PULSE_WIDTH){
            pulseWidthRightWheel = Math.abs(abcvlibMotion.pulseWidthRightWheel);
        }else{
            pulseWidthRightWheel = MAX_PULSE_WIDTH;
        }

        if(abcvlibMotion.pulseWidthLeftWheel < MAX_PULSE_WIDTH){
            pulseWidthLeftWheel = Math.abs(abcvlibMotion.pulseWidthLeftWheel);
        }else{
            pulseWidthLeftWheel = MAX_PULSE_WIDTH;
        }

        try {
            // Write all calculated values to the IOIO Board pins
            input1RightWheelController.write(input1RightWheelState);
            input2RightWheelController.write(input2RightWheelState);
            pwmControllerRightWheel.setPulseWidth(pulseWidthRightWheel);
            input1LeftWheelController.write(input1LeftWheelState);
            input2LeftWheelController.write(input2LeftWheelState);
            pwmControllerLeftWheel.setPulseWidth(pulseWidthLeftWheel);

            // Read all encoder values from IOIO Board
            boolean encoderARightWheelState = encoderARightWheel.read();
            boolean encoderBRightWheelState = encoderBRightWheel.read();
            boolean encoderALeftWheelState = encoderALeftWheel.read();
            boolean encoderBLeftWheelState = encoderBLeftWheel.read();

            setEncoderStates(input1RightWheelState, input2RightWheelState, input1LeftWheelState,
                    input2LeftWheelState, encoderARightWheelState, encoderALeftWheelState,
                    encoderBRightWheelState, encoderBLeftWheelState);

            encoderARightWheelStatePrevious = encoderARightWheelState;
            encoderBRightWheelStatePrevious = encoderBRightWheelState;
            encoderALeftWheelStatePrevious = encoderALeftWheelState;
            encoderBLeftWheelStatePrevious = encoderBLeftWheelState;

            IOIOConnectionManager.Thread.sleep(1);
        // Intentional empty catch block?
        } catch (InterruptedException e) {
            Log.i("abcvlib", "AbcvlibLooper.loop threw an InteruptedException");
        } catch (ConnectionLostException e){
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
     * Sets wheel counts for each wheel based on current turning direction of wheel as determined
     * , while encoder values de
     * @param input1RightWheelState IN1 of right Hubee Wheel
     * @param input2RightWheelState IN2 of right Hubee Wheel
     * @param input1LeftWheelState IN1 of left Hubee Wheel
     * @param input2LeftWheelState IN1 of left Hubee Wheel
     * @param encoderARightWheelState
     * @param encoderALeftWheelState
     * @param encoderBRightWheelState
     * @param encoderBLeftWheelState
     */
    private void setEncoderStates(Boolean input1RightWheelState, Boolean input2RightWheelState,
                                 Boolean input1LeftWheelState, Boolean input2LeftWheelState,
                                 Boolean encoderARightWheelState, Boolean encoderALeftWheelState,
                                 Boolean encoderBRightWheelState, Boolean encoderBLeftWheelState){

        int encoderCountRightWheel = abcvlibSensors.getWheelCountR();
        int encoderCountLeftWheel = abcvlibSensors.getWheelCountL();

        // Right is negative and left is positive since the wheels are physically mirrored so
        // while moving forward one wheel is moving ccw while the other is rotating cw.
        int newCountR = encoderCountRightWheel - abcvlibSensors.encoder(input1RightWheelState,
                input2RightWheelState, encoderARightWheelState, encoderBRightWheelState,
                encoderARightWheelStatePrevious, encoderBRightWheelStatePrevious);

        int newCountL = encoderCountLeftWheel + abcvlibSensors.encoder(input1LeftWheelState,
                input2LeftWheelState, encoderALeftWheelState, encoderBLeftWheelState,
                encoderALeftWheelStatePrevious, encoderBLeftWheelStatePrevious);

        abcvlibSensors.setWheelR(newCountR);
        abcvlibSensors.setWheelL(newCountL);
    }
}
