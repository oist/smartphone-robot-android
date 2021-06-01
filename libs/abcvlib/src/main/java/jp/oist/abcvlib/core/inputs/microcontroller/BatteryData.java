package jp.oist.abcvlib.core.inputs.microcontroller;

import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;

public class BatteryData implements AbcvlibInput {

    private TimeStepDataBuffer timeStepDataBuffer = null;
    private boolean isRecording = false;
    private BatteryDataListener batteryDataListener = null;

    public BatteryData(TimeStepDataBuffer timeStepDataBuffer){
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public void onBatteryVoltageUpdate(double voltage, long timestamp) {
        if (isRecording){
            timeStepDataBuffer.getWriteData().getBatteryData().put(voltage);
        }
        if (batteryDataListener != null){
            batteryDataListener.onBatteryVoltageUpdate(voltage, timestamp);
        }
    }

    public void onChargerVoltageUpdate(double voltage, long timestamp) {
        if (isRecording){
            timeStepDataBuffer.getWriteData().getChargerData().put(voltage);
        }
        if (batteryDataListener != null){
            batteryDataListener.onChargerVoltageUpdate(voltage, timestamp);
        }
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    public void setTimeStepDataBuffer(TimeStepDataBuffer timeStepDataBuffer) {
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public void setBatteryDataListener(BatteryDataListener batteryDataListener) {
        this.batteryDataListener = batteryDataListener;
    }

    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }

}