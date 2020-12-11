/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.oist.abcvlib.camera;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.ViewModelStore;

import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.LocalModel;
import jp.oist.abcvlib.camera.automl.AutoMLImageLabelerProcessor;
import jp.oist.abcvlib.camera.barcodescanner.BarcodeScannerProcessor;
import jp.oist.abcvlib.camera.facedetector.FaceDetectorProcessor;
import jp.oist.abcvlib.camera.labeldetector.LabelDetectorProcessor;
import jp.oist.abcvlib.camera.objectdetector.ObjectDetectorProcessor;
import jp.oist.abcvlib.camera.objectdetector.ObjectGraphic;
import jp.oist.abcvlib.camera.preference.PreferenceUtils;
import jp.oist.abcvlib.camera.preference.SettingsActivity;
import jp.oist.abcvlib.camera.preference.SettingsActivity.LaunchSource;
import jp.oist.abcvlib.camera.textdetector.TextRecognitionProcessor;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.outputs.Motion;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Live preview demo app for ML Kit APIs using CameraX.
 */
@KeepName
@RequiresApi(VERSION_CODES.LOLLIPOP)
public final class CameraXLivePreviewActivity extends AbcvlibActivity
        implements OnRequestPermissionsResultCallback,
        OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "CameraXLivePreview";
    private static final int PERMISSION_REQUESTS = 1;

    private static final String OBJECT_DETECTION = "Object Detection";
    private static final String OBJECT_DETECTION_CUSTOM = "Custom Object Detection";
    private static final String FACE_DETECTION = "Face Detection";
    private static final String TEXT_RECOGNITION = "Text Recognition";
    private static final String BARCODE_SCANNING = "Barcode Scanning";
    private static final String IMAGE_LABELING = "Image Labeling";
    private static final String IMAGE_LABELING_CUSTOM = "Custom Image Labeling";
    private static final String AUTOML_LABELING = "AutoML Image Labeling";

    private static final String STATE_SELECTED_MODEL = "selected_model";
    private static final String STATE_LENS_FACING = "lens_facing";

    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;

    @Nullable
    private ProcessCameraProvider cameraProvider;
    @Nullable
    private Preview previewUseCase;
    @Nullable
    private ImageCapture captureUseCase;
    @Nullable
    private ImageAnalysis analysisUseCase;
    @Nullable
    private VisionImageProcessor imageProcessor;
    private boolean needUpdateGraphicOverlayImageSourceInfo;

    private String selectedModel = BARCODE_SCANNING;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private CameraSelector cameraSelector;
    private Queue<List<DetectedObject>> objectDetectQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        switches.pythonControlApp = true;

        // Note the previously optional parameters that handle the connection to the python server
        initialzer(this,"192.168.28.102", 3000);

        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            Toast.makeText(
                    getApplicationContext(),
                    "CameraX is only supported on SDK version >=21. Current SDK version is "
                            + VERSION.SDK_INT,
                    Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, BARCODE_SCANNING);
            lensFacing = savedInstanceState.getInt(STATE_LENS_FACING, CameraSelector.LENS_FACING_FRONT);
        }
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        setContentView(R.layout.activity_camerax_live_preview);
        previewView = findViewById(R.id.preview_view);
        if (previewView == null) {
            Log.d(TAG, "previewView is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        Spinner spinner = findViewById(R.id.spinner);
        List<String> options = new ArrayList<>();
        options.add(BARCODE_SCANNING);
        options.add(OBJECT_DETECTION_CUSTOM);
        options.add(OBJECT_DETECTION);
        options.add(FACE_DETECTION);
        options.add(TEXT_RECOGNITION);
        options.add(IMAGE_LABELING);
        options.add(IMAGE_LABELING_CUSTOM);
        options.add(AUTOML_LABELING);
        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(this);

        ToggleButton facingSwitch = findViewById(R.id.facing_switch);
        facingSwitch.setOnCheckedChangeListener(this);

        objectDetectQueue = new ConcurrentLinkedQueue<List<DetectedObject>>();

        new ViewModelProvider(this, AndroidViewModelFactory.getInstance(getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(
                        this,
                        provider -> {
                            cameraProvider = provider;
                            if (allPermissionsGranted()) {
                                bindAllCameraUseCases();
                            }
                        });

        ImageView settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(
                v -> {
                    Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                    intent.putExtra(
                            SettingsActivity.EXTRA_LAUNCH_SOURCE,
                            SettingsActivity.LaunchSource.CAMERAX_LIVE_PREVIEW);
                    startActivity(intent);
                });

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }

        Display disp = ((WindowManager)this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(STATE_SELECTED_MODEL, selectedModel);
        bundle.putInt(STATE_LENS_FACING, lensFacing);
    }

    @Override
    public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        selectedModel = parent.getItemAtPosition(pos).toString();
        Log.d(TAG, "Selected model: " + selectedModel);
        bindAnalysisUseCase();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Set facing");
        if (cameraProvider == null) {
            return;
        }

        int newLensFacing =
                lensFacing == CameraSelector.LENS_FACING_FRONT
                        ? CameraSelector.LENS_FACING_BACK
                        : CameraSelector.LENS_FACING_FRONT;
        CameraSelector newCameraSelector =
                new CameraSelector.Builder().requireLensFacing(newLensFacing).build();
        try {
            if (cameraProvider.hasCamera(newCameraSelector)) {
                lensFacing = newLensFacing;
                cameraSelector = newCameraSelector;
                bindAllCameraUseCases();
                return;
            }
        } catch (CameraInfoUnavailableException e) {
            // Falls through
        }
        Toast.makeText(
                getApplicationContext(),
                "This device does not have lens with facing: " + newLensFacing,
                Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.live_preview_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.EXTRA_LAUNCH_SOURCE, LaunchSource.CAMERAX_LIVE_PREVIEW);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindAllCameraUseCases();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    private void bindAllCameraUseCases() {
        bindPreviewUseCase();
        bindAnalysisUseCase();
//        bindCaptureUseCase();
    }

    private void bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return;
        }
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        previewUseCase = new Preview.Builder().build();
        previewUseCase.setSurfaceProvider(previewView.createSurfaceProvider());
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewUseCase);
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        try {
            switch (selectedModel) {
                case OBJECT_DETECTION:
                    Log.i(TAG, "Using Object Detector Processor");
                    ObjectDetectorOptions objectDetectorOptions =
                            PreferenceUtils.getObjectDetectorOptionsForLivePreview(this);
                    imageProcessor = new ObjectDetectorProcessor(this, objectDetectorOptions, objectDetectQueue);
                    break;
                case OBJECT_DETECTION_CUSTOM:
                    Log.i(TAG, "Using Custom Object Detector  Processor");
                    LocalModel localModel =
                            new LocalModel.Builder()
                                    .setAssetFilePath("custom_models/model_MLKit_Hiroki_v1.tflite")
                                    .build();
                    CustomObjectDetectorOptions customObjectDetectorOptions =
                            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel);
                    imageProcessor = new ObjectDetectorProcessor(this, customObjectDetectorOptions);
                    break;
                case TEXT_RECOGNITION:
                    Log.i(TAG, "Using on-device Text recognition Processor");
                    imageProcessor = new TextRecognitionProcessor(this);
                    break;
                case FACE_DETECTION:
                    Log.i(TAG, "Using Face Detector Processor");
                    FaceDetectorOptions faceDetectorOptions =
                            PreferenceUtils.getFaceDetectorOptionsForLivePreview(this);
                    imageProcessor = new FaceDetectorProcessor(this, faceDetectorOptions);
                    break;
                case BARCODE_SCANNING:
                    Log.i(TAG, "Using Barcode Detector Processor");
                    imageProcessor = new BarcodeScannerProcessor(this);
                    // Motion Controller
                    MotionController motionController = new MotionController();
                    StopMotionController stopMotionController = new StopMotionController();
                    stopMotionController.setBarcodeScannerProcessor((BarcodeScannerProcessor) imageProcessor);
                    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2);
                    scheduledThreadPoolExecutor.execute(stopMotionController);
                    scheduledThreadPoolExecutor.scheduleAtFixedRate(motionController, 0, 2, SECONDS);
                    break;
                case IMAGE_LABELING:
                    Log.i(TAG, "Using Image Label Detector Processor");
                    imageProcessor = new LabelDetectorProcessor(this, ImageLabelerOptions.DEFAULT_OPTIONS);
                    break;
                case IMAGE_LABELING_CUSTOM:
                    Log.i(TAG, "Using Custom Image Label Detector Processor");
                    LocalModel localClassifier =
                            new LocalModel.Builder()
                                    .setAssetFilePath("custom_models/model_MLKit_Hiroki_v1.tflite")
                                    .build();
                    CustomImageLabelerOptions customImageLabelerOptions =
                            new CustomImageLabelerOptions.Builder(localClassifier).build();
                    imageProcessor = new LabelDetectorProcessor(this, customImageLabelerOptions);
                    break;
                case AUTOML_LABELING:
                    imageProcessor = new AutoMLImageLabelerProcessor(this);
                    break;
                default:
                    throw new IllegalStateException("Invalid model name");
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + selectedModel, e);
            Toast.makeText(
                    getApplicationContext(),
                    "Can not create image processor: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG)
                    .show();
            return;
        }

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        Size targetAnalysisSize = PreferenceUtils.getCameraXTargetAnalysisSize(this);
        if (targetAnalysisSize != null) {
            builder.setTargetResolution(targetAnalysisSize);
        }
        analysisUseCase = builder.build();

        needUpdateGraphicOverlayImageSourceInfo = true;
        analysisUseCase.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
                        } else {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
                        }
                        needUpdateGraphicOverlayImageSourceInfo = false;
                    }
                    try {
                        imageProcessor.processImageProxy(imageProxy, graphicOverlay);
                    } catch (MlKitException e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisUseCase);
    }

    private void bindCaptureUseCase() {

//        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
//            return;
//        }
        if (cameraProvider == null) {
            return;
        }
        if (captureUseCase != null) {
            cameraProvider.unbind(captureUseCase);
        }

        captureUseCase =
                new ImageCapture.Builder()
                        .build();
        Log.i(TAG, "captureUseCase Initialized");

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, captureUseCase);
        Log.i(TAG, "captureUseCase bound");

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

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
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

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            bindAllCameraUseCases();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    public class StopMotionController implements Runnable{

        BarcodeScannerProcessor barcodeScannerProcessor;

        @Override
        public void run() {
            while (appRunning) {
                // Wait for barcodeScannerProcessor to finish initalizing else qrCodeVisible may be null.
                if (barcodeScannerProcessor == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (barcodeScannerProcessor.qrCodeVisible) {
                    outputs.motion.setWheelOutput(0, 0);
                }
            }
        }

        public void setBarcodeScannerProcessor(BarcodeScannerProcessor barcodeScannerProcessor){
            this.barcodeScannerProcessor = barcodeScannerProcessor;
        }
    }

    public class MotionController implements Runnable{

        int speedL = 0; // Duty cycle from 0 to 100.
        int speedR = 0; // Duty cycle from 0 to 100.
        int maxAccelleration = 35;
        int minWheelCnt = 20;
        int maxSpeed = 30;
        int minSpeed = 30;
        int cnt = 0;

        ExecutorService cameraExecutor = Executors.newCachedThreadPool();

        public boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
            return false;
        }

        public void run(){

            Log.i("myLog", "entered RUN");
            randomWalk();
        }

        public void randomWalk(){
            // If current on some pin (current sense) is above X, assume stuck and reverse previous speeds for 1 s
            double countL = inputs.quadEncoders.getWheelCountL();
            double countR = inputs.quadEncoders.getWheelCountR();

            // Set a speed between min and max speed, then randomize the sign
            // Generating random integer between max and minSpeed
            speedL = ThreadLocalRandom.current().nextInt(minSpeed, maxSpeed + 1);
            // Randomly multiple by {-1, 0 , 1}
            speedL = speedL * (ThreadLocalRandom.current().nextInt(3) - 1);
            // Set a speed between min and max speed, then randomize the sign
            speedR = ThreadLocalRandom.current().nextInt(minSpeed, maxSpeed + 1);
            speedR = speedR * (ThreadLocalRandom.current().nextInt(3) - 1);

            outputs.motion.setWheelOutput(speedL, speedR);
        }

        private void captureImage(){
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/dataCollection/");
            boolean success = true;
            if (!directory.exists()){
                success = directory.mkdirs();
            }
            if (success){
                String photoName = Calendar.getInstance().getTime().toString() + "_" + cnt;
                ImageCapture.OutputFileOptions outputFileOptions =
                        new ImageCapture.OutputFileOptions.Builder(new File(directory, photoName)).build();
                if (captureUseCase != null) {
                    captureUseCase.takePicture(outputFileOptions, cameraExecutor,
                            new ImageCapture.OnImageSavedCallback() {
                                @Override
                                public void onImageSaved(@NotNull ImageCapture.OutputFileResults outputFileResults) {
                                    Log.i("imageCapture", "Image saved:" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + photoName);
                                }
                                @Override
                                public void onError(@NotNull ImageCaptureException error) {
                                    Log.i("imageCapture", "Image save failed:" + error);
                                }
                            });
                    cnt++;
                } else {
                    Log.i(TAG, "captureUseCase is null");
                }
            }


        }

    }

}
