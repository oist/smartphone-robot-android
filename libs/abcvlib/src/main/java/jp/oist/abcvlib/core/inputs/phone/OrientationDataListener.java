package jp.oist.abcvlib.core.inputs.phone;

public interface OrientationDataListener {
    /**
     * See {@link jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataListener#onBatteryVoltageUpdate(double, long)} ()}
     * @param timestamp in nanoseconds see {@link java.lang.System#nanoTime()}
     * @param thetaRad tilt angle in radians. See {@link OrientationData#getThetaDeg()} for angle in degrees
     * @param angularVelocityRad
     */
    void onOrientationUpdate(long timestamp,
                             double thetaRad,
                             double angularVelocityRad);
}
