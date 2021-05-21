package jp.oist.abcvlib.core.inputs.phone.vision;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.util.ImageOps;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.YuvToRgbConverter;

public class ImageData implements ImageAnalysis.Analyzer{

    private ImageAnalysis imageAnalysis;
    private final ExecutorService imageExecutor;
    private final YuvToRgbConverter yuvToRgbConverter;
    private final TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;

    public ImageData(AbcvlibActivity abcvlibActivity){

        imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "imageAnalysis"));

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(20)
                        .build();
        imageAnalysis.setAnalyzer(imageExecutor, this);

        yuvToRgbConverter = new YuvToRgbConverter(abcvlibActivity.getApplicationContext());
        this.timeStepDataBuffer = abcvlibActivity.getTimeStepDataAssembler().getTimeStepDataBuffer();
    }

    public ImageAnalysis getImageAnalysis() {
        return imageAnalysis;
    }
    
    @androidx.camera.core.ExperimentalGetImage
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image image = null;
        if (isRecording()){
            image = imageProxy.getImage();
        } else {
            imageProxy.close();
            return;}
        if (image != null) {
            int width = image.getWidth();
            int height = image.getHeight();
            long timestamp = image.getTimestamp();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            yuvToRgbConverter.yuvToRgb(image, bitmap);

            ByteArrayOutputStream webpByteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 0, webpByteArrayOutputStream);
            byte[] webpBytes = webpByteArrayOutputStream.toByteArray();
            Bitmap webpBitMap = ImageOps.generateBitmap(webpBytes);

            timeStepDataBuffer.getWriteData().getImageData().add(timestamp, width, height, webpBitMap, webpBytes);
//            Log.v("flatbuff", "Wrote image to timeStepDataBuffer");
        }
        imageProxy.close();
    }

    public synchronized void setRecording(boolean recording) {
        isRecording = recording;
    }

    public synchronized boolean isRecording() {
        return isRecording;
    }
}
