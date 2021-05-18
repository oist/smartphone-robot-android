package jp.oist.abcvlib.core.inputs.microcontroller;

public interface BatteryDataListener {
    void onBatteryVoltageUpdate(double voltage, double timestamp);
    void onChargerVoltageUpdate(double voltage, double timestamp);
}
