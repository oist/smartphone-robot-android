package jp.oist.abcvlib.basic;

import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import jp.oist.abcvlib.AbcvlibActivity;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework, and a simple log output of
 * theta and angular velocity via Logcat using onboard Android sensors.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    private Executor cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //switches.cameraApp = true;

        // Initalizes various objects in parent class.
        initialzer(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        ListenableFuture cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                long timestamp = image.getImageInfo().getTimestamp();
                Log.i(TAG, "Image taken:" + timestamp);
            }
        });

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis);

        // Create "runnable" object (similar to a thread, but recommended over overriding thread class)
        SimpleTest simpleTest = new SimpleTest();
        // Start the runnable thread
        new Thread(simpleTest).start();

    }

    public class SimpleTest implements Runnable{

        TextView voltageDisplay = findViewById(R.id.voltage);

        // Every runnable needs a public run method
        public void run(){
            while(appRunning){
                // Prints theta and angular velocity to android logcat
                Log.i(TAG, "theta:" + inputs.motionSensors.getThetaDeg() + " thetaDot:" +
                        inputs.motionSensors.getThetaDegDot() + "Battery Voltage:" + inputs.battery.getVoltage());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        voltageDisplay.setText(inputs.battery.getVoltage() + "V");
                    }
                });
            }
        }
    }

}

