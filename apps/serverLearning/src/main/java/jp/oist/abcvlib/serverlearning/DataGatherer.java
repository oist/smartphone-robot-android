package jp.oist.abcvlib.serverlearning;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import org.json.JSONException;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.vision.ImageAnalyzerActivity;

public class DataGatherer implements ImageAnalyzerActivity {

    AbcvlibActivity abcvlibActivity;
    MsgToServer msgToServer;
    ScheduledThreadPoolExecutor executor;
    ImageAnalysis imageAnalysis;

    public DataGatherer(AbcvlibActivity abcvlibActivity, MsgToServer msgToServer){
        this.abcvlibActivity = abcvlibActivity;
        this.msgToServer = msgToServer;

        int threadCount = 8;
        executor = new ScheduledThreadPoolExecutor(threadCount);

        executor.scheduleAtFixedRate(new WheelDataGatherer(), 0, 1000, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(new ChargerDataGatherer(), 0, 1000, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(new BatteryDataGatherer(), 0, 1000, TimeUnit.MILLISECONDS);

        /*
         * Setup CameraX ImageAnalysis Use Case
         * ref: https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder
         */
        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(executor, new ImageDataGatherer());

    }

    class WheelDataGatherer implements Runnable{

        // Todo change this to Int[] and all associated encoder counts
        Double[] wheelCount = new Double[2];

        @Override
        public void run() {
            wheelCount[0] = abcvlibActivity.inputs.quadEncoders.getWheelCountL();
            wheelCount[1] = abcvlibActivity.inputs.quadEncoders.getWheelCountR();

            try {
                msgToServer.wheelCounts.put(String.valueOf(System.nanoTime()), wheelCount);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class ChargerDataGatherer implements Runnable{

        @Override
        public void run() {
            try {
                msgToServer.wheelCounts.put(String.valueOf(System.nanoTime()), abcvlibActivity.inputs.battery.getVoltageCharger());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class BatteryDataGatherer implements Runnable{

        @Override
        public void run() {
            try {
                msgToServer.wheelCounts.put(String.valueOf(System.nanoTime()), abcvlibActivity.inputs.battery.getVoltageBatt());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class ImageDataGatherer implements ImageAnalysis.Analyzer{

        @androidx.camera.core.ExperimentalGetImage
        public void analyze(@NonNull ImageProxy imageProxy) {
            Image image = imageProxy.getImage();
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();
                byte[] frame = new byte[width * height];
                Image.Plane[] planes = image.getPlanes();
                int idx = 0;
                for (Image.Plane plane : planes){
                    ByteBuffer frameBuffer = plane.getBuffer();
                    int n = frameBuffer.limit();
                    Log.i("analyzer", "Plane: " + idx + " width: " + width + " height: " + height + " WxH: " + width*height + " limit: " + n);
//                        frameBuffer.flip();
                    frame = new byte[n];
                    frameBuffer.get(frame);
                    frameBuffer.clear();
                    idx++;
                }
            }
            imageProxy.close();
        }
    }

    // Passes custom ImageAnalysis object to core CameraX lib to bind to lifecycle, and other admin functions
    @Override
    public ImageAnalysis getAnalyzer() {
        return imageAnalysis;
    }

}
