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
import jp.oist.abcvlib.outputs.AbcvlibController;
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
        switches.put("balanceApp", false);
        /*
        Control various things from python interface
         */
        switches.put("pythonControlApp", true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        CustomController customController = new CustomController();

        initialzer("192.168.29.131", 65434, switches, customController);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(jp.oist.abcvlib.setpathbalancer.R.layout.activity_main);

        new Thread(customController).start();

    }

    public class CustomController extends AbcvlibController {

        ActualSpeed actualSpeed = new ActualSpeed();
        ErrorSpeed errorSpeed = new ErrorSpeed();

        double setSpeed = 0; // mm/s.
        double d_s = 0; // derivative controller for speed of wheels

        public void run(){

            while(appRunning) {

                actualSpeed.left = inputs.quadEncoders.getWheelSpeedL();
                actualSpeed.right = inputs.quadEncoders.getWheelSpeedR();
                Log.d(TAG, "actualSpeedLeft:" + actualSpeed.left);

                errorSpeed.left = setSpeed - actualSpeed.left;
                errorSpeed.right = setSpeed - actualSpeed.right;
                Log.d(TAG, "errorSpeed:" + errorSpeed.left);


                if (outputs.socketClient.socketMsgIn != null) {

                    try {
                        setSpeed = Double.parseDouble(outputs.socketClient.socketMsgIn.get("wheelSpeedL").toString());
                        Log.d(TAG, "setSpeed:" + setSpeed);
                        d_s = Double.parseDouble(outputs.socketClient.socketMsgIn.get("wheelSpeedControl").toString());
                        Log.d(TAG, "d_s:" + d_s);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                setOutput(errorSpeed.left * d_s, errorSpeed.right * d_s);
                Log.d(TAG, "customController out:" + (errorSpeed.left * d_s));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

        private class ActualSpeed{
            double left;
            double right;
        }

        private class ErrorSpeed{
            double left;
            double right;
        }

    }
}
