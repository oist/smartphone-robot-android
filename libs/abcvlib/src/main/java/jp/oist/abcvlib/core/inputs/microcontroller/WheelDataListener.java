package jp.oist.abcvlib.core.inputs.microcontroller;

public interface WheelDataListener {

    /**
     * Looping call from IOIOboard with quadrature encoder updates.
     * See {@link WheelData#counts2Distance(int)}
     * See {@link jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataListener#onBatteryVoltageUpdate(double, long) 
     * for details on looper}
     * @param timestamp in nanoseconds see {@link java.lang.System#nanoTime()}
     */
    void onWheelDataUpdate(long timestamp, int wheelCountL, int wheelCountR,
                           double wheelDistanceL, double wheelDistanceR,
                           double wheelSpeedL, double wheelSpeedR);
}
