package jp.oist.abcvlib.core.inputs;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.util.CameraX;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;

public class Inputs {

    public OrientationData orientationData; // Doesn't need thread since handled by sensorManager or SensorService
    public JSONObject stateVariables;
    public MicrophoneData micInput;
    private WheelData wheelData;
    public CameraX camerax;
    private final ProcessPriorityThreadFactory processPriorityThreadFactory = new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY, "Inputs");
    private ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1, processPriorityThreadFactory);
    private final String TAG = this.getClass().getName();

    public Inputs(AbcvlibActivity abcvlibActivity, ImageData imageData){

        if (abcvlibActivity.switches.motionSensorApp){
            orientationData = new OrientationData(abcvlibActivity);
        }

        if (abcvlibActivity.switches.cameraXApp){
            camerax = new CameraX(abcvlibActivity, imageData);
        }

        if (abcvlibActivity.switches.micApp){
            micInput = new MicrophoneData(abcvlibActivity);
        }

        if (abcvlibActivity.switches.quadEncoderApp){
            wheelData = new WheelData(abcvlibActivity);
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
            Log.e(TAG,"Error", e);
        }

        return jsonObject;
    }

    public WheelData getWheelData() {
        return wheelData;
    }
}
