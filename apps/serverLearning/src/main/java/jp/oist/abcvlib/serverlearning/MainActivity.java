package jp.oist.abcvlib.serverlearning;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.flatbuffers.FlatBufferBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.audio.MicrophoneInput;
import jp.oist.abcvlib.core.inputs.vision.YuvToRgbConverter;
import jp.oist.abcvlib.core.learning.fbclasses.*;
import jp.oist.abcvlib.util.FileOps;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.SocketConnectionManager;

public class MainActivity extends AbcvlibActivity {

    private TimeStepDataBuffer timeStepDataBuffer;
    private MicrophoneInput microphoneInput;

    ScheduledExecutorService executor;
    ExecutorService imageExecutor;
    ScheduledExecutorService imageAnalysisExecutor;
    ImageAnalysis imageAnalysis;
    ScheduledFuture<?> wheelDataGathererFuture;
    ScheduledFuture<?> chargerDataGathererFuture;
    ScheduledFuture<?> batteryDataGathererFuture;
    ScheduledFuture<?> timeStepDataAssemblerFuture;
    WheelDataGatherer wheelDataGatherer;
    ChargerDataGatherer chargerDataGatherer;
    BatteryDataGatherer batteryDataGatherer;
    TimeStepDataAssembler timeStepDataAssembler;

    SocketConnectionManager socketConnectionManager;

    java.nio.ByteBuffer Fbuf;
    byte[] byteBuff;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup a live preview of camera feed to the display. Remove if unwanted.
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

//        switches.pythonControlledPIDBalancer = true;
        switches.cameraXApp = true;

        timeStepDataBuffer = new TimeStepDataBuffer(3);

        int threads = 2;
        executor = Executors.newScheduledThreadPool(threads, new ProcessPriorityThreadFactory(1, "dataGatherer"));
        imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(10, "imageAnalysis"));

        microphoneInput = new MicrophoneInput(this);

        executor.execute(socketConnectionManager = new SocketConnectionManager(this,"192.168.19.196", 3000));

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(2)
                        .build();
        imageAnalysis.setAnalyzer(imageExecutor, new ImageDataGatherer());

        //todo I guess the imageAnalyzerActivity Interface is uncessary
        initialzer(this, "192.168.0.108", 3000, null, this);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onSetupFinished(){
        ExecutorService sequentialExecutor = Executors.newSingleThreadExecutor();
        sequentialExecutor.submit(new Runnable() {
            @Override
            public void run() {
                wheelDataGatherer = new WheelDataGatherer();
                chargerDataGatherer = new ChargerDataGatherer();
                batteryDataGatherer = new BatteryDataGatherer();
                timeStepDataAssembler = new TimeStepDataAssembler();
//        testFlatBuffers();

                long initDelay = 5000;
                wheelDataGathererFuture = executor.scheduleAtFixedRate(wheelDataGatherer, initDelay, 10, TimeUnit.MILLISECONDS);
                chargerDataGathererFuture = executor.scheduleAtFixedRate(new ChargerDataGatherer(), initDelay, 10, TimeUnit.MILLISECONDS);
                batteryDataGathererFuture = executor.scheduleAtFixedRate(new BatteryDataGatherer(), initDelay, 10, TimeUnit.MILLISECONDS);
                timeStepDataAssemblerFuture = executor.scheduleAtFixedRate(timeStepDataAssembler, initDelay,50, TimeUnit.MILLISECONDS);
                microphoneInput.start();
            }
        });
    }

