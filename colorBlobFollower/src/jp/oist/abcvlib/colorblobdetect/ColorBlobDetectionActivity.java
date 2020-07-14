package jp.oist.abcvlib.colorblobdetect;

import android.os.Bundle;

import org.opencv.android.OpenCVLoader;

import jp.oist.abcvlib.AbcvlibActivity;


public class ColorBlobDetectionActivity extends AbcvlibActivity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        switches.loggerOn = false;
        switches.balanceApp = true;
        switches.pythonControlApp = true;
        switches.cameraApp = true;
        switches.centerBlobApp = true;

        initialzer(this, "10.9.23.131", 3000);

        super.onCreate(savedInstanceState);
    }
}
