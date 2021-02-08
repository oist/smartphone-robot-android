package jp.oist.abcvlib.colorblobdetect;

import android.os.Bundle;

import org.opencv.android.OpenCVLoader;

import jp.oist.abcvlib.core.AbcvlibActivity;


public class ColorBlobDetectionActivity extends AbcvlibActivity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        switches.loggerOn = false;
        switches.pythonControlApp = true;
        switches.cameraApp = true;
        switches.centerBlobApp = true;
        switches.wheelPolaritySwap = false;

        initialzer(this, "192.168.20.195", 3000);

        super.onCreate(savedInstanceState);
    }
}
