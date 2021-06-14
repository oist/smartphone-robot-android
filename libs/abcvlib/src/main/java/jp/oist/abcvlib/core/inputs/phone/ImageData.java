package jp.oist.abcvlib.core.inputs.phone;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ImageOps;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;
import jp.oist.abcvlib.util.YuvToRgbConverter;

public class ImageData implements ImageAnalysis.Analyzer, AbcvlibInput{

    private ImageAnalysis imageAnalysis;
    private YuvToRgbConverter yuvToRgbConverter;
    private TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;
    private PreviewView previewView;

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;

    private ProcessCameraProvider cameraProvider;
    private ImageDataListener imageDataListener = null;
    private final String TAG = getClass().getName();

    /**
     * You can construct this with a PreviewView and or ImageAnalysis, but they can also be null and
     * set later with {@link #setPreviewView(PreviewView)} and
     * {@link #setImageAnalysis(ImageAnalysis, TimeStepDataBuffer, ImageDataListener)}. Note you
     * can also use {@link #setDefaultImageAnalysis(TimeStepDataBuffer, ImageDataListener)} if you
     * have no preference as to how the ImageAnalysis instance is built.
     * After you have set either or both, call the {@link #startCamera(Context, LifecycleOwner, CountDownLatch)} to
     * start one or both. The startCamera method will initialize only those that have been setup
     * prior to calling the startCamera method.
     * @param timeStepDataBuffer
     * @param previewView
     * @param imageAnalysis
     */
    public ImageData(TimeStepDataBuffer timeStepDataBuffer, PreviewView previewView,
                     ImageAnalysis imageAnalysis){

        if (previewView != null){
            setPreviewView(previewView);
        }
        if (imageAnalysis != null){
            setImageAnalysis(imageAnalysis, timeStepDataBuffer, null);
        }else {
            setDefaultImageAnalysis(this.timeStepDataBuffer, null);
        }
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public ImageAnalysis getImageAnalysis() {
        return imageAnalysis;
    }

    @androidx.camera.core.ExperimentalGetImage
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image image = null;
        if (isRecording() || imageDataListener != null){
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

            if (isRecording()){
                timeStepDataBuffer.getWriteData().getImageData().add(timestamp, width, height, webpBitMap, webpBytes);
            }
            if (imageDataListener != null){
                imageDataListener.onImageDataUpdate(timestamp, width, height, webpBitMap, webpBytes);
            }
//            Log.v("flatbuff", "Wrote image to timeStepDataBuffer");
        }
        imageProxy.close();
    }

    public void setPreviewView(PreviewView previewView) {
        this.previewView = previewView;
    }

    private void setDefaultImageAnalysis(TimeStepDataBuffer timeStepDataBuffer,
                                                     ImageDataListener imageDataListener){
        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(20)
                        .build();
        setImageAnalysis(imageAnalysis, timeStepDataBuffer, imageDataListener);
    }

    /**
     * @param imageAnalysis
     * @param timeStepDataBuffer
     * @param imageDataListener
     */
    public void setImageAnalysis(ImageAnalysis imageAnalysis, TimeStepDataBuffer timeStepDataBuffer,
                                 ImageDataListener imageDataListener) {
        this.timeStepDataBuffer = timeStepDataBuffer;
        this.imageDataListener = imageDataListener;
        this.imageAnalysis = imageAnalysis;
    }

    public void startCamera(Context context, LifecycleOwner lifecycleOwner, CountDownLatch latch) {
        ExecutorService imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "imageAnalysis"));
        if (imageAnalysis == null && previewView == null){
            throw new UnsupportedOperationException("Either setImageAnalysis or setPreviewView must be called prior to calling the startCamera method");
        }
        if (imageAnalysis != null && (timeStepDataBuffer != null || imageDataListener != null)){
            yuvToRgbConverter = new YuvToRgbConverter(context);
            imageAnalysis.setAnalyzer(imageExecutor, this);
        }
        if (previewView != null){
            previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
            previewView.post(() -> {
                mCameraProviderFuture = ProcessCameraProvider.getInstance(context);
                mCameraProviderFuture.addListener(() -> {
                    try {
                        cameraProvider = mCameraProviderFuture.get();
                        bindAll(cameraProvider, lifecycleOwner, latch);
                    } catch (ExecutionException | InterruptedException e) {
                        ErrorHandler.eLog(TAG, "Unexpected Error", e, true);
                    }
                }, ContextCompat.getMainExecutor(context));
            });
        }
    }

    private void bindAll(@NonNull ProcessCameraProvider cameraProvider, LifecycleOwner lifecycleOwner,
                         CountDownLatch latch) {
        Preview preview = new Preview.Builder()
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        Camera camera;
        if (imageAnalysis != null){
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);
        }else{
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview);
        }
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        latch.countDown();
    }

    public synchronized void setRecording(boolean recording) throws RecordingWithoutTimeStepBufferException {
        if (timeStepDataBuffer == null){
            throw new RecordingWithoutTimeStepBufferException();
        }else{
            isRecording = recording;
        }
    }

    public synchronized boolean isRecording() {
        return isRecording;
    }

    @Override
    public void setTimeStepDataBuffer(TimeStepDataBuffer timeStepDataBuffer) {
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public void setImageDataListener(ImageDataListener imageDataListener) {
        this.imageDataListener = imageDataListener;
    }

    @Override
    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }
}
