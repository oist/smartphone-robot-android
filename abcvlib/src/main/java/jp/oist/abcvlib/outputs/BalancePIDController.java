package jp.oist.abcvlib.outputs;

import android.util.Log;

import org.json.JSONException;

import java.util.Arrays;

import jp.oist.abcvlib.AbcvlibActivity;

public class BalancePIDController extends AbcvlibController{

    // Initialize all sensor reading variables
    double p_tilt = 0;
    double i_tilt = 0;
    double d_tilt = 0;
    double setPoint = 0;
    double p_wheel = 0;

    // BalancePIDController Setup
    double thetaDeg; // tilt of phone with vertical being 0.
    double thetaDegDot; // derivative of tilt (angular velocity)
    double wheelCountL; // encoder count on left wheel
    double wheelCountR; // encoder count on right wheel
    double distanceL; // distances traveled by left wheel from start point (mm)
    double distanceR; // distances traveled by right wheel from start point (mm)
    double speedL; // Current speed on left wheel in mm/s
    double speedR; // Current speed on right wheel in mm/s

    double e_t = 0; // e(t) of wikipedia
    double e_w = 0; // error betweeen actual and desired wheel speed (default 0)

    double int_e_t; // integral of e(t) from wikipedia. Discrete, so just a sum here.

    double maxAbsTilt = 30; // in Degrees
    double maxTiltAngle = setPoint + maxAbsTilt; // in Degrees
    double minTiltAngle = setPoint - maxAbsTilt; // in Degrees

    long[] PIDTimer = new long[3];
    long[] PIDTimeSteps = new long[4];
    int timerCount = 1;
    double thetaDiff;
    long lastUpdateTime;
    long updateTimeStep;

    private AbcvlibActivity abcvlibActivity;

    private int avgCount = 1000;

    public BalancePIDController(AbcvlibActivity abcvlibActivity){

        this.abcvlibActivity = abcvlibActivity;
        Log.d("abcvlib", "BalanceApp Created");


    }

    public void run(){

        while(abcvlibActivity.balanceApp) {

//            Log.v("abcvlib", "In balanceApp.run");

            PIDTimer[0] = System.nanoTime();

            thetaDiff = thetaDeg - abcvlibActivity.inputs.motionSensors.getThetaDeg();

            if (thetaDiff !=0){
                updateTimeStep = PIDTimer[0]- lastUpdateTime;
                lastUpdateTime = PIDTimer[0];
                if (abcvlibActivity.loggerOn){
                    Log.i("theta", "theta was updated in " + (updateTimeStep / 1000000) + " ms");
                }
            }

            thetaDeg = abcvlibActivity.inputs.motionSensors.getThetaDeg();
            thetaDegDot = abcvlibActivity.inputs.motionSensors.getThetaDegDot();
            wheelCountL = abcvlibActivity.inputs.quadEncoders.getWheelCountL();
            wheelCountL = abcvlibActivity.inputs.quadEncoders.getWheelCountR();
            distanceL = abcvlibActivity.inputs.quadEncoders.getDistanceL();
            distanceR = abcvlibActivity.inputs.quadEncoders.getDistanceR();
            speedL = abcvlibActivity.inputs.quadEncoders.getWheelSpeedL();
            speedR = abcvlibActivity.inputs.quadEncoders.getWheelSpeedR();
            maxTiltAngle = setPoint + maxAbsTilt;
            minTiltAngle = setPoint - maxAbsTilt;

            PIDTimer[1] = System.nanoTime();

            try {
                linearController();
            } catch (InterruptedException e) {
                e.printStackTrace();
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

                if (abcvlibActivity.loggerOn){
                    Log.i("timers", "PIDTimer Averages = " + Arrays.toString(PIDTimeSteps));
                }
            }

            timerCount ++;
        }
    }

    private void linearController() throws InterruptedException {

        try {
            if (abcvlibActivity.outputs.socketClient.socketMsgIn != null){

                try {
                    setPoint = Double.parseDouble(abcvlibActivity.outputs.socketClient.socketMsgIn.get("setPoint").toString());
                    p_tilt = Double.parseDouble(abcvlibActivity.outputs.socketClient.socketMsgIn.get("p_tilt").toString());
                    i_tilt = Double.parseDouble(abcvlibActivity.outputs.socketClient.socketMsgIn.get("i_tilt").toString());
                    d_tilt = Double.parseDouble(abcvlibActivity.outputs.socketClient.socketMsgIn.get("d_tilt").toString());
                    p_wheel = Double.parseDouble(abcvlibActivity.outputs.socketClient.socketMsgIn.get("p_wheel").toString());

//                Log.v("abcvlib", "linearConroller updated values from socketClient");

                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        } catch (NullPointerException e){
            e.printStackTrace();
            Thread.sleep(1000);
        }

        // TODO this needs to account for length of time on each interval, or overall time length. Here this just assumes a width of 1 for all intervals.
        int_e_t = int_e_t + e_t;
        e_t = setPoint - thetaDeg;
        e_w = 0.0 - speedL;

        double p_out = (p_tilt * e_t) + (p_wheel * e_w);
        double i_out = i_tilt * int_e_t;
        double d_out = d_tilt * thetaDegDot;

//        Log.v("abcvlib", "balanceController out:" + (p_out + i_out + d_out));

        setOutput((p_out + i_out + d_out), (p_out + i_out + d_out));

        Output testOutput = getOutput();

//        Log.v("abcvlib", this.toString() + testOutput.left);


    }

}
