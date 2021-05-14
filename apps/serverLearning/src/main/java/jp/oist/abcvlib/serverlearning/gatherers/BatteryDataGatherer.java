package jp.oist.abcvlib.serverlearning.gatherers;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.Inputs;

public class BatteryDataGatherer implements Runnable{

    private final TimeStepDataBuffer timeStepDataBuffer;
    private final Inputs inputs;

    public BatteryDataGatherer(AbcvlibActivity abcvlibActivity, TimeStepDataBuffer timeStepDataBuffer){
        this.inputs = abcvlibActivity.inputs;
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    @Override
    public void run() {
        timeStepDataBuffer.getWriteData().getBatteryData().put(inputs.battery.getVoltageBatt());
    }
}
