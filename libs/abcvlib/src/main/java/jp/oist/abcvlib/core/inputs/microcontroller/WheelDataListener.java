package jp.oist.abcvlib.core.inputs.microcontroller;

public interface WheelDataListener {

    /**
     * Looping call from IOIOboard with quadrature encoder updates.
     * See {@link WheelData#calcDistance(int)}
     * See {@link jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataListener#onBatteryVoltageUpdate(double, long) 
     * for details on looper}
     * @param timestamp in nanoseconds see {@link java.lang.System#nanoTime()}
     * @param countLeft Quadrature encoder counts from left wheel
     * @param countRight Quadrature encoder counts from right wheel
     */
    void onWheelDataUpdate(long timestamp, int countLeft, int countRight, double speedL, double speedR);
}
