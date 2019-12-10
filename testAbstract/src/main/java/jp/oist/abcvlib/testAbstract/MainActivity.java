package jp.oist.abcvlib.testAbstract;

import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

import jp.oist.abcvlib.AbcvlibActivity;
import jp.oist.abcvlib.outputs.AbcvlibController;


/**
 * Most basic Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Shows basics of setting up any standard Android Application framework, and a simple log output of
 * theta and angular velocity via Logcat using onboard Android sensors.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    /**
     * Various booleans to switch on/off various functionalities. All of these have default values
     * within AbcvlibActivity, so they can be supplied or will default to typical values.
     */
    private static HashMap<String, Boolean> switches;
    static {
        switches = new HashMap<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        CustomController customController = new CustomController();
        CustomController customController2 = new CustomController();
        CustomController customController3 = new CustomController();

        initialzer("192.168.29.131", 65434, switches);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(jp.oist.abcvlib.testAbstract.R.layout.activity_main);

        new Thread(customController).start();
        new Thread(customController2).start();
        new Thread(customController3).start();

    }

    public class CustomController extends AbcvlibController {

        public void run(){

            setOutput(1, 1);
            Log.d(TAG,  this.toString() + "left:" + output.left);
        }

    }

}

