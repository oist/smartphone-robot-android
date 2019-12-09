package jp.oist.abcvlib.setpathbalancer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Mat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.AbcvlibActivity;
import jp.oist.abcvlib.inputs.Inputs;
import jp.oist.abcvlib.outputs.SocketClient;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    private static HashMap<String, Boolean> switches;
    static {
        switches = new HashMap<>();
        /*
         * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
         * memory/disk space on the phone and may result in memory failure if run for a long time
         * such as any learning tasks.
         */
        switches.put("loggerOn", false);
        /*
         * Enable/disable this to swap the polarity of the wheels such that the default forward
         * direction will be swapped (i.e. wheels will move cw vs ccw as forward).
         */
        switches.put("wheelPolaritySwap", false);
        /*
         * Tell initilizer to set up the PID controlled balancer
         */
        switches.put("balanceApp", true);
        /*
        Control various things from python interface
         */
        switches.put("pythonControlApp", true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        initialzer("192.168.28.151", 65434, switches);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(jp.oist.abcvlib.setpathbalancer.R.layout.activity_main);

        // Linear Back and Forth every 10 mm
//        setPath setPathThread = new setPath();
//        new Thread(setPathThread).start();

    }

    public class setPath implements Runnable{

        double actualSpeed = 0;
        double setSpeed = 10; // mm/s.
        double error_speed = 0; // difference between actual and setSpeed;
        double controllerOutput = 0;
        int totalOutput = 0;
        double d_s = 0; // derivative controller for speed of wheels

        public void run(){

            while(true) {

                actualSpeed = inputs.quadEncoders.getWheelSpeedL();
                error_speed = setSpeed - actualSpeed;
                controllerOutput = error_speed * d_s;
                totalOutput = (int) Math.round(outputs.balancePIDController.getOutput() + controllerOutput);
                Log.i(TAG, "controllerOut:" + controllerOutput + " balancerOut:" + outputs.balancePIDController.getOutput() + "total:" + totalOutput);
                outputs.motion.setWheelOutput(totalOutput, totalOutput);

            }
        }
    }
}
