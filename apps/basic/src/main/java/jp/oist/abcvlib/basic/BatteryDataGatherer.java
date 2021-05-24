package jp.oist.abcvlib.basic;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;

public class BatteryDataGatherer extends BatteryData {
    public BatteryDataGatherer(AbcvlibActivity abcvlibActivity) {
        super(abcvlibActivity);
    }

    @Override
    public void onBatteryVoltageUpdate(double voltage, long timestamp){
        super.onBatteryVoltageUpdate(voltage, timestamp);

    }
}
