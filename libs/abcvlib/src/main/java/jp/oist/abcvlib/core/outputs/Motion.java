package jp.oist.abcvlib.core.outputs;


import jp.oist.abcvlib.core.Switches;

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
    private double dutyCycleRightWheel = 0;
    /**
     * @see #dutyCycleRightWheel
     */
    private double dutyCycleLeftWheel = 0;
    
    private Switches switches;

    /**
     * Constructor to pass other module objects in. Keep public.
      */
    public Motion(Switches switches){
        this.switches = switches;
    }

    /**
     * Sets the pulse width for each wheel. This directly correlates with the speed of the wheel.
     * This is the only method in here that doesn't require a separate thread, since it does not
     * need to be updated so long as you just want to drive the wheels at a constant speed
     * indefinitely. Therefore it makes a good example test case since its the easiest to implement.
     * @param right pulse width of right wheel from 0 to 100
     * @param left pulse width of left wheel from 0 to 100
     */
    public void setWheelOutput(double left, double right){

        if (right > 1){
            right = 1;
        }
        else if (right < -1){
            right = -1;
        }
        if (left > 1){
            left = 1;
        }
        else if (left < -1){
            left = -1;
        }

        if (switches.wheelPolaritySwap){
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
    public double getDutyCycleRight(){
        return dutyCycleRightWheel;
    }

    /**
     * @return Pulse Width of left wheel
     */
    public double getDutyCycleLeft(){
        return dutyCycleLeftWheel;
    }
}
