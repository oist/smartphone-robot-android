package jp.oist.abcvlib.core;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.intentfilter.androidpermissions.PermissionManager;
import com.intentfilter.androidpermissions.models.DeniedPermission;
import com.intentfilter.androidpermissions.models.DeniedPermissions;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.core.outputs.Outputs;

import static java.util.Collections.singleton;

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
public abstract class AbcvlibService extends IOIOService implements AbcvlibAbstractObject {
    // Publically accessible objects that encapsulate a lot other core functionality
    private Inputs inputs;
    private Outputs outputs;
    private final Switches switches = new Switches();
    private AbcvlibLooper abcvlibLooper;
    private static final String TAG = "abcvlib";
    private IOReadyListener ioReadyListener;
    private final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    public void setIoReadyListener(IOReadyListener ioReadyListener) {
        this.ioReadyListener = ioReadyListener;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager notificationManager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (intent != null && intent.getAction() != null
                && intent.getAction().equals("stop")) {
            // User clicked the notification. Need to stop the service.
            if (notificationManager != null) {
                notificationManager.cancel(0);
            }
            stopSelf();
        } else {

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getBaseContext());

            notificationBuilder
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker("IOIO service running")
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle("IOIO Service")
                    .setContentText("Click to stop")
                    .setContentIntent(PendingIntent.getService(this, 0, new Intent(
                            "stop", null, this, this.getClass()), 0));

            if (notificationManager != null) {
                notificationManager.notify(1, notificationBuilder.build());
            }
        }

        checkPermissions();

        inputs = new Inputs(getApplicationContext());
        int result = super.onStartCommand(intent, flags, startId);
        return result;
    }

    public void checkPermissions(){
        PermissionManager.PermissionRequestListener permissionRequestListener = new PermissionManager.PermissionRequestListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(getApplicationContext(), "Permissions Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(DeniedPermissions deniedPermissions) {
                String deniedPermissionsText = "Denied: " + Arrays.toString(deniedPermissions.toArray());
                Toast.makeText(getApplicationContext(), deniedPermissionsText, Toast.LENGTH_SHORT).show();

                for (DeniedPermission deniedPermission : deniedPermissions) {
                    if(deniedPermission.shouldShowRationale()) {
                        // Display a rationale about why this permission is required
                    }
                }
            }
        };

        PermissionManager permissionManager = PermissionManager.getInstance(getApplicationContext());
        permissionManager.checkPermissions(singleton(Manifest.permission.CAMERA), permissionRequestListener);
        permissionManager.checkPermissions(singleton(Manifest.permission.RECORD_AUDIO), permissionRequestListener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        abcvlibLooper.setDutyCycle(0, 0);
        Log.v(TAG, "AbcvlibService onDestroy");
        super.onDestroy();
    }

    @Override
    public Inputs getInputs() {
        return inputs;
    }

    public Switches getSwitches() {
        return switches;
    }

    public Outputs getOutputs() {
        return outputs;
    }

    private void initializeOutputs(){
        outputs = new Outputs(switches, abcvlibLooper, inputs);
    }

    /**
     Overriding here passes the initialized AbcvlibLooper object to the IOIOLooper class which
     then connects the object to the actual IOIOBoard. There is no need to start a separate thread
     or create a global objects in the Android App MainActivity or otherwise.
      */
    @Override
    protected IOIOLooper createIOIOLooper() {
        if (this.abcvlibLooper == null){
            this.abcvlibLooper = new AbcvlibLooper(this);
            initializeOutputs();
            if (ioReadyListener != null){
                ioReadyListener.onIOReady();
            }
            Log.d("abcvlib", "createIOIOLooper Finished");
        }
        return this.abcvlibLooper;
    }
}
