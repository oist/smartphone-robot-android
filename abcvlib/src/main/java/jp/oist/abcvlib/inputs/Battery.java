package jp.oist.abcvlib.inputs;

public class Battery {

    private double voltage;
    private double timestamp;

    public double getVoltage() {
        return this.voltage;
    }

    public void setVoltage(double voltage, double timestamp) {
        this.timestamp = timestamp;
        this.voltage = voltage;
    }

}
