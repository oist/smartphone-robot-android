package jp.oist.abcvlib.core.inputs.microcontroller;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;

public class WheelDataGatherer {

    private final Inputs inputs;
    private final TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;

    public WheelDataGatherer(AbcvlibActivity abcvlibActivity, TimeStepDataBuffer timeStepDataBuffer){
        this.inputs = abcvlibActivity.inputs;
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public void onWheelDataUpdate(long timestamp, double left, double right){
        if (isRecording){
            timeStepDataBuffer.getWriteData().getWheelCounts().put(timestamp, left, right);
        }
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }
}
