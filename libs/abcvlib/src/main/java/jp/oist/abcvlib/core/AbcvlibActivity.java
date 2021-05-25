package jp.oist.abcvlib.core;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataAssembler;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.core.outputs.AbcvlibController;
import jp.oist.abcvlib.core.outputs.AbcvlibOutput;
import jp.oist.abcvlib.core.outputs.Outputs;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;
import jp.oist.abcvlib.util.SocketListener;

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
public abstract class AbcvlibActivity extends IOIOActivity implements SocketListener {

    // Publically accessible objects that encapsulate a lot other core functionality
    private Inputs inputs;
    public Outputs outputs;
    public Switches switches = new Switches();
    private TimeStepDataAssembler timeStepDataAssembler;
    private TimeStepDataBuffer timeStepDataBuffer;

    /**
     * Lets various loops know its time to wrap things up when false, and prevents other loops from
     * starting until true. Set to true after AbcvlibActivity.initializer() finishes.
     */
    public boolean appRunning = false;

    // Other generics
    protected static final String TAG = "abcvlib";

    private static final String[] REQUIRED_PERMISSIONS = new String[0];

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.i(TAG, "End of AbcvlibActivity.onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "End of AbcvlibActivity.onStart");
    }

    @Override
    protected void onStop() {
        appRunning = false;
        super.onStop();
        outputs.motion.setWheelOutput(0, 0);
        Toast.makeText(this, "In onStop", Toast.LENGTH_LONG).show();
        Log.i(TAG, "onStop Log");
        Log.i(TAG, "End of AbcvlibActivity.onStop");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        outputs.motion.setWheelOutput(0, 0);
        Log.i(TAG, "End of AbcvlibActivity.onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "End of AbcvlibActivity.onDestroy");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.i(TAG, "End of AbcvlibActivity.onResume");
    }

    protected void initializer(AbcvlibActivity abcvlibActivity,
                               AbcvlibController controller,
                               TimeStepDataAssembler timeStepDataAssembler,
                               ArrayList<AbcvlibInput> inputArrayList,
                               ArrayList<AbcvlibOutput> outputArrayList) {

        Log.i(TAG, "Start of AbcvlibActivity.initializer");

        if (inputArrayList == null){
            inputArrayList = new ArrayList<>();
        }
        if (outputArrayList == null){
            outputArrayList = new ArrayList<>();
        }

        if (timeStepDataAssembler != null){
            this.timeStepDataAssembler = timeStepDataAssembler;
        }else{
            this.timeStepDataBuffer = new TimeStepDataBuffer(10);
        }

        inputs = new Inputs(abcvlibActivity, inputArrayList);
        outputs = new Outputs(abcvlibActivity, controller);

        if (timeStepDataAssembler != null){
            this.timeStepDataAssembler.initializeInputs();
            try {
                this.timeStepDataAssembler.startGatherers();
            } catch (RecordingWithoutTimeStepBufferException e) {
                ErrorHandler.eLog(TAG, "Make sure to initialize a TimeStepDataBuffer object prior " +
                        "to setting isRecording to true", e, true);
            }
        }

        // Tell all child classes it is ok to proceed.
        this.appRunning = true;

        Log.i(TAG, "End of AbcvlibActivity.initializer");
    }

    /**
     * null initializer for basic module or those not interacting with anything other than itself
     */
    protected void initializer(AbcvlibActivity abcvlibActivity) {
        initializer(abcvlibActivity, null, null, null, null);

    }

    public Inputs getInputs() {
        return inputs;
    }

    protected void checkPermissions(AbcvlibActivity abcvlibActivity, String[] permissions){
        boolean permissionsGranted = false;
        for (String permission:permissions){
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED){
                permissionsGranted = true;
            }else{
                permissionsGranted = false;
            }
        }
        if (permissionsGranted) {
            abcvlibActivity.onPermissionsGranted();
        } else {
            ActivityResultLauncher<String[]> requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                        Iterator<Map.Entry<String, Boolean>> iterator = isGranted.entrySet().iterator();
                        boolean allGranted = false;
                        while(iterator.hasNext()){
                            Map.Entry<String, Boolean> pair = iterator.next();
                            allGranted = pair.getValue();
                        }
                        if (allGranted) {
                            Log.i(TAG, "Permissions granted");
                            abcvlibActivity.onPermissionsGranted();
                        } else {
                            throw new RuntimeException("You did not approve required permissions");
                        }
                    });
            requestPermissionLauncher.launch(permissions);
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    public boolean allPermissionsGranted() {
        if (PackageManager.PERMISSION_DENIED ==
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)){

        }
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    protected void onPermissionsGranted(){}

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
        while (!appRunning){
            try {
                Thread.yield();
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.e(TAG,"Error", e);
            }
        }
        Log.d("abcvlib", "createIOIOLooper Finished");
        return new AbcvlibLooper(this);
    }

    @Override
    public void onServerReadSuccess(JSONObject jsonHeader, ByteBuffer msgFromServer) {
        // Parse Message from Server
        Log.i("server", "Reading from Server Completed");
    }

    public TimeStepDataAssembler getTimeStepDataAssembler() {
        return timeStepDataAssembler;
    }

    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }
}
