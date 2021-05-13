package jp.oist.abcvlib.core.outputs;


import android.util.Log;

import java.util.List;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.QuadEncoders;
import jp.oist.abcvlib.core.inputs.MotionSensors;

/**
 * Motion is a collection of methods that implement various predefined motions via
 * controlling the values of dutyCycleRightWheel and pulseWidthLeftWhee. These variables
 * indirectly control the speed of the wheel by adjusting the pulseWidth of the PWM signal sent
 * to each wheel.
 *
 * Motion does not run on its own anywhere within the core library. In order to use any
 * of the methods within this class, they must be directly called from the Android App
 * MainActivity. In order to do this, an object instance of the class must be created.
 *
 * k_p is a proportional controller parameter
 * k_d1 and k_d2 are derivative controller parameters
 *
 * Most motions first go through a logical condition determining if the tilt angle is within
 * minTiltAngle and maxTiltAngle to determine whether to use a linear PD controller or the
 * Central Pattern Generator destabalizer cpgUpdate(). More on this within Judy's paper here:
 * https://www.frontiersin.org/articles/10.3389/fnbot.2017.00001/full
 *
 * @author Jiexin Wang https://github.com/ha5ha6
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class Motion {

    /**
    Represents some int value from 0 to 100 which indirectly controls the speed of the wheel.
     0 representing a 0% duty cycle (i.e. always zero) and 100 representing a 100% duty cycle.
    */
    private int dutyCycleRightWheel = 0;
    /**
     * @see #dutyCycleRightWheel
     */
    private int dutyCycleLeftWheel = 0;

    private MotionSensors motionSensors;
    private QuadEncoders quadEncoders;
    private AbcvlibActivity abcvlibActivity;

    /**
     * Constructor to pass other module objects in. Keep public.
      */
    public Motion(AbcvlibActivity abcvlibActivity){
        this.abcvlibActivity = abcvlibActivity;
        this.motionSensors = abcvlibActivity.inputs.motionSensors;
        this.quadEncoders = abcvlibActivity.inputs.quadEncoders;
    }

    /**
     * Sets the pulse width for each wheel. This directly correlates with the speed of the wheel.
     * This is the only method in here that doesn't require a separate thread, since it does not
     * need to be updated so long as you just want to drive the wheels at a constant speed
     * indefinitely. Therefore it makes a good example test case since its the easiest to implement.
     * @param right pulse width of right wheel from 0 to 100
     * @param left pulse width of left wheel from 0 to 100
     */
    public void setWheelOutput(int left, int right){

        if (right > 100){
            right = 100;
        }
        else if (right < -100){
            right = -100;
        }
        if (left > 100){
            left = 100;
        }
        else if (left < -100){
            left = -100;
        }

        if (abcvlibActivity.switches.wheelPolaritySwap){
            dutyCycleRightWheel = -right;
            // Wheels must be opposite polarity to turn in same direction
            dutyCycleLeftWheel = left;
        }
        else {
            dutyCycleRightWheel = right;
            // Wheels must be opposite polarity to turn in same direction
            dutyCycleLeftWheel = -left;
        }
    }

    /**
     * @return Pulse Width of right wheel
     */
    public int getDutyCycleRight(){
        return dutyCycleRightWheel;
    }

    /**
     * @return Pulse Width of left wheel
     */
    public int getDutyCycleLeft(){
        return dutyCycleLeftWheel;
    }
}
