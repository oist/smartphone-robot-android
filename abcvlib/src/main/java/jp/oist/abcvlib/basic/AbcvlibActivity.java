package jp.oist.abcvlib.basic;

import android.os.Bundle;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

/**
 * AbcvlibActivity is where all of the other classes are initialized into an object. The objects
 * are then passed to one another in order to coordinate the various shared values between them.
 *
 * Android app MainActivity can start motion by extending AbcvlibActivity and then running
 * any of the methods within the object instance abcvlibMotion within an infinite threaded loop
 * e.g:
 *
 * @author Christopher Buckley https://github.com/topherbuckley
 *
 */
public class AbcvlibActivity extends IOIOActivity {

    public AbcvlibSensors abcvlibSensors;
    public AbcvlibMotion abcvlibMotion;

    /**
     * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
     * memory/disk space on the phone and may result in memory failure if run for a long time.
     */
    protected boolean loggerOn = false;

    /**
     * Enable/disable this
     */
    protected boolean wheelPolaritySwap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Pass Android App information up to parent classes
        super.onCreate(savedInstanceState);
        // Initialize AbcvlibSensors and AbcvlibMotion objects.
        abcvlibSensors = new AbcvlibSensors(this);
        abcvlibMotion = new AbcvlibMotion(abcvlibSensors);
    }

    /**
     Overriding here passes the initialized AbcvlibLooper object to the IOIOLooper class which
     then connects the object to the actual IOIOBoard. There is no need to start a separate thread
     or create a global objects in the Android App MainActivity or otherwise.
      */
    @Override
    protected IOIOLooper createIOIOLooper() {
        return new AbcvlibLooper(abcvlibSensors, abcvlibMotion, loggerOn, wheelPolaritySwap);
    }

}
