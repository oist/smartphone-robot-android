package jp.oist.abcvlib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import jp.oist.abcvlib.learning.ActionDistribution;
import jp.oist.abcvlib.learning.ActionSelector;
import jp.oist.abcvlib.learning.RewardGenerator;
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
public abstract class AbcvlibActivity extends IOIOActivity implements RewardGenerator {

    // Publically accessible objects that encapsulate a lot other core functionality
    public Inputs inputs;
    public Outputs outputs;
    public ActionDistribution aD;
    public ActionSelector aS;
    private Thread actionSelectorThread;
    public Switches switches = new Switches();

    /**
     * Lets various loops know its time to wrap things up when false, and prevents other loops from
     * starting until true. Set to true after AbcvlibActivity.initializer() finishes.
     */
    public boolean appRunning = false;

    // Other generics
    protected static final String TAG = "abcvlib";

    public int avgCount = 1000;

    protected void onCreate(Bundle savedInstanceState) {
        if(!appRunning){
            throw new IllegalStateException("initialize() not called prior to onCreate()");
        }
        else{
            super.onCreate(savedInstanceState);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onStart() {
        if (switches.cameraApp){
            inputs.vision.onStart();
        }
        super.onStart();
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
        if (switches.cameraApp){
            inputs.vision.onPause();
        }

        super.onPause();
        outputs.motion.setWheelOutput(0, 0);
    }

    @Override
    public void onDestroy() {
        if (switches.cameraApp) {
            inputs.vision.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onResume()
    {
        if (switches.cameraApp){
            inputs.vision.onResume();
        }
        super.onResume();
    }

    protected void initialzer(AbcvlibActivity abcvlibActivity, String hostIP, int hostPort, AbcvlibController controller){

        //Todo some logic here to test for boolean combinations that would lead to errors.
        // e.g. balanceApp without pythonControlApp

        inputs = new Inputs(abcvlibActivity);
        outputs = new Outputs(abcvlibActivity, hostIP, hostPort, controller);

        if (switches.actionSelectorApp){
            if (aD == null){
                aD = new ActionDistribution();
            }
            aS = new ActionSelector(this);
            aS.start();
        }

        // Tell all child classes it is ok to proceed.
        this.appRunning = true;
    }

    /**
     * Default null controller
     * @param hostIP
     * @param hostPort
     */
    protected void initialzer(AbcvlibActivity abcvlibActivity, String hostIP, int hostPort){

        initialzer(abcvlibActivity, hostIP, hostPort, null);

    }

    /**
     * null initializer for basic module or those not interacting with anything other than itself
     */
    protected void initialzer(AbcvlibActivity abcvlibActivity){

        initialzer(abcvlibActivity, null, 0, null);

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
        Log.d("abcvlib", "createIOIOLooper Finished");
        return new AbcvlibLooper(this, switches.loggerOn, switches.wheelPolaritySwap);
    }

    public double determineReward(){
        return 0;
    }
}
