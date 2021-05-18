package jp.oist.abcvlib.core.inputs.microcontroller;

public class Battery {

    private double voltageBatt;
    private double voltageCharger;
    private double timestamp;

    public double getVoltageBatt() {
        return this.voltageBatt;
    }

    public double getVoltageCharger() {
        return this.voltageCharger;
    }

    public void setBatteryVoltage(double voltage, double timestamp) {
        this.timestamp = timestamp;
        this.voltageBatt = voltage;
    }

    public void setChargerVoltage(double voltage, double timestamp) {
        this.timestamp = timestamp;
        this.voltageCharger = voltage;
    }

}
