package jp.oist.abcvlib.core.inputs.microcontroller;

public interface BatteryDataListener {
    void onBatteryVoltageUpdate(double voltage, long timestamp);
    void onChargerVoltageUpdate(double voltage, long timestamp);
}
