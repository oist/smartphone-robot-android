package jp.oist.abcvlib.core.inputs.microcontroller;

import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;

public class BatteryData {

    private final TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;

    public BatteryData(TimeStepDataBuffer timeStepDataBuffer){
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public void onBatteryVoltageUpdate(double voltage, long timestamp) {
        if (isRecording){
            timeStepDataBuffer.getWriteData().getBatteryData().put(voltage);
        }
    }

    public void onChargerVoltageUpdate(double voltage, long timestamp) {
        if (isRecording){
            timeStepDataBuffer.getWriteData().getChargerData().put(voltage);
        }
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }
}