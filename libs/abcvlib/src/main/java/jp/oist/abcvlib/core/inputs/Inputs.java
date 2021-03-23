package jp.oist.abcvlib.core.inputs;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.audio.MicrophoneInput;
import jp.oist.abcvlib.core.inputs.vision.CameraX;
import jp.oist.abcvlib.core.inputs.vision.ImageAnalyzerActivity;
import jp.oist.abcvlib.core.inputs.vision.Vision;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;

public class Inputs implements CameraBridgeViewBase.CvCameraViewListener2 {

    public MotionSensors motionSensors; // Doesn't need thread since handled by sensorManager or SensorService
    public Vision vision; // Doesn't need thread since this is started by CameraBridgeViewBase
    public QuadEncoders quadEncoders; // Doesnt need thread since AbcvlibLooper is handling this already
    public JSONObject stateVariables;
    public MicrophoneInput micInput;
    public CameraX camerax;
    public Battery battery;
    private final ProcessPriorityThreadFactory processPriorityThreadFactory = new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY, "Inputs");
    private ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1, processPriorityThreadFactory);

    public Inputs(AbcvlibActivity abcvlibActivity, ImageAnalyzerActivity imageAnalyzerActivity){

        if (abcvlibActivity.switches.motionSensorApp){
            motionSensors = new MotionSensors(abcvlibActivity);
        }

        if (abcvlibActivity.switches.quadEncoderApp) {
            quadEncoders = new QuadEncoders(abcvlibActivity);
        }

        if (abcvlibActivity.switches.cameraApp) {
            vision = new Vision(abcvlibActivity, 400, 240);
        }

        if (abcvlibActivity.switches.cameraXApp){
            camerax = new CameraX(abcvlibActivity, imageAnalyzerActivity);
        }

        if (abcvlibActivity.switches.micApp){
            micInput = new MicrophoneInput(abcvlibActivity);
            micInput.start();
        }

        battery = new Battery();

        stateVariables = initializeStateVariables();

    }

    private JSONObject initializeStateVariables(){

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("timeAndroid", 0.0);
            jsonObject.put("theta", 0.0);
            jsonObject.put("thetaDot", 0.0);
            jsonObject.put("wheelCountL", 0.0);
            jsonObject.put("wheelCountR", 0.0);
            jsonObject.put("distanceL", 0.0);
            jsonObject.put("distanceR", 0.0);
            jsonObject.put("wheelSpeedL", 0.0);
            jsonObject.put("wheelSpeedR", 0.0);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }
    @Override
    public void onCameraViewStopped() {

    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return null;
    }

}
