package jp.oist.abcvlibapp;

import android.os.Bundle;

import jp.oist.abcvlib.basic.AbcvlibActivity;

/**
 * Most basic Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    /**
     * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
     * memory/disk space on the phone and may result in memory failure if run for a long time
     * such as any learning tasks.
     */
    private boolean loggerOn = true;
    /**
     * Enable/disable this to swap the polarity of the wheels such that the default forward
     * direction will be swapped (i.e. wheels will move cw vs ccw as forward).
     */
    private boolean wheelPolaritySwap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
        super.loggerOn = loggerOn;
        super.wheelPolaritySwap = wheelPolaritySwap;

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        movement movementThread = new movement();
        new Thread(movementThread).start();

    }

    /**
     * Edit the contents of this method to customize the desired robot movement. Make sure only
     * one method from abcvlibMotion is used at a time. They will conflict otherwise.
     */
    public class movement implements Runnable{

        // PID Setup
        float thetaDeg;
        float thetaDegDot;

        int output; //  u(t) of wikipedia
        float setPoint = -2.5f; // SP of wikipedia
        float e_t = 0; // e(t) of wikipedia
        float int_e_t; // integral of e(t) from wikipedia. Discrete, so just a sum here.

        float k_p = 300;
//        float k_i = 0.0003f;
        float k_i = 0;
//        float k_d = -f;
        float k_d = 0;

        float maxTiltAngle = 5f;
        float minTiltAngle = -8f;

        private int stuckCount = 0;

        public void run(){

            while(true) {

                /*
                Option 1: This simply directly sets a static value of 500 as the pulse width on
                each wheel
                 */
//                MainActivity.super.abcvlibMotion.setWheelSpeed(-500,500);

                /*
                Option 2: This attempts to set the tilt angle to zero via a simple PID controller
                */
                PIDLoop();

            }
        }

        private void PIDLoop(){

            thetaDeg = abcvlibSensors.getThetaDeg();
            thetaDegDot = abcvlibSensors.getThetaDegDot();

            // if tilt angle is within minTiltAngle and maxTiltAngle, use PD controller, else use bouncing non-linear controller
            if(thetaDeg < maxTiltAngle && thetaDeg > minTiltAngle){

                linearController();
                stuckCount = 0;
            }
            else if(stuckCount < 500000){
                linearController();
                stuckCount = stuckCount + 1;
            }
            else{
                if (output == 1000){
                    output = -1000;
                }
                else{
                    output = 1000;
                }
                }

            abcvlibMotion.setWheelSpeed(output, output);
        }

        private void linearController(){
            int_e_t = int_e_t + e_t;
            e_t = setPoint - thetaDeg;

            float p_out = k_p * e_t;
            float i_out = k_i * int_e_t;
            float d_out = k_d * thetaDegDot;

            output = Math.round(p_out + i_out + d_out);
        }
    }
}
