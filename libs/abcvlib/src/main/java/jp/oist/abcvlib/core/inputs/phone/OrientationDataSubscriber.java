package jp.oist.abcvlib.core.inputs.phone;

import jp.oist.abcvlib.core.inputs.Subscriber;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataSubscriber;

public interface OrientationDataSubscriber extends Subscriber {
    /**
     * See {@link BatteryDataSubscriber#onBatteryVoltageUpdate(double, long)} ()}
     * @param timestamp in nanoseconds see {@link java.lang.System#nanoTime()}
     * @param thetaRad tilt angle in radians. See {@link OrientationData#getThetaDeg()} for angle in degrees
     * @param angularVelocityRad
     */
    void onOrientationUpdate(long timestamp,
                             double thetaRad,
                             double angularVelocityRad);
}
