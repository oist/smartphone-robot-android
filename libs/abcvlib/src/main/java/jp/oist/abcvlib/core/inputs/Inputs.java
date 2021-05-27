package jp.oist.abcvlib.core.inputs;

import android.content.Context;

import java.util.ArrayList;

import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;

public class Inputs {

    private TimeStepDataBuffer timeStepDataBuffer = null;
    private BatteryData batteryData = null;
    private WheelData wheelData = null;
    private ImageData imageData = null;
    private MicrophoneData microphoneData = null;
    private OrientationData orientationData = null;
    private ArrayList<AbcvlibInput> inputsList;

    public Inputs(Context context, TimeStepDataBuffer timeStepDataBuffer, ArrayList<AbcvlibInput> inputsList){
        this.timeStepDataBuffer = timeStepDataBuffer;

        if (timeStepDataBuffer == null){
            this.timeStepDataBuffer = new TimeStepDataBuffer(10);
        }
        // Set default input data instances
        batteryData = new BatteryData(this.timeStepDataBuffer);
        wheelData = new WheelData(this.timeStepDataBuffer);
        orientationData = new OrientationData(this.timeStepDataBuffer, context);

        if (inputsList == null){
            inputsList = new ArrayList<>();
        }
        this.inputsList = inputsList;

        // Set custom input data instances if provided
        for (AbcvlibInput input:inputsList){
            Class<?> inputClass = input.getClass();

            if (inputClass == BatteryData.class){
                this.batteryData = (BatteryData) input;
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
            input.setTimeStepDataBuffer(this.timeStepDataBuffer);
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

    public ArrayList<AbcvlibInput> getInputsList() {
        return inputsList;
    }
}
