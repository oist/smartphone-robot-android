package jp.oist.abcvlib.serverlearning;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.audio.MicrophoneInput;
import jp.oist.abcvlib.core.inputs.vision.ImageAnalyzerActivity;
import jp.oist.abcvlib.core.inputs.vision.YuvToRgbConverter;

public class DataGatherer implements ImageAnalyzerActivity {

    AbcvlibActivity abcvlibActivity;
    MsgToServer msgToServer;
    ScheduledThreadPoolExecutor executor;
    ImageAnalysis imageAnalysis;
    MicrophoneInput microphoneInput;
    ScheduledFuture<?> wheelDataGatherer;
    ScheduledFuture<?> chargerDataGatherer;
    ScheduledFuture<?> batteryDataGatherer;
    ScheduledFuture<?> logger;

    public DataGatherer(AbcvlibActivity abcvlibActivity, MsgToServer msgToServer){
        this.abcvlibActivity = abcvlibActivity;
        this.msgToServer = msgToServer;

        int threadCount = 4;
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

        microphoneInput = new MicrophoneInput(abcvlibActivity);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void start(){
        wheelDataGatherer = executor.scheduleAtFixedRate(new WheelDataGatherer(), 0, 100, TimeUnit.MILLISECONDS);
        chargerDataGatherer = executor.scheduleAtFixedRate(new ChargerDataGatherer(), 0, 100, TimeUnit.MILLISECONDS);
        batteryDataGatherer = executor.scheduleAtFixedRate(new BatteryDataGatherer(), 0, 100, TimeUnit.MILLISECONDS);
        logger = executor.schedule(new Logger(), 1000, TimeUnit.MILLISECONDS);
    }

    class WheelDataGatherer implements Runnable{
        @Override
        public void run() {
            msgToServer.wheelCounts.put(abcvlibActivity.inputs.quadEncoders.getWheelCountL(),
                    abcvlibActivity.inputs.quadEncoders.getWheelCountR());
        }
    }

    class ChargerDataGatherer implements Runnable{
        @Override
        public void run() {
            msgToServer.chargerData.put(abcvlibActivity.inputs.battery.getVoltageCharger());
        }
    }

    class BatteryDataGatherer implements Runnable{
        @Override
        public void run() {
            msgToServer.batteryData.put(abcvlibActivity.inputs.battery.getVoltageBatt());
        }
    }

    class ImageDataGatherer implements ImageAnalysis.Analyzer{

        YuvToRgbConverter yuvToRgbConverter = new YuvToRgbConverter(abcvlibActivity);

        @androidx.camera.core.ExperimentalGetImage
        public void analyze(@NonNull ImageProxy imageProxy) {
            Image image = imageProxy.getImage();
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();
                long timestamp = image.getTimestamp();

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                yuvToRgbConverter.yuvToRgb(image, bitmap);

                int[] intFrame = new int[width * height];
                bitmap.getPixels(intFrame, 0, width, 0, 0, width, height);

                // convert bitmap to three byte[] with rgb.
                Bitmap2RGBVectors bitmap2RGBVectors = new Bitmap2RGBVectors(bitmap);
                int[][] rgbVectors = bitmap2RGBVectors.getRGBVectors();

                msgToServer.imageData.add(timestamp, width, height, rgbVectors);
            }
            imageProxy.close();
        }
    }

    static class Bitmap2RGBVectors {

        int[][] rgbVectors;

        public Bitmap2RGBVectors(Bitmap bitmap){

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int size = width * height;

            int[] r = new int[size];
            int[] g = new int[size];
            int[] b = new int[size];

            for(int y = 0; y < height; y++){
                for(int x = 0 ; x < width ; x++){
                    int pixel = bitmap.getPixel(x,y);
                    r[(x + (y * width))] = Color.red(pixel);
                    g[(x + (y * width))] = Color.green(pixel);
                    b[(x + (y * width))] = Color.blue(pixel);
                }
            }
            rgbVectors = new int[3][size];

            rgbVectors[0] = r;
            rgbVectors[1] = g;
            rgbVectors[2] = b;
        }

        public int[][] getRGBVectors(){
            return rgbVectors;
        }
    }

    class Logger implements Runnable{

        @Override
        public void run() {
            Log.i("datagatherer", "start of logger run");
            wheelDataGatherer.cancel(true);
            chargerDataGatherer.cancel(true);
            batteryDataGatherer.cancel(true);
            imageAnalysis.clearAnalyzer();
            microphoneInput.stop();
            msgToServer.soundData.setMetaData(
                    microphoneInput.getSampleRate(), microphoneInput.getStartTime(),
                    microphoneInput.getEndTime());
            microphoneInput.close();
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
                    String string = gson.toJson(msgToServer);
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
