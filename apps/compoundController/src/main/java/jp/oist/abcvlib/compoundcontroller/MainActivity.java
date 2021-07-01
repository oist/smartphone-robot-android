package jp.oist.abcvlib.compoundcontroller;

import android.os.Bundle;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataListener;
import jp.oist.abcvlib.core.outputs.AbcvlibController;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * Shows how to setup custom controller in conjunction with the the PID balance controller.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements IOReadyListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Various switches are available to turn on/off core functionality.
        getSwitches().balanceApp = true;

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onIOReady() {
        CustomController customController = new CustomController();

        // connect the customConroller as a subscriber to wheel data updates
        getInputs().getWheelData().setWheelDataListener(customController);

        // Add the custom controller to the grand controller (controller that assembles other controllers)
        getOutputs().getMasterController().addController(customController);

        new Thread(null, customController, "CustomController").start();
    }

    /**
     * Simple proportional controller trying to achieve some setSpeed set by python server GUI.
     */
    public static class CustomController extends AbcvlibController implements WheelDataListener {

        double actualSpeed = 0;
        double errorSpeed = 0;

        double setSpeed = 0; // mm/s.
        double d_s = 0; // derivative controller for speed of wheels

        public void run(){
            errorSpeed = setSpeed - actualSpeed;

            // Note the use of the same output for controlling both wheels. Due to various errors
            // that build up over time, controling individual wheels has so far led to chaos
            // and unstable controllers.
            setOutput((float) (errorSpeed * d_s), (float) (errorSpeed * d_s));
        }

        @Override
        public void onWheelDataUpdate(long timestamp, int wheelCountL, int wheelCountR, double wheelDistanceL, double wheelDistanceR, double wheelSpeedInstantL, double wheelSpeedInstantR, double wheelSpeedBufferedL, double wheelSpeedBufferedR, double wheelSpeedExpAvgL, double wheelSpeedExpAvgR) {
            actualSpeed = wheelSpeedBufferedL;
        }
    }
}
