package jp.oist.abcvlib.core.inputs;

import android.content.Context;

import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.util.ErrorHandler;

public class Inputs {

    private TimeStepDataBuffer timeStepDataBuffer;
    private BatteryData batteryData;
    private WheelData wheelData;
    private ImageData imageData;
    private MicrophoneData microphoneData;
    private OrientationData orientationData;
    private ArrayList<AbcvlibInput> inputsList;

    public Inputs(Context context){
        this.timeStepDataBuffer = new TimeStepDataBuffer(10);
        // Set default input data instances
        batteryData = new BatteryData(this.timeStepDataBuffer);
        wheelData = new WheelData(this.timeStepDataBuffer);
        orientationData = new OrientationData(this.timeStepDataBuffer, context);
        microphoneData = new MicrophoneData(this.timeStepDataBuffer);
        imageData = new ImageData(this.timeStepDataBuffer, null, null);
    }

    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
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

    public void setTimeStepDataBuffer(TimeStepDataBuffer timeStepDataBuffer) {
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public void setBatteryData(BatteryData batteryData) {
        this.batteryData = batteryData;
    }

    public void setImageData(ImageData imageData) {
        this.imageData = imageData;
    }

    public void setInputsList(ArrayList<AbcvlibInput> inputsList) {
        this.inputsList = inputsList;
    }

    public void setMicrophoneData(MicrophoneData microphoneData) {
        this.microphoneData = microphoneData;
    }

    public void setOrientationData(OrientationData orientationData) {
        this.orientationData = orientationData;
    }

    public void setWheelData(WheelData wheelData) {
        this.wheelData = wheelData;
    }

    public void overwriteDefaults(ArrayList<AbcvlibInput> inputs, AbcvlibLooper abcvlibLooper)
            throws ClassNotFoundException {
        for (AbcvlibInput input : inputs){
            if (input.getClass() == BatteryData.class){
                this.batteryData = null;
                setBatteryData((BatteryData) input);
                abcvlibLooper.setBatteryData((BatteryData) input);
            }else if (input.getClass() == WheelData.class){
                this.wheelData = null;
                setWheelData((WheelData) input);
                abcvlibLooper.setWheelData((WheelData) input);
            }else if (input.getClass() == OrientationData.class){
                this.orientationData = null;
                setOrientationData((OrientationData) input);
            }else if (input.getClass() == MicrophoneData.class){
                this.microphoneData = null;
                setMicrophoneData((MicrophoneData) input);
            }else if (input.getClass() == ImageData.class){
                this.imageData = null;
                setImageData((ImageData) input);
            }else{
                throw new ClassNotFoundException("Unrecognized input type in ArrayList. " +
                        "Must be of type BatteryData, WheelData, OrientationData, MicrophoneData, " +
                        "or ImageData. A ");
            }
        }
    }
}
