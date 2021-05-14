package jp.oist.abcvlib.serverlearning.gatherers;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.Inputs;

public class ChargerDataGatherer implements Runnable{

    private final Inputs inputs;
    private final TimeStepDataBuffer timeStepDataBuffer;

    public ChargerDataGatherer(AbcvlibActivity abcvlibActivity, TimeStepDataBuffer timeStepDataBuffer){
        this.inputs = abcvlibActivity.inputs;
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    @Override
    public void run() {
        timeStepDataBuffer.getWriteData().getChargerData().put(inputs.battery.getVoltageCharger());
    }
}
