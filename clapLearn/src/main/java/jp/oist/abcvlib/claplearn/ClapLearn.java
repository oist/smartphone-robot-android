package jp.oist.abcvlib.claplearn;

import android.os.Bundle;

import jp.oist.abcvlib.AbcvlibActivity;

import android.widget.TextView;

import java.util.HashMap;


/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs BalancePIDController controller locally on Android, but takes BalancePIDController parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class ClapLearn extends AbcvlibActivity {

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
        switches.put("wheelPolaritySwap", true);
        /*
         * Tell initilizer to set up the PID controlled balancer
         */
        switches.put("balance", true);
        /*
         * Does the app use the camera as an input?
         */
        switches.put("cameraApp", false);
        /*
         * Controller to center blob when tracked. Can be used on top of balance and results will be
         * additive
         */
        switches.put("centerBlob", false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes states up to Android Activity. Do not modify
        super.onCreate(savedInstanceState);
        // Passes Android App information up to parent classes for various usages.
        initialzer("192.168.28.151", 65434, switches);
        // Read the layout and construct.
        setContentView(R.layout.main);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     *  This method gets called by the micInput object owned by this activity.
     *  It first computes the RMS value and then it sets up a bit of
     *  code/closure that runs on the UI thread that does the actual drawing.
     */

    public void setAudioFile(){
        // Set audiofile up here
    }

    public void setWheelOutput(double left, double right){
        // Set wheeloutput here
    }

    public void setPID(){
        // Set PID params here
    }

}
