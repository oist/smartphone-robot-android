package jp.oist.abcvlib.core;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
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

    private Outputs outputs;
    private final Switches switches = new Switches();
    protected AbcvlibLooper abcvlibLooper;
    private static final String TAG = "abcvlib";
    private IOReadyListener ioReadyListener;

    public void setIoReadyListener(IOReadyListener ioReadyListener) {
        this.ioReadyListener = ioReadyListener;
    }

    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.v(TAG, "End of AbcvlibActivity.onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "End of AbcvlibActivity.onStop");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (abcvlibLooper != null){
//            abcvlibLooper.setDutyCycle(0, 0);
            abcvlibLooper.shutDown();
        }
        Log.i(TAG, "End of AbcvlibActivity.onPause");
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
