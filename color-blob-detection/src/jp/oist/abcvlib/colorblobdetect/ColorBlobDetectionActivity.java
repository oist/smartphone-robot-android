package jp.oist.abcvlib.colorblobdetect;

import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.content.Context;
import android.os.Bundle;
import android.view.Window;

import jp.oist.abcvlib.AbcvlibActivity;
import jp.oist.abcvlib.outputs.SocketClient;

public class ColorBlobDetectionActivity extends AbcvlibActivity {

    private static HashMap<String, Boolean> switches;
    static {
        switches = new HashMap<>();
        /*
         * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
         * memory/disk space on the phone and may result in memory failure if run for a long time
         * such as any learning tasks.
         */
        switches.put("loggerOn", false);
        /*
         * Enable/disable this to swap the polarity of the wheels such that the default forward
         * direction will be swapped (i.e. wheels will move cw vs ccw as forward).
         */
        switches.put("wheelPolaritySwap", true);
        /*
         * Tell initilizer to set up the PID controlled balancer
         */
        switches.put("balance", false);
        /*
         * Does the app use the camera as an input?
         */
        switches.put("cameraApp", false);
        /*
         * Controller to center blob when tracked. Can be used on top of balance and results will be
         * additive
         */
        switches.put("centerBlob", true);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        initialzer("192.168.28.151", 65434, switches);

        super.onCreate(savedInstanceState);

//        setContentView(R.layout.color_blob_detection_surface_view);
//        super.mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
    }
}
