package jp.oist.abcvlib.util;

import jp.oist.abcvlib.core.AbcvlibLooper;

public interface SerialReadyListener {
    /**
     * Called by abcvlibActivity after both Inputs and Output objects have been created, and Serial
     * connection has been established with MCU.
     * Implement this method in your MainActivity and put any code that uses the outputs or inputs
     * there so as to ensure no null pointers.
     */
    void onSerialReady(UsbSerial usbSerial);
}