//    private void testFlatBuffers(){
//        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
//
//        int ra1 = RobotAction.createRobotAction(builder, (byte) 5, (byte) 16);
//        int[] ra = {ra1};
//        int aV = TimeStep.createActionsVector(builder, ra);
//
//        int ts1 = TimeStep.createTimeStep(builder, aV);
//        int[] ts = {ts1};
//        int tsV = Episode.createTimestepsVector(builder, ts);
//
//        Episode.startEpisode(builder);
//        long[] timestamps = new long[]{1, 2, 3};
//        double[] left = new double[]{1,2,3};
//        double[] right = new double[]{1,2,3};
//
//        int ts = WheelCounts.createTimestampsVector(builder, timestamps);
//        int l = WheelCounts.createLeftVector(builder, left);
//        int r = WheelCounts.createRightVector(builder, right);
//        WheelCounts.createWheelCounts(builder, ts, l, r);
//        int episode = Episode.endEpisode(builder);
//
//        builder.finish(episode); // You could also call `Monster.finishMonsterBuffer(builder, orc);`.
//        // This must be called after `finish()`.
//        byteBuff = builder.sizedByteArray();
//        Log.i("alskdjasd", "I'm a breakpoint!");
//    }

    class WheelDataGatherer implements Runnable{
        @Override
        public void run() {
            double left = inputs.quadEncoders.getWheelCountL();
            double right = inputs.quadEncoders.getWheelCountR();
            timeStepDataBuffer.writeData.wheelCounts.put(left, right);
        }
    }

    class ChargerDataGatherer implements Runnable{
        @Override
        public void run() {
            timeStepDataBuffer.writeData.chargerData.put(inputs.battery.getVoltageCharger());
        }
    }

    class BatteryDataGatherer implements Runnable{
        @Override
        public void run() {
            timeStepDataBuffer.writeData.batteryData.put(inputs.battery.getVoltageBatt());
        }
    }

    class ImageDataGatherer implements ImageAnalysis.Analyzer{

        YuvToRgbConverter yuvToRgbConverter = new YuvToRgbConverter(getApplicationContext());

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

                // todo this is causing a memory leak and crashing.
                timeStepDataBuffer.writeData.imageData.add(timestamp, width, height, rgbVectors);
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

    class TimeStepDataAssembler implements Runnable{

        private int timeStepCount = 0;
        private int maxTimeStep = 40;
        private FlatBufferBuilder builder;
        private int[] timeStepVector = new int[maxTimeStep + 1];

        public TimeStepDataAssembler(){
            startEpisode();
        }

        public void startEpisode(){
            builder = new FlatBufferBuilder(1024);
        }

        public void addTimeStep(){

            int _wheelCounts = addWheelCounts();
            int _chargerData = addChargerData();
            int _batteryData = addBatteryData();
            int _soundData = addSoundData();
            int _imageData = addImageData();

            TimeStep.startTimeStep(builder);
            TimeStep.addWheelCounts(builder, _wheelCounts);
            TimeStep.addChargerData(builder, _chargerData);
            TimeStep.addBatteryData(builder, _batteryData);
            TimeStep.addSoundData(builder, _soundData);
            TimeStep.addImageData(builder, _imageData);
            int ts = TimeStep.endTimeStep(builder);
            timeStepVector[timeStepCount]  = ts;
        }

        private int addWheelCounts(){
            Log.i("flatbuff", "STEP wheelCount TimeStamps Length: " +
                    timeStepDataBuffer.readData.wheelCounts.getTimeStamps().length);
            int ts = WheelCounts.createTimestampsVector(builder,
                    timeStepDataBuffer.readData.wheelCounts.getTimeStamps());
            int left = WheelCounts.createLeftVector(builder,
                    timeStepDataBuffer.readData.wheelCounts.getLeft());
            int right = WheelCounts.createLeftVector(builder,
                    timeStepDataBuffer.readData.wheelCounts.getRight());
            return WheelCounts.createWheelCounts(builder, ts, left, right);
        }

        private int addChargerData(){
            Log.i("flatbuff", "STEP chargerData TimeStamps Length: " +
                    timeStepDataBuffer.readData.chargerData.getTimeStamps().length);
            int ts = WheelCounts.createTimestampsVector(builder,
                    timeStepDataBuffer.readData.chargerData.getTimeStamps());
            int voltage = WheelCounts.createLeftVector(builder,
                    timeStepDataBuffer.readData.chargerData.getVoltage());
            return ChargerData.createChargerData(builder, ts, voltage);
        }

        private int addBatteryData(){
            Log.i("flatbuff", "STEP batteryData TimeStamps Length: " +
                    timeStepDataBuffer.readData.batteryData.getTimeStamps().length);
            int ts = WheelCounts.createTimestampsVector(builder,
                    timeStepDataBuffer.readData.batteryData.getTimeStamps());
            int voltage = WheelCounts.createLeftVector(builder,
                    timeStepDataBuffer.readData.batteryData.getVoltage());
            return ChargerData.createChargerData(builder, ts, voltage);
        }

        private int addSoundData(){

            TimeStepDataBuffer.TimeStepData.SoundData soundData = timeStepDataBuffer.readData.soundData;

            Log.i("flatbuff", "Sound Data TotalSamples: " +
                    soundData.totalSamples);

            int _startTime = AudioTimestamp.createAudioTimestamp(builder,
                    soundData.startTime.framePosition,
                    soundData.startTime.nanoTime);
            int _endTime = AudioTimestamp.createAudioTimestamp(builder,
                    soundData.startTime.framePosition,
                    soundData.startTime.nanoTime);
            int _levels = SoundData.createLevelsVector(builder,
                    timeStepDataBuffer.readData.soundData.getLevels());

            SoundData.startSoundData(builder);
            SoundData.addStartTime(builder, _startTime);
            SoundData.addEndTime(builder, _endTime);
            SoundData.addTotalTime(builder, soundData.totalTime);
            SoundData.addSampleRate(builder, soundData.sampleRate);
            SoundData.addTotalSamples(builder, soundData.totalSamples);
            SoundData.addLevels(builder, _levels);
            int _soundData = SoundData.endSoundData(builder);

            return _soundData;
        }

        private int addImageData(){
            TimeStepDataBuffer.TimeStepData.ImageData imageData = timeStepDataBuffer.readData.imageData;

            // Offset for all image data to be returned from this method
            int _imageData = 0;

            int numOfImages = imageData.images.size();

            Log.i("flatbuff", numOfImages + " images gathered");
            Log.i("flatbuff", "Step:" + timeStepCount);

            int[] _images = new int[numOfImages];

            for (int i = 0; i < numOfImages ; i++){
                TimeStepDataBuffer.TimeStepData.ImageData.SingleImage image = imageData.images.get(i);

                int _r = Pixels.createRVector(builder, image.pixels.r);
                int _g = Pixels.createGVector(builder, image.pixels.g);
                int _b = Pixels.createBVector(builder, image.pixels.b);
                int _pixels = Pixels.createPixels(builder, _r, _g, _b);

                jp.oist.abcvlib.core.learning.fbclasses.Image.startImage(builder);
                jp.oist.abcvlib.core.learning.fbclasses.Image.addPixels(builder, _pixels);
                jp.oist.abcvlib.core.learning.fbclasses.Image.addTimestamp(builder, image.timestamp);
                jp.oist.abcvlib.core.learning.fbclasses.Image.addHeight(builder, image.height);
                jp.oist.abcvlib.core.learning.fbclasses.Image.addWidth(builder, image.width);
                int _image = jp.oist.abcvlib.core.learning.fbclasses.Image.endImage(builder);

                _images[i] = _image;
            }

            if (numOfImages > 0){
                int _images_offset = ImageData.createImagesVector(builder, _images);
                ImageData.startImageData(builder);
                ImageData.addImages(builder, _images_offset);
                _imageData = ImageData.endImageData(builder);
            }

            return _imageData;
        }

//        private int addActionData(){}


        public void endEpisode(){

            closeall();

            int ts = Episode.createTimestepsVector(builder, timeStepVector); //todo I think I need to add each timestep when it is generated rather than all at once? Is this the leak?
            Episode.startEpisode(builder);
            Episode.addTimesteps(builder, ts);
            int ep = Episode.endEpisode(builder);
            builder.finish(ep);

            byte[] episode = builder.sizedByteArray();

            // The following is just to check the contents of the flatbuffer prior to sending to the server. You should comment this out if not using it as it doubles the required memory.
//            Episode episodeTest = Episode.getRootAsEpisode(episode);
//            Log.d("flatbuff", "TimeSteps Length: "  + String.valueOf(episodeTest.timestepsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 0 Length: "  + String.valueOf(episodeTest.timesteps(0).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 1 Length: "  + String.valueOf(episodeTest.timesteps(1).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 2 Length: "  + String.valueOf(episodeTest.timesteps(2).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 3 Length: "  + String.valueOf(episodeTest.timesteps(3).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 3 idx 0: "  + String.valueOf(episodeTest.timesteps(3).wheelCounts().timestamps(0)));

            boolean wroteToSendBuffer = sendToServer(episode);

            if (wroteToSendBuffer){
                builder.clear();
                builder = null;
            }

            try {
//                ActivityManager.getMyMemoryState();
                boolean deleted = false;
                boolean created = false;
                File file=new File(getFilesDir() + File.separator + "dump.hprof");
                if (file.exists()){
                    deleted = file.delete();
                }
                if (!file.exists() || deleted){
                    created = file.createNewFile();
                }
                Debug.dumpHprofData(file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }

//            Log.i("flatbuff", "prior to getting msg from server");
//            outputs.socketClient.getMessageFromServer();
//            Log.i("flatbuff", "after getting msg from server");
        }

        @Override
        public void run() {

            // Choose action based on current timestep data
            MyStepHandler myStepHandler = new MyStepHandler(timeStepDataBuffer.writeData, maxTimeStep);
            boolean lastEpisode = myStepHandler.foward(timeStepCount);

            assembleAudio();

            // Moves timeStepDataBuffer.writeData to readData and nulls out the writeData for new data
            timeStepDataBuffer.nextTimeStep();

            // Add timestep and return int representing offset in flatbuffer
            Log.i("flatbuff", "prior addTimeStep");
            addTimeStep();
            Log.i("flatbuff", "after addTimeStep");

            // If some criteria met, end episode.
            if (lastEpisode){
                endEpisode();
            }

            timeStepCount++;
        }

        public void assembleAudio(){
            // Don't put these inline, else you will pass by reference rather than value and references will continue to update
            android.media.AudioTimestamp startTime = microphoneInput.getStartTime();
            android.media.AudioTimestamp endTime = microphoneInput.getEndTime();
            int sampleRate = microphoneInput.getSampleRate();
            timeStepDataBuffer.writeData.soundData.setMetaData(sampleRate, startTime, endTime);

            microphoneInput.setStartTime();
        }

        public void closeall(){
            wheelDataGathererFuture.cancel(true);
            chargerDataGathererFuture.cancel(true);
            batteryDataGathererFuture.cancel(true);
            imageAnalysis.clearAnalyzer();
            microphoneInput.stop();
            microphoneInput.close();
        }
    }

    // Passes custom ImageAnalysis object to core CameraX lib to bind to lifecycle, and other admin functions
    @Override
    public ImageAnalysis getAnalyzer() {
        return imageAnalysis;
    }


    @Override
    protected void onNewAudioData(float[] audioData, int numSamples){
        timeStepDataBuffer.writeData.soundData.add(audioData, numSamples);
    }

    @Override
    public void onServerReadSuccess(JSONObject jsonHeader, ByteBuffer msgFromServer) {
        // Parse whatever you sent from python here
        //loadMappedFile...
        try {
            if (jsonHeader.get("content-encoding").equals("modelVector")){
                Log.d(TAG, "Writing model files to disk");
                int contentLength = (int) jsonHeader.get("content-length");
                JSONArray modelNames = (JSONArray) jsonHeader.get("model-names");
                JSONArray modelLengths = (JSONArray) jsonHeader.get("model-lengths");

                msgFromServer.flip();

                for (int i = 0; i < modelNames.length(); i++){
                    byte[] bytes = new byte[modelLengths.getInt(i)];
                    msgFromServer.get(bytes);
                    FileOps.savedata(this, bytes, modelNames.getString(i) + ".tflite");
                }

            }else{
                Log.d(TAG, "Data from server does not contain modelVector content. Be sure to set content-encoding to \"modelVector\" in the python jsonHeader");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Something wrong with parsing the JSONheader from python", e);
        }
    }

    private boolean sendToServer(byte[] episode){
        ActivityManager am = (ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        int mb = am.getMemoryClass();
        return socketConnectionManager.sendMsgToServer(episode);
    }
}
