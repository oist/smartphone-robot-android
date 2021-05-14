package jp.oist.abcvlib.serverlearning.gatherers;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.Inputs;

public class WheelDataGatherer implements Runnable{

    private Inputs inputs;
    private TimeStepDataBuffer timeStepDataBuffer;

    public WheelDataGatherer(AbcvlibActivity abcvlibActivity, TimeStepDataBuffer timeStepDataBuffer){
        this.inputs = abcvlibActivity.inputs;
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    @Override
    public void run() {
        double left = inputs.quadEncoders.getWheelCountL();
        double right = inputs.quadEncoders.getWheelCountR();
        timeStepDataBuffer.getWriteData().getWheelCounts().put(left, right);
    }
}
