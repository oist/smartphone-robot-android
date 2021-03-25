package jp.oist.abcvlib.serverlearning;

import android.media.Image;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.audio.MicrophoneInput;
import jp.oist.abcvlib.core.inputs.vision.ImageAnalyzerActivity;

public class DataGatherer implements ImageAnalyzerActivity {

    AbcvlibActivity abcvlibActivity;
    MsgToServer msgToServer;
    ScheduledThreadPoolExecutor executor;
    ImageAnalysis imageAnalysis;
    MicrophoneInput microphoneInput;
    ScheduledFuture<?> wheelDataGatherer;
    ScheduledFuture<?> chargerDataGatherer;
    ScheduledFuture<?> batteryDataGatherer;
    ScheduledFuture<?> soundDataGatherer;
    ScheduledFuture<?> logger;

    public DataGatherer(AbcvlibActivity abcvlibActivity, MsgToServer msgToServer){
        this.abcvlibActivity = abcvlibActivity;
        this.msgToServer = msgToServer;

        int threadCount = 6;
        executor = new ScheduledThreadPoolExecutor(threadCount);

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

    void start(){
        microphoneInput = new MicrophoneInput(abcvlibActivity);

        wheelDataGatherer = executor.scheduleAtFixedRate(new WheelDataGatherer(), 0, 100, TimeUnit.MILLISECONDS);
        chargerDataGatherer = executor.scheduleAtFixedRate(new ChargerDataGatherer(), 0, 100, TimeUnit.MILLISECONDS);
        batteryDataGatherer = executor.scheduleAtFixedRate(new BatteryDataGatherer(), 0, 100, TimeUnit.MILLISECONDS);
        soundDataGatherer = executor.scheduleAtFixedRate(new SoundDataGather(), 0, 100, TimeUnit.MILLISECONDS);
        logger = executor.schedule(new Logger(), 1000, TimeUnit.MILLISECONDS);
    }

    class WheelDataGatherer implements Runnable{

        @Override
        public void run() {
            try {
                JSONArray wheelCount = new JSONArray();
                wheelCount.put(abcvlibActivity.inputs.quadEncoders.getWheelCountL());
                wheelCount.put(abcvlibActivity.inputs.quadEncoders.getWheelCountR());
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
                msgToServer.chargerData.put(String.valueOf(System.nanoTime()), abcvlibActivity.inputs.battery.getVoltageCharger());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class BatteryDataGatherer implements Runnable{

        @Override
        public void run() {
            try {
                msgToServer.batteryData.put(String.valueOf(System.nanoTime()), abcvlibActivity.inputs.battery.getVoltageBatt());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class ImageDataGatherer implements ImageAnalysis.Analyzer{

        JSONObject planesJSON = new JSONObject();

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
//                    Log.i("analyzer", "Plane: " + idx + " width: " + width + " height: " + height + " WxH: " + width*height + " limit: " + n);
//                        frameBuffer.flip();
                    frame = new byte[n];
                    frameBuffer.get(frame);

                    try {
                        planesJSON.put("Plane" + idx, new JSONArray(frame));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    frameBuffer.clear();
                    idx++;
                }

                try {
                    msgToServer.imageData.put(String.valueOf(image.getTimestamp()), planesJSON);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            imageProxy.close();
        }
    }

    class SoundDataGather implements Runnable{

        JSONArray audio = new JSONArray();

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            microphoneInput.recorder.read(microphoneInput.buffer, 0,
                    microphoneInput.buffer.length);
            msgToServer.soundData.put(microphoneInput.buffer);
        }
    }

    class Logger implements Runnable{

        @Override
        public void run() {
            Log.i("datagatherer", "start of logger run");
            wheelDataGatherer.cancel(true);
            chargerDataGatherer.cancel(true);
            batteryDataGatherer.cancel(true);
            soundDataGatherer.cancel(true);
            imageAnalysis.clearAnalyzer();
            Log.i("datagatherer", "after logger cancellations");
            Log.i("datagatherer", "logger enter try");
            msgToServer.assembleEpisode();
            Log.i("datagatherer", "prior to printing JSON");
//                Log.i("datagatherer", msgToServer.toString(4));
            Log.i("datagatherer", "after to printing JSON");
            Log.i("datagatherer", "end of logger run");

            Writer output = null;
            Log.i("datagatherer", "1");
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                Log.i("datagatherer", "2");
                File file = new File(abcvlibActivity.getExternalFilesDir(null), "test.json");
                Log.i("datagatherer", "3");
                try {
                    if (file.exists()){
                        file.delete();
                    }
                    file.createNewFile();
                    output = new BufferedWriter(new FileWriter(file));
                    Log.i("datagatherer", "4");
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    Log.i("datagatherer", "4.1");
                    String string = gson.toJson(msgToServer, JSONObject.class);
                    Log.i("datagatherer", "4.2");
                    output.write(string);
                    Log.i("datagatherer", "5");
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    // Passes custom ImageAnalysis object to core CameraX lib to bind to lifecycle, and other admin functions
    @Override
    public ImageAnalysis getAnalyzer() {
        return imageAnalysis;
    }

}
