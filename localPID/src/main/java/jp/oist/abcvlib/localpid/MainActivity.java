package jp.oist.abcvlib.localpid;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.AbcvlibActivity;
import jp.oist.abcvlib.outputs.SocketClient;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    /**
     * Various booleans controlling optional functionality. All switches are optional as default
     * values have been set elsewhere. All changes here will override default values
     * Possible switches currently include:
     * <p><ul>
     * <li> loggerOn = false:<p>
     *      Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
     *      memory/disk space on the phone and may result in memory failure if run for a long time
     *      such as any learning tasks.
     * <li> wheelPolaritySwap = false:<p>
     *      Enable/disable this to swap the polarity of the wheels such that the default forward
     *      direction will be swapped (i.e. wheels will move cw vs ccw as forward).
     * <li> motionSensorApp = true;<p>
     *      Enable readings from phone gyroscope, accelerometer, and sensor fusion software sensors
     *      determining the angle of tile, angular velocity, etc.
     * <li> quadEncoderApp = true;<p>
     *      Enable readings from the wheel quadrature encoders to determine things like wheel speed,
     *      distance traveled, etc.
     * <li> pythonControlApp = false:<p>
     *      Control various things from a remote python server interface
     * <li> balanceApp = false:<p>
     *      Enable default PID controlled balancer. Custom controllers can be added to the output of
     *      this controller to enable balanced movements.
     * <li> cameraApp = false:<p>
     *      Enables the use of camera inputs via OpenCV.
     * <li> centerBlobApp = false;<p>
     *      Determines center of color blob and moves wheels in order to keep blob centered on screen
     * <li> micApp = false;<p>
     *      Enables raw audio feed as well as simple calculated metrics like rms,
     *      dB SPL (uncalibrated), etc.
     * <li> actionSelectorApp = false;<p>
     *      Generates an action selector with basic Q-learning. Generalized version still in development
     * </ul>
     */
    private static HashMap<String, Boolean> switches;
    static {
        switches = new HashMap<>();
        switches.put("balanceApp", true);
        switches.put("pythonControlApp", true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        initialzer("192.168.29.131", 65434, switches);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(jp.oist.abcvlib.localpid.R.layout.activity_main);
    }
}
