package jp.oist.abcvlib.inputs;

import android.content.SharedPreferences;
import android.media.MediaRecorder;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.util.HashMap;

import jp.oist.abcvlib.AbcvlibActivity;
import jp.oist.abcvlib.inputs.audio.MicrophoneInput;
import jp.oist.abcvlib.inputs.audio.MicrophoneInputListener;
import jp.oist.abcvlib.inputs.vision.Vision;

import static android.content.Context.MODE_PRIVATE;

public class Inputs implements InputsInterface, CameraBridgeViewBase.CvCameraViewListener2, MicrophoneInputListener {

    private Thread audioThread;
    public MotionSensors motionSensors; // Doesn't need thread since handled by sensorManager or SensorService
    public Vision vision; // Doesn't need thread since this is started by CameraBridgeViewBase
    public QuadEncoders quadEncoders; // Doesnt need thread since AbcvlibLooper is handling this already
    public JSONObject stateVariables;
    public MicrophoneInput micInput;

    double mOffsetdB = 10;  // Offset for bar, i.e. 0 lit LEDs at 10 dB.
    // The Google ASR input requirements state that audio input sensitivity
    // should be set such that 90 dB SPL at 1000 Hz yields RMS of 2500 for
    // 16-bit samples, i.e. 20 * log_10(2500 / mGain) = 90.
    double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
    // For displaying error in calibration.
    double mRmsSmoothed;  // Temporally filtered version of RMS.
    double mAlpha = 0.9;  // Coefficient of IIR smoothing filter for RMS.
    double rmsdB;

    public Inputs(AbcvlibActivity abcvlibActivity){

        if (abcvlibActivity.motionSensorApp){
            motionSensors = new MotionSensors(abcvlibActivity);
        }

        if (abcvlibActivity.quadEncoderApp) {
            quadEncoders = new QuadEncoders();
        }

        if (abcvlibActivity.cameraApp) {
            vision = new Vision(abcvlibActivity, 800, 480);
        }

        if (abcvlibActivity.micApp){
            audioThread = new Thread(new MicrophoneInput(this));;
            audioThread.start();
            micInput = new MicrophoneInput(this);
            SharedPreferences preferences = abcvlibActivity.getSharedPreferences("LevelMeter", MODE_PRIVATE);
            int mSampleRate = preferences.getInt("SampleRate", 16000);
            int mAudioSource = preferences.getInt("AudioSource",
                    MediaRecorder.AudioSource.VOICE_RECOGNITION);
            micInput.setSampleRate(mSampleRate);
            micInput.setAudioSource(mAudioSource);
            micInput.start();
        }

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
    public void onSpatialUpdate() {

    }
    @Override
    public void onQuadUpdate() {

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
    @Override
    public void processAudioFrame(short[] audioFrame) {
        // Compute the RMS value. (Note that this does not remove DC).
        double rms = 0;
        for (short value : audioFrame) {
            rms += value * value;
        }
        rms = Math.sqrt(rms / audioFrame.length);

        // Compute a smoothed version for less flickering of the display.
        mRmsSmoothed = (mRmsSmoothed * mAlpha) + (1 - mAlpha) * rms;
        rmsdB = 20 + (20.0 * Math.log10(mGain * mRmsSmoothed));
    }

}
