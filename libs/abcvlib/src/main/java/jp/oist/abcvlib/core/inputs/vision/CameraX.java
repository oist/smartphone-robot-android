package jp.oist.abcvlib.core.inputs.vision;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

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

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.R;

public class CameraX {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = { Manifest.permission.CAMERA };

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;

    private ExecutorService analysisExecutor;
    private AbcvlibActivity abcvlibActivity;

    private PreviewView mPreviewView;

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

    private void bindAll(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(analysisExecutor, new ImageAnalysis.Analyzer() {
            @Override
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
        });

        Camera camera = cameraProvider.bindToLifecycle(abcvlibActivity, cameraSelector, preview, imageAnalysis);
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
    }

    public void startCamera() {
        if (mPreviewView != null){
            mPreviewView.post(() -> {
                mCameraProviderFuture = ProcessCameraProvider.getInstance(abcvlibActivity);
                mCameraProviderFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = mCameraProviderFuture.get();
                        bindAll(cameraProvider);
                    } catch (ExecutionException | InterruptedException e) {
                        // No errors need to be handled for this Future.
                        // This should never be reached.
                    }
                }, ContextCompat.getMainExecutor(abcvlibActivity));
            });
        }
    }





}
