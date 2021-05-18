package jp.oist.abcvlib.serverlearning.gatherers;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataListener;

public class BatteryDataGatherer implements BatteryDataListener {

    private final TimeStepDataBuffer timeStepDataBuffer;
    private final Inputs inputs;

    public BatteryDataGatherer(AbcvlibActivity abcvlibActivity, TimeStepDataBuffer timeStepDataBuffer){
        this.inputs = abcvlibActivity.inputs;
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    @Override
    public void onBatteryVoltageUpdate(double voltage, long timestamp) {
        timeStepDataBuffer.getWriteData().getBatteryData().put(voltage);
    }

    @Override
    public void onChargerVoltageUpdate(double voltage, long timestamp) {
        timeStepDataBuffer.getWriteData().getChargerData().put(voltage);
    }
}
