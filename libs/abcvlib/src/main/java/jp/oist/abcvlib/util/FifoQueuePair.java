package jp.oist.abcvlib.util;

public class FifoQueuePair {
    private AndroidToRP2040Command androidToRP2040Command;
    private byte[] byteArray;

    public FifoQueuePair(AndroidToRP2040Command androidToRP2040Command, byte[] byteArray) {
        this.androidToRP2040Command = androidToRP2040Command;
        this.byteArray = byteArray;
    }

    public AndroidToRP2040Command getAndroidToRP2040Command() {
        return androidToRP2040Command;
    }

    public byte[] getByteArray() {
        return byteArray;
    }
}
