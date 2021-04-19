package jp.oist.abcvlib.camera;

import android.os.Bundle;

import androidx.camera.core.ImageAnalysis;

import jp.oist.abcvlib.core.AbcvlibActivity;


public class MainActivity extends AbcvlibActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup a live preview of camera feed to the display. Remove if unwanted. 
        setContentView(R.layout.camera_x_preview);

        switches.cameraXApp = true;
        initialzer(this);
        super.onCreate(savedInstanceState);
    }
}
