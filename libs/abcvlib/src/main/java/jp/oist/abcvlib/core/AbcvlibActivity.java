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

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.core.outputs.Outputs;

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
    private Inputs inputs;
    public Outputs outputs;
    public Switches switches = new Switches();
    private AbcvlibLooper abcvlibLooper;
    protected static final String TAG = "abcvlib";

    protected void onCreate(Bundle savedInstanceState) {

        inputs = new Inputs(getApplicationContext());
        /*
        This much be called prior to initializing outputs as this is what triggers the creation
        of the abcvlibLooper instance passed to the Outputs constructor
        */
        super.onCreate(savedInstanceState);
        outputs = new Outputs(switches, abcvlibLooper); //todo need to remove dependence on abcvlibActivty here

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.i(TAG, "End of AbcvlibActivity.onCreate");
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            abcvlibLooper.setDutyCycle(0, 0);
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "End of AbcvlibActivity.onStop");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try {
            abcvlibLooper.setDutyCycle(0, 0);
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }        Log.i(TAG, "End of AbcvlibActivity.onPause");
    }

    public Inputs getInputs() {
        return inputs;
    }

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
    protected IOIOLooper createIOIOLooper() {
        this.abcvlibLooper = new AbcvlibLooper(this);
        Log.d("abcvlib", "createIOIOLooper Finished");
        return this.abcvlibLooper;
    }
}
