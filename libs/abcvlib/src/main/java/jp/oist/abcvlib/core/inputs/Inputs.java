package jp.oist.abcvlib.core.inputs;

import android.content.Context;

import java.util.ArrayList;

import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;

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
}
