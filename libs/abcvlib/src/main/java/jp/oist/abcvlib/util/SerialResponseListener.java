package jp.oist.abcvlib.util;

public interface SerialResponseListener {
    /**
     * Called by abcvlibActivity after both Inputs and Output objects have been created, and Serial
     * connection has been established with MCU.
     * Implement this method in your MainActivity and put any code that uses the outputs or inputs
     * there so as to ensure no null pointers.
     */
    void onSerialReady(UsbSerial usbSerial);

    default void onRawPacketReceived(byte[] packet){
        // Do nothing
    }
    default void onAckReceived(){
        // Do nothing
    }

}
