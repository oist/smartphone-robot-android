package jp.oist.abcvlib.core.inputs.phone;

import android.Manifest;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.R;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.util.ImageOps;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;
import jp.oist.abcvlib.util.YuvToRgbConverter;

public class ImageData implements ImageAnalysis.Analyzer, AbcvlibInput {

    private ImageAnalysis imageAnalysis;
    private final ExecutorService imageExecutor;
    private final YuvToRgbConverter yuvToRgbConverter;
    private TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;
    private PreviewView mPreviewView;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = { Manifest.permission.CAMERA };

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;

    private CameraSelector cameraSelector;
    private Preview preview;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;

    public ImageData(AbcvlibActivity abcvlibActivity){

        imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "imageAnalysis"));

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(20)
                        .build();
        imageAnalysis.setAnalyzer(imageExecutor, this);

        mPreviewView = abcvlibActivity.findViewById(R.id.camera_x_preview);

        // Request camera permissions
        if (abcvlibActivity.allPermissionsGranted()) {
            startCamera(abcvlibActivity);
        } else {
            ActivityCompat.requestPermissions(
                    abcvlibActivity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        yuvToRgbConverter = new YuvToRgbConverter(abcvlibActivity.getApplicationContext());
        this.timeStepDataBuffer = abcvlibActivity.getTimeStepDataBuffer();
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

    public void startCamera(AbcvlibActivity abcvlibActivity) {
        if (mPreviewView != null){
            mPreviewView.post(() -> {
                mCameraProviderFuture = ProcessCameraProvider.getInstance(abcvlibActivity);
                mCameraProviderFuture.addListener(() -> {
                    try {
                        cameraProvider = mCameraProviderFuture.get();
                        bindAll(cameraProvider, abcvlibActivity);
                    } catch (ExecutionException | InterruptedException e) {
                        // No errors need to be handled for this Future.
                        // This should never be reached.
                    }
                }, ContextCompat.getMainExecutor(abcvlibActivity));
            });
        }
    }

    private void bindAll(@NonNull ProcessCameraProvider cameraProvider, AbcvlibActivity abcvlibActivity) {
        preview = new Preview.Builder()
                .build();
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        if (imageAnalysis != null){
            camera = cameraProvider.bindToLifecycle(abcvlibActivity, cameraSelector, preview, imageAnalysis);
        }else{
            camera = cameraProvider.bindToLifecycle(abcvlibActivity, cameraSelector, preview);
        }
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
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

    @Override
    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }
}
