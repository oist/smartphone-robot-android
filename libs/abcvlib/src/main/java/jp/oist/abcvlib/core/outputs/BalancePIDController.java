package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import java.util.Arrays;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.Switches;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataListener;
import jp.oist.abcvlib.core.inputs.phone.OrientationDataListener;
import jp.oist.abcvlib.util.ErrorHandler;

public class BalancePIDController extends AbcvlibController implements WheelDataListener, OrientationDataListener {

    private final String TAG = this.getClass().getName();

    // Initialize all sensor reading variables
    private double p_tilt = -24;
    private double i_tilt = 0;
    private double d_tilt = 1.0;
    private double setPoint = 2.8;
    private double p_wheel = 0.4;
    private double expWeight = 0.25;

    // BalancePIDController Setup
    private double thetaDeg; // tilt of phone with vertical being 0.
    private double angularVelocityDeg; // derivative of tilt (angular velocity)
    private int wheelCountL; // encoder count on left wheel
    private int wheelCountR; // encoder count on right wheel
    private double distanceL; // distances traveled by left wheel from start point (mm)
    private double distanceR; // distances traveled by right wheel from start point (mm)
    private double speedL; // Current speed on left wheel in mm/s
    private double speedR; // Current speed on right wheel in mm/s

    private double e_t = 0; // e(t) of wikipedia
    private double e_w = 0; // error betweeen actual and desired wheel speed (default 0)

    private double int_e_t; // integral of e(t) from wikipedia. Discrete, so just a sum here.

    private double maxAbsTilt = 6.5; // in Degrees
    private double maxTiltAngle = setPoint + maxAbsTilt; // in Degrees
    private double minTiltAngle = setPoint - maxAbsTilt; // in Degrees

    private long[] PIDTimer = new long[3];
    private long[] PIDTimeSteps = new long[4];
    private int timerCount = 1;
    private double thetaDiff;
    private long lastUpdateTime;
    private long updateTimeStep;

    private boolean socketLock = false;

    private Switches switches;

    private int avgCount = 1000;

    private int bounceLoopCount = 0;
    // loop steps between turning on and off wheels.
    private int bouncePulseWidth = 100;

    public BalancePIDController(Switches switches){
        this.switches = switches;
        Log.i("abcvlib", "BalanceApp Created");
    }

    public void run(){

        PIDTimer[0] = System.nanoTime();

        thetaDiff = thetaDeg - getThetaDeg();

        if (thetaDiff !=0){
            updateTimeStep = PIDTimer[0]- lastUpdateTime;
            lastUpdateTime = PIDTimer[0];
            if (switches.loggerOn){
                Log.v("theta", "theta was updated in " + (updateTimeStep / 1000000) + " ms");
            }
        }

        thetaDeg = getThetaDeg();
        angularVelocityDeg = getAngularVelocityDeg();
        double distanceLPrev = distanceL;
        double distanceRPrev = distanceR;
        wheelCountL = getWheelCountL();
        wheelCountR = getWheelCountR();
        distanceL = WheelData.countsToDistance(wheelCountL);
        distanceR = WheelData.countsToDistance(wheelCountR);
        speedL = getSpeedL();
        speedR = getSpeedR();
        maxTiltAngle = setPoint + maxAbsTilt;
        minTiltAngle = setPoint - maxAbsTilt;

        PIDTimer[1] = System.nanoTime();

        // Bounce Up
        if (minTiltAngle > thetaDeg){
            bounce(false);
        }else if(maxTiltAngle < thetaDeg){
            bounce(true);
        }else{
            try {
                bounceLoopCount = 0;
                linearController();
            } catch (InterruptedException e) {
                ErrorHandler.eLog(TAG, "Interupted when trying to run linearController", e, true);
            }
        }

        PIDTimer[2] = System.nanoTime();

        PIDTimeSteps[3] += System.nanoTime() - PIDTimer[2];
        PIDTimeSteps[2] += PIDTimer[2] - PIDTimer[1];
        PIDTimeSteps[1] += PIDTimer[1] - PIDTimer[0];
        PIDTimeSteps[0] = PIDTimer[0];

        // Take basic stats of every 1000 time step lengths rather than pushing all.
        if (timerCount % avgCount == 0){

            for (int i=1; i < PIDTimeSteps.length; i++){

                PIDTimeSteps[i] = (PIDTimeSteps[i] / avgCount) / 1000000;

            }

            if (switches.loggerOn){
                Log.v("timers", "PIDTimer Averages = " + Arrays.toString(PIDTimeSteps));
            }
        }

        timerCount ++;
    }

