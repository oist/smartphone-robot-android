package jp.oist.abcvlib.core.inputs.phone.vision;

import android.graphics.Bitmap;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.util.ImageOps;

public class ImageAnalyzer implements ImageAnalysis.Analyzer {

    private final YuvToRgbConverter yuvToRgbConverter;
    private final TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;

    public ImageAnalyzer(AbcvlibActivity abcvlibActivity){
        yuvToRgbConverter = new YuvToRgbConverter(abcvlibActivity.getApplicationContext());
        this.timeStepDataBuffer = abcvlibActivity.getTimeStepDataAssembler().getTimeStepDataBuffer();
    }

    @androidx.camera.core.ExperimentalGetImage
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
