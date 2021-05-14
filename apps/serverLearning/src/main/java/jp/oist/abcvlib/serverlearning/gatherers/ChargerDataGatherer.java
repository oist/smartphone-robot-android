package jp.oist.abcvlib.serverlearning.gatherers;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.Inputs;

public class ChargerDataGatherer implements Runnable{

    private Inputs inputs;
    private TimeStepDataBuffer timeStepDataBuffer;

    public ChargerDataGatherer(AbcvlibActivity abcvlibActivity, TimeStepDataBuffer timeStepDataBuffer){
        this.inputs = abcvlibActivity.inputs;
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    @Override
    public void run() {
        timeStepDataBuffer.getWriteData().getChargerData().put(inputs.battery.getVoltageCharger());
    }
}
