package jp.oist.abcvlib.core.inputs;

import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;

public class Inputs {

    private BatteryData batteryData = null;
    private WheelData wheelData = null;
    private ImageData imageData = null;
    private MicrophoneData microphoneData = null;
    private OrientationData orientationData = null;

    public Inputs(AbcvlibActivity abcvlibActivity, ArrayList<AbcvlibInput> inputArrayList){

        // Set default input data instances
        batteryData = new BatteryData(abcvlibActivity);
        wheelData = new WheelData(abcvlibActivity);
        orientationData = new OrientationData(abcvlibActivity);

        // Set custom input data instances if provided
        for (AbcvlibInput input:inputArrayList){
            Class<?> inputClass = input.getClass();

            if (inputClass == BatteryData.class){
                batteryData = (BatteryData) input;
            }else if(inputClass == WheelData.class){
                wheelData = (WheelData) input;
            }else if(inputClass == ImageData.class){
                imageData = (ImageData) input;
            }else if(inputClass == MicrophoneData.class){
                microphoneData = (MicrophoneData) input;
            }else if(inputClass == OrientationData.class){
                orientationData = (OrientationData) input;
            }else {
                throw new IllegalStateException("Unexpected value: " + input.getClass());
            }
            //Ensures all inputs, including those that were initialized outside of this class have a buffer
            input.setTimeStepDataBuffer(abcvlibActivity.getTimeStepDataBuffer());
        }
    }

    public BatteryData getBatteryData() {
        return batteryData;
    }

    public WheelData getWheelData() {
        return wheelData;
    }

    public ImageData getImageData() {
        return imageData;
    }

    public MicrophoneData getMicrophoneData() {
        return microphoneData;
    }

    public OrientationData getOrientationData() {
        return orientationData;
    }
}
