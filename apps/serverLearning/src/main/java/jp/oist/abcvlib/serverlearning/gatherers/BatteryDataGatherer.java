package jp.oist.abcvlib.serverlearning.gatherers;

import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataListener;

public class BatteryDataGatherer implements BatteryDataListener {

    private final TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;

    public BatteryDataGatherer(TimeStepDataBuffer timeStepDataBuffer){
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    @Override
    public void onBatteryVoltageUpdate(double voltage, long timestamp) {
        if (isRecording){
            timeStepDataBuffer.getWriteData().getBatteryData().put(voltage);
        }
    }

    @Override
    public void onChargerVoltageUpdate(double voltage, long timestamp) {
        if (isRecording){
            timeStepDataBuffer.getWriteData().getChargerData().put(voltage);
        }
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }
}