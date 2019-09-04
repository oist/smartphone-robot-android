package jp.oist.abcvlib.basic;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

/**
 * AbcvlibActivity is where all of the other classes are initialized into objects. The objects
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
    public AbcvlibQuadEncoders abcvlibQuadEncoders;
    public AbcvlibSaveData abcvlibSaveData;

    /**
     * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
     * memory/disk space on the phone and may result in memory failure if run for a long time.
     */
    public boolean loggerOn = false;

    /**
     * Enable/disable this
     */
    public boolean wheelPolaritySwap = false;

    /**
     *  Not sure why initial PWM_FREQ is 1000, but assume this can be modified as necessary.
     *  This may depend on the motor or microcontroller requirements/specs. <br><br>
     *
     *  If motor is just a DC motor, I guess this does not matter much, but for servos, this would
     *  be the control function, so would have to match the baud rate of the microcontroller. Note
     *  this library is not set up to control servos at this time. <br><br>
     *
     *  The microcontroller likely has a maximum frequency which it can turn ON/OFF the IO, so
     *  setting PWM_FREQ too high may cause issues for certain microcontrollers.
     */
    private final int PWM_FREQ = 1000;

    public AbcvlibActivity(Context context) {
        // Initialize AbcvlibSensors and AbcvlibMotion objects.
        abcvlibSensors = new AbcvlibSensors(context, loggerOn);
        abcvlibQuadEncoders = new AbcvlibQuadEncoders(loggerOn);
        abcvlibMotion = new AbcvlibMotion(abcvlibSensors, abcvlibQuadEncoders, PWM_FREQ);
        abcvlibSaveData = new AbcvlibSaveData();
    }

    public AbcvlibActivity() {
        // Initialize AbcvlibSensors and AbcvlibMotion objects.
        abcvlibSensors = new AbcvlibSensors(this, loggerOn);
        abcvlibQuadEncoders = new AbcvlibQuadEncoders(loggerOn);
        abcvlibMotion = new AbcvlibMotion(abcvlibSensors, abcvlibQuadEncoders, PWM_FREQ);
        abcvlibSaveData = new AbcvlibSaveData();

        // Keeps screen from timing out
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        Toast.makeText(this, "In onStop", Toast.LENGTH_LONG).show();
        Log.i("abcvlib", "onStop Log");
        abcvlibMotion.setWheelSpeed(0,0);
        super.onStop();
    }

    /**
     Overriding here passes the initialized AbcvlibLooper object to the IOIOLooper class which
     then connects the object to the actual IOIOBoard. There is no need to start a separate thread
     or create a global objects in the Android App MainActivity or otherwise.
      */
    @Override
    protected IOIOLooper createIOIOLooper() {
        return new AbcvlibLooper(abcvlibSensors, abcvlibMotion, abcvlibQuadEncoders,PWM_FREQ, loggerOn, wheelPolaritySwap);
    }

}
