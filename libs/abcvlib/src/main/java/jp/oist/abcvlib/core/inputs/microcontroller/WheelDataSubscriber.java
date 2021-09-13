package jp.oist.abcvlib.core.inputs.microcontroller;

import jp.oist.abcvlib.core.inputs.Subscriber;

public interface WheelDataSubscriber extends Subscriber {

    /**
     * Looping call from IOIOboard with quadrature encoder updates
     * See {@link BatteryDataSubscriber#onBatteryVoltageUpdate(double, long)
     * for details on looper}
     * @param timestamp in nanoseconds see {@link java.lang.System#nanoTime()}
     */
    void onWheelDataUpdate(long timestamp, int wheelCountL, int wheelCountR,
                           double wheelDistanceL, double wheelDistanceR,
                           double wheelSpeedInstantL, double wheelSpeedInstantR,
                           double wheelSpeedBufferedL, double wheelSpeedBufferedR,
                           double wheelSpeedExpAvgL, double wheelSpeedExpAvgR);
}
