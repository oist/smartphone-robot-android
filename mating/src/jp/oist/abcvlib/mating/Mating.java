package jp.oist.abcvlib.mating;

import java.util.List;

import org.json.JSONObject;
import org.opencv.core.Point;

import android.os.Bundle;
import android.view.Window;

import jp.oist.abcvlib.AbcvlibActivity;
import jp.oist.abcvlib.outputs.SocketClient;

public class Mating extends AbcvlibActivity {


    private List<Point>          centroids;
    private JSONObject controls;
    double p_tilt = 0;
    double i_tilt = 0;
    double d_tilt = 0;
    double setPoint = 0;
    double p_wheel = 0;
    double p_phi = 0;
    private SocketClient socketClient = null;
    private String gender;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.mating_gui);
//        super.mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        inputs.vision.cameraApp = true; // enables various camera operations within AbcvlibActivity
        super.loggerOn = false;
        super.wheelPolaritySwap = true;
        gender = "male";
        super.onCreate(savedInstanceState);

    }
}
