package jp.oist.abcvlib.inputs;

import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import jp.oist.abcvlib.inputs.vision.Vision;
import jp.oist.abcvlib.outputs.Motion;

/**
 * Interface for all abcvlib inputs other than those handled by external libraries (e.g. openCV
 * provides its own camera interface via CvCameraViewListener2)
 */
public interface InputsInterface {

    // Microphone

    // Listeners
    void onSpatialUpdate(); // Accelerometer and gyro updates
    void onQuadUpdate(); // Quadrature encoder updates
}
