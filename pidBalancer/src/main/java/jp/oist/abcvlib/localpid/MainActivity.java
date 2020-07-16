package jp.oist.abcvlib.localpid;

import android.os.Bundle;
import jp.oist.abcvlib.AbcvlibActivity;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Various switches are available to turn on/off core functionality.
        switches.balanceApp = true;
        switches.pythonControlApp = true;
        switches.wheelPolaritySwap = false;

        // Note the previously optional parameters that handle the connection to the python server
        initialzer(this,"192.168.20.26", 3000);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);
    }
}
