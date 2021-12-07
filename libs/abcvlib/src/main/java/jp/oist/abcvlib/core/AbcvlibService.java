package jp.oist.abcvlib.core;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import jp.oist.abcvlib.core.inputs.Subscriber;
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
public abstract class AbcvlibService extends IOIOService implements Subscriber {
    // Publically accessible objects that encapsulate a lot other core functionality
    private Outputs outputs;
    private final Switches switches = new Switches();
    private AbcvlibLooper abcvlibLooper;
    private static final String TAG = "abcvlib";
    private IOReadyListener ioReadyListener;
    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public AbcvlibService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AbcvlibService.this;
        }
    }

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

        int result = super.onStartCommand(intent, flags, startId);
        return result;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "AbcvlibService onDestroy");
        super.onDestroy();
    }

    public void onPause(){
        if (abcvlibLooper != null){
            abcvlibLooper.shutDown();
        }
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
     Overriding here passes the initialized AbcvlibLooper object to the IOIOLooper class which
     then connects the object to the actual IOIOBoard. There is no need to start a separate thread
     or create a global objects in the Android App MainActivity or otherwise.
     */
    @Override
    public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
        if (this.abcvlibLooper == null && connectionType.equals("ioio.lib.android.accessory.AccessoryConnectionBootstrap.Connection")){
            this.abcvlibLooper = new AbcvlibLooper(ioReadyListener);
            initializeOutputs();
        }
        return this.abcvlibLooper;
    }
}
