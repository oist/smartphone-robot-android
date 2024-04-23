package jp.oist.abcvlib.core.inputs.microcontroller;

import jp.oist.abcvlib.core.inputs.Subscriber;

public interface BatteryDataSubscriber extends Subscriber {
    /**
     * Called every time the IOIOLooper runs once. Note this will happen at a variable time length
     * each call, but should be on the order of 2 milliseconds. You may want to ignore every 10
     * calls, filter results, or use the more robust TimeStepDataBuffer as a pipeline to access
     * this data.
     * @param voltage
     * @param timestamp in nanoseconds see {@link java.lang.System#nanoTime()}
     */
    void onBatteryVoltageUpdate(long timestamp, double voltage);
    /**
     * See {@link #onBatteryVoltageUpdate(long, double)} ()}
     */
    void onChargerVoltageUpdate(long timestamp, double chargerVoltage, double coilVoltage);
}
