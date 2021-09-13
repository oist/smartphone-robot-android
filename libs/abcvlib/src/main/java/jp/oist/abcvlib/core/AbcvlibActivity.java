package jp.oist.abcvlib.core;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import java.util.Iterator;
import java.util.Map;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.outputs.Outputs;
import jp.oist.abcvlib.util.ErrorHandler;

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
public abstract class AbcvlibActivity extends IOIOActivity implements AbcvlibAbstractObject {

    // Publically accessible objects that encapsulate a lot other core functionality
    private Outputs outputs;
    private final Switches switches = new Switches();
    protected AbcvlibLooper abcvlibLooper;
    private static final String TAG = "abcvlib";
    private IOReadyListener ioReadyListener;

    public void setIoReadyListener(IOReadyListener ioReadyListener) {
        this.ioReadyListener = ioReadyListener;
    }

    protected void onCreate(Bundle savedInstanceState) {
        /*
        This much be called prior to initializing outputs as this is what triggers the creation
        of the abcvlibLooper instance passed to the Outputs constructor
        */
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.v(TAG, "End of AbcvlibActivity.onCreate");
    }

    @Override
    protected void onStop() {
        super.onStop();
        abcvlibLooper.setDutyCycle(0, 0);
        Log.v(TAG, "End of AbcvlibActivity.onStop");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        abcvlibLooper.setDutyCycle(0, 0);
        Log.i(TAG, "End of AbcvlibActivity.onPause");
    }

    protected TimeStepDataBuffer getTimeStepDataBuffer(){
        return getInputs().getTimeStepDataBuffer();
    }

    public Switches getSwitches() {
        return switches;
    }

    public Outputs getOutputs() {
        return outputs;
    }

    private void initializeOutputs(){
        outputs = new Outputs(switches, abcvlibLooper);
    }

    /**
     * Take an array of permissions and check if they've all been granted. If not, request them. If
     * denied close app.
     * @param permissionsListener: Typically your main activity, where you'd implement this interface
     *                           and put all your main code into the {@link PermissionsListener#onPermissionsGranted()}
     * @param permissions: list of {@link android.Manifest.permission} strings your app requires.
     */
    protected void checkPermissions(PermissionsListener permissionsListener, String[] permissions){
        boolean permissionsGranted = true;
        for (String permission:permissions){
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED){
                permissionsGranted = false;
            }
        }
        if (permissionsGranted) {
            permissionsListener.onPermissionsGranted();
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
                            permissionsListener.onPermissionsGranted();
                        } else {
                            throw new RuntimeException("You did not approve required permissions");
                        }
                    });
            requestPermissionLauncher.launch(permissions);
        }
    }

    /**
     Overriding here passes the initialized AbcvlibLooper object to the IOIOLooper class which
     then connects the object to the actual IOIOBoard. There is no need to start a separate thread
     or create a global objects in the Android App MainActivity or otherwise.
      */
    @Override
    public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
        if (this.abcvlibLooper == null && connectionType.equals("ioio.lib.android.accessory.AccessoryConnectionBootstrap.Connection")){
            this.abcvlibLooper = new AbcvlibLooper(ioReadyListener);
            initializeOutputs();
            Log.d("abcvlib", "createIOIOLooper Finished");
        }
        return this.abcvlibLooper;
    }
}
