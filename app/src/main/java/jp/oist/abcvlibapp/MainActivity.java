package jp.oist.abcvlibapp;

import android.os.Bundle;

import jp.oist.abcvlib.basic.AbcvlibActivity;

/**
 * Most basic Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages.
        super.onCreate(savedInstanceState);
        // Sets PWM pulse width to 500 microseconds for both wheels.
        super.abcvlibMotion.setWheelSpeed(500,500);
        // Setup Android GUI.
        setContentView(R.layout.activity_main);
    }

}
