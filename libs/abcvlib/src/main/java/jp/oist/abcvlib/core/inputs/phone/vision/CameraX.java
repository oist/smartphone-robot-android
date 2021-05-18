package jp.oist.abcvlib.core.inputs.phone.vision;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.R;

public class CameraX {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = { Manifest.permission.CAMERA };

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;

    public ExecutorService analysisExecutor;
    private AbcvlibActivity abcvlibActivity;
    private ImageAnalyzerActivity imageAnalyzerActivity;

    private PreviewView mPreviewView;

    public CameraSelector cameraSelector;
    public Preview preview;
    public Camera camera;
    public ProcessCameraProvider cameraProvider;

    public CameraX(AbcvlibActivity abcvlibActivity, ImageAnalyzerActivity imageAnalyzerActivity){

        this.abcvlibActivity = abcvlibActivity;
        this.imageAnalyzerActivity = imageAnalyzerActivity;

        mPreviewView = abcvlibActivity.findViewById(R.id.camera_x_preview);

        // Request camera permissions
        if (abcvlibActivity.allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    abcvlibActivity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        int threadPoolSize = 8;
        analysisExecutor = new ScheduledThreadPoolExecutor(threadPoolSize);
    }

    public CameraX(AbcvlibActivity abcvlibActivity){
        this.abcvlibActivity = abcvlibActivity;

        mPreviewView = abcvlibActivity.findViewById(R.id.camera_x_preview);

        // Request camera permissions
        if (abcvlibActivity.allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    abcvlibActivity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        int threadPoolSize = 8;
        analysisExecutor = new ScheduledThreadPoolExecutor(threadPoolSize);
    }

    public void startCamera() {
        if (mPreviewView != null){
            mPreviewView.post(() -> {
                mCameraProviderFuture = ProcessCameraProvider.getInstance(abcvlibActivity);
                mCameraProviderFuture.addListener(() -> {
                    try {
                        cameraProvider = mCameraProviderFuture.get();
                        bindAll(cameraProvider);
                    } catch (ExecutionException | InterruptedException e) {
                        // No errors need to be handled for this Future.
                        // This should never be reached.
                    }
                }, ContextCompat.getMainExecutor(abcvlibActivity));
            });
        }
    }

    private void bindAll(@NonNull ProcessCameraProvider cameraProvider) {
        preview = new Preview.Builder()
                .build();
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        if (imageAnalyzerActivity != null){
            ImageAnalysis imageAnalysis =  imageAnalyzerActivity.getAnalyzer();
            if (imageAnalysis != null){
                camera = cameraProvider.bindToLifecycle(abcvlibActivity, cameraSelector, preview, imageAnalysis);
            }
        }else{
            camera = cameraProvider.bindToLifecycle(abcvlibActivity, cameraSelector, preview);
        }
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
    }
}
