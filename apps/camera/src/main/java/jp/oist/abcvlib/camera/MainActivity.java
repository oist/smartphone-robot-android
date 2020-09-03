package jp.oist.abcvlib.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.PredefinedCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.oist.abcvlib.core.AbcvlibActivity;

/**
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity{

    private static final int PERMISSION_REQUESTS = 1;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService executor;
    private static String TAG = "abcvlibCameraX";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        //switches.cameraApp = true;

        // Initalizes various objects in parent class.
        initialzer(this);

        super.onCreate(savedInstanceState);

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }

        setContentView(R.layout.activity_main);

        executor = Executors.newSingleThreadExecutor();

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        initCameraProcessing();
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private void initCameraProcessing(){
        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();

                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                PreviewView previewView = findViewById(R.id.previewView);

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(
                        previewView.createSurfaceProvider());

                // Set up the capture use case to allow users to take photos
                ImageCapture imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new BarCodeAnalyzer());

                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis);

            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get() should
                // not block since the listener is being called, so no need to
                // handle InterruptedException.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public class BarCodeAnalyzer implements ImageAnalysis.Analyzer {
        private BarcodeScanner scanner = buildBarCodeScanner();
        @Override
        public void analyze(ImageProxy imageProxy) {
            @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                // Pass image to an ML Kit Vision API
                Task<List<Barcode>> result = scanner.process(image);

                result.addOnSuccessListener(executor, new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        // Task completed successfully
                        Log.i("CameraXApp3", "scanner task successful");
                        processBarCodes(barcodes);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        Log.i("CameraXApp3", "scanner task failed. Error:" + e);

                    }
                }).addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Barcode>> task) {
                        mediaImage.close();
                        imageProxy.close();
                    }
                });
            }
        }
        private BarcodeScanner buildBarCodeScanner() {
            BarcodeScannerOptions options =
                    new BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                    Barcode.FORMAT_QR_CODE)
                            .build();
            return BarcodeScanning.getClient(options);
        }
        //
        private void processBarCodes(List<Barcode> barcodes) {
            for (Barcode barcode : barcodes) {
                String rawValue = barcode.getRawValue();
                int valueType = barcode.getValueType();
                // See API reference for complete list of supported types
                if (valueType == Barcode.TYPE_TEXT) {
                    toast(getApplicationContext(), rawValue);
                }
            }
        }

        public void toast(final Context context, final String text) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
        }
    }

    public class ObjectRecognitionAnalyzer implements ImageAnalysis.Analyzer {
        private ObjectDetector scanner = buildObjectDetector();
        @Override
        public void analyze(ImageProxy imageProxy) {
            @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                // Pass image to an ML Kit Vision API
                Task<List<DetectedObject>> result = scanner.process(image);

                result.addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
                    @Override
                    public void onSuccess(@NonNull List<DetectedObject> detectedObjects) {
                        // Task completed successfully
                        Log.i("CameraXApp3", "scanner task successful");
                        processObjects(detectedObjects);
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        Log.i("CameraXApp3", "scanner task failed. Error:" + e);

                    }
                });
                mediaImage.close();
                imageProxy.close();
            }
        }
        private ObjectDetector buildObjectDetector() {
            ObjectDetectorOptions options =
                    new ObjectDetectorOptions.Builder()
                            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                            .enableClassification()  // Optional
                            .build();
            return ObjectDetection.getClient(options);
        }
        //
        private void processObjects(List<DetectedObject> detectedObjects) {
            for (DetectedObject detectedObject : detectedObjects) {
                toast(getApplicationContext(), "Received Message:");
                Rect boundingBox = detectedObject.getBoundingBox();
                Integer trackingId = detectedObject.getTrackingId();
                for (DetectedObject.Label label : detectedObject.getLabels()) {
                    String text = label.getText();
                    if (PredefinedCategory.FOOD.equals(text)) {
                        // Do something
                    }
                    int index = label.getIndex();
                    if (PredefinedCategory.FOOD_INDEX == index) {
                        // Do Something
                    }
                    float confidence = label.getConfidence();
                }

            }
        }

        public void toast(final Context context, final String text) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(context, text, Toast.LENGTH_SHORT).show());
        }
    }

}

