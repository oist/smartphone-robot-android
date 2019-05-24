package jp.oist.abcvlib.basic;


/**
 * AbcvlibMotion is a collection of methods that implement various predefined motions via
 * controlling the values of pulseWidthRightWheel and pulseWidthLeftWhee. These variables
 * indirectly control the speed of the wheel by adjusting the pulseWidth of the PWM signal sent
 * to each wheel.
 *
 * AbcvlibMotion does not run on its own anywhere within the core library. In order to use any
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
public class AbcvlibMotion {

    /**
    Represents some int value from 0 to 1000 which indirectly controls the speed of the wheel.
     0 representing a 0 microsecond pulse width (i.e. always zero) and 1000 representing
     1000 microseconds, or 1 ms. As the PWM freq is set to 1000 Hz (1 pulse per 1/1000 seconds)
     a value of 1 ms for the pulseWidthRightWheel would result in an always high voltage (i.e.
    no pulses at all, just a flat DC high voltage).
    */
    private int pulseWidthRightWheel = 0;
    /**
     * @see #pulseWidthRightWheel
     */
    private int pulseWidthLeftWheel = 0;

    /**
     * Central Pattern Generator destabalizer initial parameters.
     */
    private double[] cpgXY = {900, 900};
    private float ddt = 0.005F; //TODO update to sensor sampling rate? Judy says no, but need to read more about the Runge Kutta methods she claims to have used.
    /**
     * Some bias constant to the proportional controller. Strange value kept for historical reference.
     */
    private double c_p = -7 + 67 * (0.1 + 0.004600000102072954);
    /**
     * Some bias constant to one of the derivative controllers. Past value kept for historical reference.
     */
    private float c_d1 = 1.737F;
    /**
     * Some bias constant to one of the derivative controllers. Past value kept for historical reference.
     */
    private float c_d2 = 0.4928F;
    /**
     * Maximum allowed tilt angle of phone in degrees. Angles above this will result in no motion.
     */
    private float maxTiltAngle = 20F;
    /**
     * Minimum allowed tilt angle of phone in degrees. Angles above this will result in no motion.
     */
    private float minTiltAngle = -20F;

    private AbcvlibSensors abcvlibSensors;
    private AbcvlibQuadEncoders abcvlibQuadEncoders;

    /**
     * PWM frequency. Do not modify locally. Modify at AbcvlibActivity level if necessary.
     */
    private int PWM_FREQ;

    /**
     * Constructor to pass other module objects in. Keep public.
      */
    public AbcvlibMotion(AbcvlibSensors abcvlibSensors, AbcvlibQuadEncoders abcvlibQuadEncoders, Integer PWM_FREQ){
        this.abcvlibSensors = abcvlibSensors;
        this.abcvlibQuadEncoders = abcvlibQuadEncoders;
        this.PWM_FREQ = PWM_FREQ;
    }

    /**
     * Sets the pulse width for each wheel. This directly correlates with the speed of the wheel.
     * This is the only method in here that doesn't require a separate thread, since it does not
     * need to be updated so long as you just want to drive the wheels at a constant speed
     * indefinitely. Therefore it makes a good example test case since its the easiest to implement.
     * @param right pulse width of right wheel from 0 to PWM_FREQ
     * @param left pulse width of left wheel from 0 to AbcvlibLooper.PWM_FREQ
     */
    public void setWheelSpeed(int left, int right){

        if (right > 1000){
            right = 1000;
        }
        else if (right < -1000){
            right = -1000;
        }
        if (left > 1000){
            left = 1000;
        }
        else if (left < -1000){
            left = -1000;
        }

        pulseWidthRightWheel = right;
        // Wheels must be opposite polarity to turn in same direction
        pulseWidthLeftWheel = -left;
    }

    /**
     * @return Pulse Width of right wheel
     */
    int getPwRight(){
        return pulseWidthRightWheel;
    }

    /**
     * @return Pulse Width of left wheel
     */
    int getPwLeft(){
        return pulseWidthLeftWheel;
    }
//
//    /**
//    Central Pattern Generator based destabalizer is applied when phone tilt angle is not within
//    minTiltAngle and maxTiltAngle. Apparently this method implements the Runge Kutta methods.
//    dt_sample should correlate with the dt_sample shown in the below mentioned wiki article.
//    https://en.wikipedia.org/wiki/Runge%E2%80%93Kutta_methods
//     */
//    public double[] cpgUpdate(double[] cpgXY, float dt_sample, double thetadot, float omega, float beta){
//        double a1, a2, a3, a4; // dummy variables
//        double aa1, aa2, aa3, aa4; // dummy variables
//        double[] x = new double[2];
//
//        a1 = getXDot(cpgXY[1], thetadot, omega, beta);
//        a2 = getXDot(cpgXY[1] + a1 * dt_sample / 2, thetadot, omega, beta);
//        a3 = getXDot(cpgXY[1] + a2 * dt_sample / 2, thetadot, omega, beta);
//        a4 = getXDot(cpgXY[1]+ a3 * dt_sample , thetadot, omega, beta);
//        x[0] = cpgXY[0] + (a1 + 2 * a2 + 2 * a3 + a4) * dt_sample / 6;
//
//        aa1 = getYDot(cpgXY[0], omega);
//        aa2 = getYDot(cpgXY[0] + aa1 * dt_sample / 2, omega);
//        aa3 = getYDot(cpgXY[0] + aa2 * dt_sample / 2, omega);
//        aa4 = getYDot(cpgXY[0] + aa3 * dt_sample, omega);
//        x[1] = cpgXY[1] + (aa1 + 2 * aa2 + 2 * aa3 + aa4) * dt_sample / 6;
//
//        return x;
//    }
//
//    /**
//     * Just a slave function of cpgUpdate
//     * @param y
//     * @param thetadot
//     * @param omega
//     * @param beta
//     * @return
//     */
//    private double getXDot(double y, double thetadot, double omega, double beta){
//        double xdot;
//        xdot = (omega * y + beta * thetadot);
//        return xdot;
//    }
//
//    /**
//     * Just a slave function of cpgUpdate
//     * @param x
//     * @param omega
//     * @return
//     */
//    private double getYDot(double x, double omega){
//        double ydot;
//        ydot = -omega*x;
//        return ydot;
//    }

//    public static void linearfeedback(int k_p, int k_d1, int k_d2){
//        k_d1 = k_d1 * 0.01F; // Just put these here for better readability
//        k_d2 = k_d2 * 0.01F; // Just put these here for better readability
//        pulseWidthRightWheel = (int) -((k_p * (abcvlibSensors.thetaDeg + c_p) + k_d1 * (abcvlibSensors.thetaDegDot + c_d1) + k_d2 * (abcvlibSensors.speedRightWheel + c_d2) + k_d2 * (abcvlibSensors.speedLeftWheel + c_d2)));
//        pulseWidthLeftWheel=pulseWidthRightWheel;
//    }

//    public static void cpg(int omega, int beta){
//        pulseWidthRightWheel = (int) -cpgXY[1];
//        pulseWidthLeftWheel = pulseWidthRightWheel;
//        cpgXY = cpgUpdate(cpgXY, ddt, abcvlibSensors.thetaDegDot, omega, beta);
//    }
//
//    public static void sine(int t, int f){
//        pulseWidthRightWheel = (int)(1000 * Math.sin(2 * Math.PI * f * t));
//        pulseWidthLeftWheel = pulseWidthRightWheel;
//    }
}
