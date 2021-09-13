package jp.oist.abcvlib.core.inputs.phone;

import android.graphics.Bitmap;

import jp.oist.abcvlib.core.inputs.Subscriber;

public interface ImageDataSubscriber extends Subscriber {
    /**
     * Likely easier to use TimeStampDataBuffer where all of this is collected over time into
     * timesteps, but this serves as a way to inspect the stream.
     * @param timestamp in nanoseconds see {@link java.lang.System#nanoTime()}
     * @param width in pixels
     * @param height in pixels
     * @param bitmap compressed bitmap object
     * @param webpImage byte array representing bitmap
     */
    void onImageDataUpdate(long timestamp, int width, int height, Bitmap bitmap, byte[] webpImage);
}
