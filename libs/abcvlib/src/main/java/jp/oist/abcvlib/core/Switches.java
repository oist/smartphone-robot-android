package jp.oist.abcvlib.core;

/**
 * Various booleans controlling optional functionality. All switches are optional as default
 * values have been set elsewhere. All changes here will override default values
 */
public class Switches {
    /**
     * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
     * memory/disk space on the phone and may result in memory failure if run for a long time
     * such as any learning tasks.
     */
    public boolean loggerOn = false;
    /**
     *Enables measurements of time intervals between various functions and outputs to Logcat
     */
    public boolean timersOn = false;
    /**
     * Enable/disable this to swap the polarity of the wheels such that the default forward
     * direction will be swapped (i.e. wheels will move cw vs ccw as forward).
     */
    public boolean wheelPolaritySwap = true;
    /**
     * Enable readings from phone gyroscope, accelerometer, and sensor fusion software sensors
     * determining the angle of tile, angular velocity, etc
     */
    public boolean motionSensorApp = true;
    /**
     * Enable readings from the wheel quadrature encoders to determine things like wheel speed,
     * distance traveled, etc.
     */
    public boolean quadEncoderApp = true;
    /**
     * Control various things from a remote python server interface
     */
    public boolean pythonControlApp = false;
    /**
     * Enable default PID controlled balancer. Custom controllers can be added to the output of
     * this controller to enable balanced movements.
     */
    public boolean balanceApp = false;
    /**
     * Enables the use of camera inputs via OpenCV.
     */
    public boolean cameraApp = false;
    /**
     * Determines center of color blob and moves wheels in order to keep blob centered on screen
     */
    public boolean centerBlobApp = false;
    /**
     * Enables raw audio feed as well as simple calculated metrics like rms,
     * dB SPL (uncalibrated), etc.
     */
    public boolean micApp = false;
    /**
     * Generates an action selector with basic Q-learning. Generalized version still in development
     */
    public boolean actionSelectorApp = false;
}