    synchronized public void setPID(double p_tilt_,
                                    double i_tilt_,
                                    double d_tilt_,
                                    double setPoint_,
                                    double p_wheel_,
                                    double expWeight_,
                                    double maxAbsTilt_)
            throws InterruptedException {

        try {
                setPoint = setPoint_;
                p_tilt = p_tilt_;
                i_tilt = i_tilt_;
                d_tilt = d_tilt_;
                p_wheel = p_wheel_;
                expWeight = expWeight_;
                maxAbsTilt = maxAbsTilt_;
                Log.v("abcvlib", "linearConroller updated values from local");
        } catch (NullPointerException e){
            Log.e(TAG,"Error", e);
            Thread.sleep(1000);
        }
    }

    private void bounce(boolean forward) {
        int speed = 100;
        if (bounceLoopCount < bouncePulseWidth * 0.1){
            setOutput(0,0);
        }else if (bounceLoopCount < bouncePulseWidth * 1.1){
            if (forward){
                setOutput(speed,speed);
            }else{
                setOutput(-speed,-speed);
            }
        }else if (bounceLoopCount < bouncePulseWidth * 1.2){
            setOutput(0,0);
        }else if (bounceLoopCount < bouncePulseWidth * 2.2) {
            if (forward){
                setOutput(-speed,-speed);
            }else{
                setOutput(speed,speed);
            }
        }else {
            bounceLoopCount = 0;
        }
        bounceLoopCount++;
    }

    private void linearController() throws InterruptedException {

        setPID(p_tilt, i_tilt, d_tilt, setPoint, p_wheel, expWeight, maxAbsTilt);

        // TODO this needs to account for length of time on each interval, or overall time length. Here this just assumes a width of 1 for all intervals.
        int_e_t = int_e_t + e_t;
        e_t = setPoint - thetaDeg;
        e_w = 0.0 - speedL;

        double p_out = (p_tilt * e_t) + (p_wheel * e_w);
        double i_out = i_tilt * int_e_t;
        double d_out = d_tilt * angularVelocityDeg;

        setOutput((p_out + i_out + d_out), (p_out + i_out + d_out));

        Output testOutput = getOutput();
    }

    public synchronized int getWheelCountL() {
        return wheelCountL;
    }

    public synchronized int getWheelCountR() {
        return wheelCountR;
    }

    public synchronized double getThetaDeg() {
        return thetaDeg;
    }

    public synchronized double getAngularVelocityDeg() {
        return angularVelocityDeg;
    }

    public synchronized double getSpeedL() {
        return speedL;
    }

    public synchronized double getSpeedR() {
        return speedR;
    }

    public synchronized void setWheelCountL(int wheelCountL) {
        this.wheelCountL = wheelCountL;
    }

    public synchronized void setWheelCountR(int wheelCountR) {
        this.wheelCountR = wheelCountR;
    }

    public synchronized void setSpeedL(double speedL) {
        this.speedL = speedL;
    }

    public synchronized void setSpeedR(double speedR) {
        this.speedR = speedR;
    }

    @Override
    public void onWheelDataUpdate(long timestamp, int countLeft, int countRight, double speedL, double speedR) {
        setWheelCountL(countLeft);
        setWheelCountR(countRight);
        setSpeedL(speedL);
        setSpeedR(speedR);
    }

    @Override
    public void onOrientationUpdate(long timestamp, double thetaRad, double angularVelocityRad) {

    }
}
