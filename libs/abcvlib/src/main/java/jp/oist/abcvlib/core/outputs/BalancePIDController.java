package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import java.util.Arrays;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.Switches;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataListener;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
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
    private double e_t = 0; // e(t) of wikipedia
    private double int_e_t; // integral of e(t) from wikipedia. Discrete, so just a sum here.

    private double maxAbsTilt = 6.5; // in Degrees

    private final long[] PIDTimer = new long[3];
    private final long[] PIDTimeSteps = new long[4];
    private int timerCount = 1;
    private long lastUpdateTime;

    private double speedL;
    private double speedR;
    private double thetaDeg;
    private double angularVelocityDeg;

    private final boolean socketLock = false;

    private final Switches switches;

    private int bounceLoopCount = 0;
    private long timestamp;

    public BalancePIDController(Switches switches, Inputs inputs){
        this.switches = switches;
        inputs.getOrientationData().setOrientationDataListener(this);
        inputs.getWheelData().setWheelDataListener(this);
        Log.i("abcvlib", "BalanceApp Created");
    }

    public void run(){

        PIDTimer[0] = System.nanoTime();
        // in Degrees
        double maxTiltAngle = setPoint + maxAbsTilt;
        // in Degrees
        double minTiltAngle = setPoint - maxAbsTilt;

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
        int avgCount = 1000;
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
        } catch (NullPointerException e){
            Log.e(TAG,"Error", e);
            Thread.sleep(1000);
        }
    }

    private void bounce(boolean forward) {
        int speed = 1;
        // loop steps between turning on and off wheels.
        int bouncePulseWidth = 100;
        if (bounceLoopCount < bouncePulseWidth * 0.1){
            setOutput(0,0);
        }else if (bounceLoopCount < bouncePulseWidth * 1.1){
            if (forward){
                setOutput(speed,-speed);
            }else{
                setOutput(-speed,speed);
            }
        }else if (bounceLoopCount < bouncePulseWidth * 1.2){
            setOutput(0,0);
        }else if (bounceLoopCount < bouncePulseWidth * 2.2) {
            if (forward){
                setOutput(-speed,speed);
            }else{
                setOutput(speed,-speed);
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
        // error betweeen actual and desired wheel speed (default 0)
        double e_w = 0.0 - speedL;
        Log.v(TAG, "speedL:" + speedL);

        double p_out = (p_tilt * e_t) + (p_wheel * e_w);
        double i_out = i_tilt * int_e_t;
        double d_out = d_tilt * angularVelocityDeg;

        setOutput((float)-(p_out + i_out + d_out), (float)(p_out + i_out + d_out));

        Output testOutput = getOutput();
    }

    public void setAngularVelocityDeg(double angularVelocityDeg) {
        this.angularVelocityDeg = angularVelocityDeg;
    }


    @Override
    public void onWheelDataUpdate(long timestamp, int wheelCountL, int wheelCountR,
                                  double wheelDistanceL, double wheelDistanceR,
                                  double wheelSpeedL, double wheelSpeedR) {
        this.timestamp = timestamp;
        speedL = wheelSpeedL;
        speedR = wheelSpeedR;
//        wheelData.setExpWeight(expWeight); // todo enable access to this in GUI somehow
    }

    @Override
    public void onOrientationUpdate(long timestamp, double thetaRad, double angularVelocityRad) {
        thetaDeg = OrientationData.getThetaDeg(thetaRad);
        angularVelocityDeg = OrientationData.getAngularVelocityDeg(angularVelocityRad);
    }
}
