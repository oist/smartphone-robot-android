package jp.oist.abcvlib;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import jp.oist.abcvlib.learning.ActionDistribution;
import jp.oist.abcvlib.learning.ActionSelector;
import jp.oist.abcvlib.outputs.*;
import jp.oist.abcvlib.inputs.*;
import jp.oist.abcvlib.inputs.vision.*;

/**
 * AbcvlibActivity is where all of the other classes are initialized into objects. The objects
 * are then passed to one another in order to coordinate the various shared values between them.
 *
 * Android app MainActivity can start Motion by extending AbcvlibActivity and then running
 * any of the methods within the object instance Motion within an infinite threaded loop
 * e.g:
 *
 * @author Christopher Buckley https://github.com/topherbuckley
 *
 */
public abstract class AbcvlibActivity extends IOIOActivity {

    // Publically accessible objects that encapsulate a lot other core functionality
    public Inputs inputs;
    public Outputs outputs;
    public ActionDistribution aD;
    public ActionSelector aS;
    private Thread actionSelectorThread;

    // Default Booleans
    public boolean loggerOn = false;
    public boolean wheelPolaritySwap = false;
    public boolean motionSensorApp = true;
    public boolean quadEncoderApp = true;
    public boolean pythonControlApp = false;
    public boolean balanceApp = false;
    public boolean cameraApp = false;
    public boolean centerBlobApp = false;
    public boolean micApp = false;
    public boolean actionSelectorApp = false;
    /**
     * Lets various loops know its time to wrap things up when false, and prevents other loops from
     * starting until true.
     */
    public boolean appRunning = false;

    /*
    Enables measurements of time intervals between various functions and outputs to Logcat
    */
    public boolean timersOn = false;

    // Other generics
    protected static final String TAG = "abcvlib";

    public    int                  avgCount = 1000;

    protected void onCreate(Bundle savedInstanceState) {
        if(!appRunning){
            throw new IllegalStateException("initialize() not called prior to onCreate()");
        }
        else{
            super.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (cameraApp){
            inputs.vision.onStart();
        }
    }

    @Override
    protected void onStop() {
        appRunning = false;
        super.onStop();
        outputs.motion.setWheelOutput(0, 0);
        Toast.makeText(this, "In onStop", Toast.LENGTH_LONG).show();
        Log.i(TAG, "onStop Log");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (cameraApp){
            inputs.vision.onPause();
        }
        outputs.motion.setWheelOutput(0, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraApp) {
            inputs.vision.onDestroy();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (cameraApp){
            inputs.vision.onResume();
        }
    }

    /**
     * This must be called from child class
     * @param hostIP
     * @param hostPort
     * @param switches
     */
    protected void initialzer(String hostIP, int hostPort, HashMap<String, Boolean> switches){

        // loop over each hashmap entry to set the boolean switches needed in this class
        for (HashMap.Entry<String, Boolean> entry : switches.entrySet()){
            switch (entry.getKey()){
                case "loggerOn":
                    this.loggerOn = entry.getValue();
                    break;
                case "wheelPolaritySwap":
                    this.wheelPolaritySwap = entry.getValue();
                    break;
                case "motionSensorApp":
                    this.motionSensorApp = entry.getValue();
                    break;
                case "quadEncoderApp":
                    this.quadEncoderApp = entry.getValue();
                    break;
                case "pythonControlApp":
                    this.pythonControlApp = entry.getValue();
                    break;
                case "balanceApp":
                    this.balanceApp = entry.getValue();
                    break;
                case "cameraApp":
                    this.cameraApp = entry.getValue();
                    break;
                case "centerBlobApp":
                    this.centerBlobApp = entry.getValue();
                    break;
                case "micApp":
                    this.micApp = entry.getValue();
                    break;
                case "actionSelectorApp":
                    this.actionSelectorApp = entry.getValue();
                    break;
            }
        }

        inputs = new Inputs(this);
        outputs = new Outputs(this, hostIP, hostPort);

        if (actionSelectorApp){
            aD = new ActionDistribution();
            aS = new ActionSelector(this);

            ActionSelector actionSelector = new ActionSelector(this);
            actionSelectorThread = new Thread(actionSelector);
            actionSelectorThread.start();
        }

        // Tell all child classes it is ok to proceed.
        this.appRunning = true;
    }


    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == Vision.CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            inputs.vision.onCameraPermissionGranted();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     Overriding here passes the initialized AbcvlibLooper object to the IOIOLooper class which
     then connects the object to the actual IOIOBoard. There is no need to start a separate thread
     or create a global objects in the Android App MainActivity or otherwise.
      */
    @Override
    protected IOIOLooper createIOIOLooper() {
        /*
         This wait loop prevents the creation of AbcvlibLooper before we can assign the values of
         loggerOn and wheelPolaritySwap. Since we have no access to this call within IOIOActivity,
         this is the only way I found possible. Though this may have a better workaround, it should
         only potentially add a few hundred milliseconds to the startup of the app, and have no
         performance degredation while up and running.
         */
//        while (!appRunning){
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        return new AbcvlibLooper(this, loggerOn, wheelPolaritySwap);
    }
}
