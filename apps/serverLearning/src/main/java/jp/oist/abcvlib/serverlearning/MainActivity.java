package jp.oist.abcvlib.serverlearning;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.flatbuffers.FlatBufferBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.audio.MicrophoneInput;
import jp.oist.abcvlib.core.inputs.vision.YuvToRgbConverter;
import jp.oist.abcvlib.core.learning.fbclasses.*;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.FileOps;
import jp.oist.abcvlib.util.ImageOps;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;
import jp.oist.abcvlib.util.SocketConnectionManager;

public class MainActivity extends AbcvlibActivity {

    private TimeStepDataBuffer timeStepDataBuffer;
    private MicrophoneInput microphoneInput;

    ScheduledExecutorServiceWithException executor;
    ExecutorService imageExecutor;
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
    InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.2.102", 3000);
    private boolean pauseRecording = false;

    java.nio.ByteBuffer Fbuf;
    byte[] byteBuff;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        FileOps.copyAssets(getApplicationContext(), "models/");

        // Setup a live preview of camera feed to the display. Remove if unwanted.
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

//        switches.pythonControlledPIDBalancer = true;
        switches.cameraXApp = true;

        timeStepDataBuffer = new TimeStepDataBuffer(10);

        int threads = 5;
        executor = new ScheduledExecutorServiceWithException(threads, new ProcessPriorityThreadFactory(1, "dataGatherer"));
        imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "imageAnalysis"));

        microphoneInput = new MicrophoneInput(this);

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(20)
                        .build();
        imageAnalysis.setAnalyzer(imageExecutor, new ImageDataGatherer());

        initialzer(this, null, this);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSetupFinished() {
        wheelDataGatherer = new WheelDataGatherer();
        chargerDataGatherer = new ChargerDataGatherer();
        batteryDataGatherer = new BatteryDataGatherer();
        timeStepDataAssembler = new TimeStepDataAssembler();
        try {
            startGatherers();
        } catch (InterruptedException e) {
            ErrorHandler.eLog(TAG, "Error starting gathers", e, true);
        }
    }

    protected void startGatherers() throws InterruptedException {
        CountDownLatch gatherersReady = new CountDownLatch(1);
        ExecutorService sequentialExecutor = Executors.newSingleThreadExecutor();

        Log.d("SocketConnection", "Starting new runnable for gatherers");

        sequentialExecutor.submit(new Runnable() {
            @Override
            public void run() {
                long initDelay = 0;
                microphoneInput.start();
                imageAnalysis.setAnalyzer(imageExecutor, new ImageDataGatherer());
                wheelDataGathererFuture = executor.scheduleAtFixedRate(wheelDataGatherer, initDelay, 10, TimeUnit.MILLISECONDS);
                chargerDataGathererFuture = executor.scheduleAtFixedRate(new ChargerDataGatherer(), initDelay, 10, TimeUnit.MILLISECONDS);
                batteryDataGathererFuture = executor.scheduleAtFixedRate(new BatteryDataGatherer(), initDelay, 10, TimeUnit.MILLISECONDS);
                timeStepDataAssemblerFuture = executor.scheduleAtFixedRate(timeStepDataAssembler, 50,50, TimeUnit.MILLISECONDS);
                gatherersReady.countDown();
            }
        });
        Log.d("SocketConnection", "Waiting for gatherers to finish");
        gatherersReady.await();
        Log.d("SocketConnection", "Gatherers finished initializing");

    }

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

                ByteArrayOutputStream webpByteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.WEBP, 0, webpByteArrayOutputStream);
                byte[] webpBytes = webpByteArrayOutputStream.toByteArray();
                Bitmap webpBitMap = ImageOps.generateBitmap(webpBytes);

                // todo this is causing a memory leak and crashing.
                timeStepDataBuffer.writeData.imageData.add(timestamp, width, height, webpBitMap, webpBytes);
                Log.v("flatbuff", "Wrote image to timeStepDataBuffer");
            }
            imageProxy.close();
        }
    }

    class TimeStepDataAssembler implements Runnable{

        private int timeStepCount = 0;
        private final int maxTimeStep = 100;
        private FlatBufferBuilder builder;
        private int[] timeStepVector = new int[maxTimeStep + 1];
        private MyStepHandler myStepHandler;
        private int episodeCount = 0;

        public TimeStepDataAssembler(){
            myStepHandler = new MyStepHandler(getApplicationContext(), maxTimeStep, 10000, 10);
            startEpisode();
        }

        public void startEpisode(){
//            ByteBuffer bb = ByteBuffer.allocateDirect(4096);
            builder = new FlatBufferBuilder(1024);
            Log.v("flatbuff", "starting New Episode");
            // todo reload tflite models here for myStepHandler
        }

        public void addTimeStep(){

            int _wheelCounts = addWheelCounts();
            int _chargerData = addChargerData();
            int _batteryData = addBatteryData();
            int _soundData = addSoundData();
            int _imageData = addImageData();
            int _actionData = addActionData();

            TimeStep.startTimeStep(builder);
            TimeStep.addWheelCounts(builder, _wheelCounts);
            TimeStep.addChargerData(builder, _chargerData);
            TimeStep.addBatteryData(builder, _batteryData);
            TimeStep.addSoundData(builder, _soundData);
            TimeStep.addImageData(builder, _imageData);
            TimeStep.addActions(builder, _actionData);
            int ts = TimeStep.endTimeStep(builder);
            timeStepVector[timeStepCount]  = ts;
        }

        private int addWheelCounts(){
            Log.v("flatbuff", "STEP wheelCount TimeStamps Length: " +
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
            Log.v("flatbuff", "STEP chargerData TimeStamps Length: " +
                    timeStepDataBuffer.readData.chargerData.getTimeStamps().length);
            int ts = WheelCounts.createTimestampsVector(builder,
                    timeStepDataBuffer.readData.chargerData.getTimeStamps());
            int voltage = WheelCounts.createLeftVector(builder,
                    timeStepDataBuffer.readData.chargerData.getVoltage());
            return ChargerData.createChargerData(builder, ts, voltage);
        }

        private int addBatteryData(){
            Log.v("flatbuff", "STEP batteryData TimeStamps Length: " +
                    timeStepDataBuffer.readData.batteryData.getTimeStamps().length);
            int ts = WheelCounts.createTimestampsVector(builder,
                    timeStepDataBuffer.readData.batteryData.getTimeStamps());
            int voltage = WheelCounts.createLeftVector(builder,
                    timeStepDataBuffer.readData.batteryData.getVoltage());
            return ChargerData.createChargerData(builder, ts, voltage);
        }

        private int addSoundData(){

            TimeStepDataBuffer.TimeStepData.SoundData soundData = timeStepDataBuffer.readData.soundData;

            Log.v("flatbuff", "Sound Data TotalSamples: " +
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

            Log.v("flatbuff", numOfImages + " images gathered");
            Log.v("flatbuff", "Step:" + timeStepCount);

            int[] _images = new int[numOfImages];

            for (int i = 0; i < numOfImages ; i++){
                TimeStepDataBuffer.TimeStepData.ImageData.SingleImage image = imageData.images.get(i);

                int _webpImage = jp.oist.abcvlib.core.learning.fbclasses.Image.createWebpImageVector(builder, image.webpImage);
                jp.oist.abcvlib.core.learning.fbclasses.Image.startImage(builder);
                jp.oist.abcvlib.core.learning.fbclasses.Image.addWebpImage(builder, _webpImage);
                jp.oist.abcvlib.core.learning.fbclasses.Image.addTimestamp(builder, image.timestamp);
                jp.oist.abcvlib.core.learning.fbclasses.Image.addHeight(builder, image.height);
                jp.oist.abcvlib.core.learning.fbclasses.Image.addWidth(builder, image.width);
                int _image = jp.oist.abcvlib.core.learning.fbclasses.Image.endImage(builder);

                _images[i] = _image;
            }

            int _images_offset = ImageData.createImagesVector(builder, _images);
            ImageData.startImageData(builder);
            ImageData.addImages(builder, _images_offset);
            _imageData = ImageData.endImageData(builder);

            return _imageData;
        }

        private int addActionData(){
            CommAction ca = timeStepDataBuffer.readData.actions.getCommAction();
            MotionAction ma = timeStepDataBuffer.readData.actions.getMotionAction();
            Log.v("flatbuff", "CommAction : " + ca.getActionNumber());
            Log.v("flatbuff", "MotionAction : " + ma.getActionName());

            return RobotAction.createRobotAction(builder, (byte) ca.getActionNumber(), (byte) ma.getActionByte());
        }

        @Override
        public void run() {

            // Choose action wte based on current timestep data
            ActionSet actionSet = myStepHandler.forward(timeStepDataBuffer.writeData, timeStepCount);

            Log.v("SocketConnection", "Running TimeStepAssembler Run Method");

            assembleAudio();

            // Moves timeStepDataBuffer.writeData to readData and nulls out the writeData for new data
            timeStepDataBuffer.nextTimeStep();

            // Add timestep and return int representing offset in flatbuffer
            addTimeStep();

            timeStepCount++;

            // If some criteria met, end episode.
            if (myStepHandler.isLastTimestep()){
                try {
                    endEpisode();
                    if(myStepHandler.isLastEpisode()){
                        endTrail();
                    }
                } catch (BrokenBarrierException | InterruptedException | IOException e) {
                    ErrorHandler.eLog(TAG, "Error when trying to end episode or trail", e, true);
                }
            }
        }

        public void assembleAudio(){
            // Don't put these inline, else you will pass by reference rather than value and references will continue to update
            // todo between episodes the startTime is larger than the end time somehow.
            android.media.AudioTimestamp startTime = microphoneInput.getStartTime();
            android.media.AudioTimestamp endTime = microphoneInput.getEndTime();
            int sampleRate = microphoneInput.getSampleRate();
            timeStepDataBuffer.writeData.soundData.setMetaData(sampleRate, startTime, endTime);

            microphoneInput.setStartTime();
        }

        public void stopRecordingData(){

            pauseRecording = true;

            wheelDataGathererFuture.cancel(true);
            chargerDataGathererFuture.cancel(true);
            batteryDataGathererFuture.cancel(true);
            imageAnalysis.clearAnalyzer();
            microphoneInput.stop();
            timeStepCount = 0;
            myStepHandler.setLastTimestep(false);
            timeStepDataAssemblerFuture.cancel(false);
        }

        // End episode after some reward has been acheived or maxtimesteps has been reached
        public void endEpisode() throws BrokenBarrierException, InterruptedException, IOException {

            Log.d("SocketConnections", "End of episode:" + episodeCount);

            int ts = Episode.createTimestepsVector(builder, timeStepVector); //todo I think I need to add each timestep when it is generated rather than all at once? Is this the leak?
            Episode.startEpisode(builder);
            Episode.addTimesteps(builder, ts);
            int ep = Episode.endEpisode(builder);
            builder.finish(ep);

//            byte[] episode = builder.sizedByteArray();
            ByteBuffer episode = builder.dataBuffer();

            // Stop all gathering threads momentarily.
            stopRecordingData();
            timeStepDataBuffer.nextTimeStep();

//             The following is just to check the contents of the flatbuffer prior to sending to the server.
//             You should comment this out if not using it as it doubles the required memory.
//            Also it seems the getRootAsEpisode modifes the episode buffer itself, thus messing up later processing.
//            Therefore I propose only using this as an inline debugging step or if you don't want
//            To evaluate anything past this point for a given run.

//            Episode episodeTest = Episode.getRootAsEpisode(episode);
//            Log.d("flatbuff", "TimeSteps Length: "  + String.valueOf(episodeTest.timestepsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 0 Length: "  + String.valueOf(episodeTest.timesteps(0).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 1 Length: "  + String.valueOf(episodeTest.timesteps(1).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 2 Length: "  + String.valueOf(episodeTest.timesteps(2).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 3 Length: "  + String.valueOf(episodeTest.timesteps(3).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 3 idx 0: "  + String.valueOf(episodeTest.timesteps(3).wheelCounts().timestamps(0)));
//            Log.d("flatbuff", "Levels Length TimeStep 100: "  + String.valueOf(episodeTest.timesteps(100).soundData().levelsLength()));
//            Log.d("flatbuff", "SoundData ByteBuffer Length TimeStep 100: "  + String.valueOf(episodeTest.timesteps(100).soundData().getByteBuffer().capacity()));
//            Log.d("flatbuff", "ImageData ByteBuffer Length TimeStep 100: "  + String.valueOf(episodeTest.timesteps(100).imageData().getByteBuffer().capacity()));


//            float[] soundFloats = new float[10];
//            episodeTest.timesteps(1).soundData().levelsAsByteBuffer().asFloatBuffer().get(soundFloats);
//            Log.d("flatbuff", "Sound TimeStep 1 as numpy: "  + Arrays.toString(soundFloats));

            // Sets action to take after server has recevied and sent back data completely
            CyclicBarrier doneSignal = new CyclicBarrier(2, new Runnable() {
                @Override
                public void run() {
                    builder.clear();
                    builder = null;
                    timeStepDataAssembler.startEpisode();
                }
            });

            // Todo this is getting stuck on registering to the selector likely because selector is running in another thread?
            sendToServer(episode, doneSignal);

            // Waits for server to finish, then the runnable set in the init of doneSignal above will be fired.
            Log.d("SocketConnection", "Waiting for socket transfer R/W to complete.");
            doneSignal.await();
            Log.d("SocketConnection", "Finished waiting for socket connection. Starting gatherers again.");

            startGatherers();

            episodeCount++;

            // Wait for transfer to server and return message received

//            Log.i("flatbuff", "prior to getting msg from server");
//            outputs.socketClient.getMessageFromServer();
//            Log.i("flatbuff", "after getting msg from server");
        }

        private void endTrail(){
            Log.i(TAG, "Need to handle end of trail here");
            episodeCount = 0;
            stopRecordingData();
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
                    FileOps.savedata(this, bytes, "models", modelNames.getString(i) + ".tflite");
                }

            }else{
                Log.d(TAG, "Data from server does not contain modelVector content. Be sure to set content-encoding to \"modelVector\" in the python jsonHeader");
            }
        } catch (JSONException e) {
            ErrorHandler.eLog(TAG, "Something wrong with parsing the JSONheader from python", e, true);

        }
    }

    private void sendToServer(ByteBuffer episode, CyclicBarrier doneSignal) throws IOException {
        ActivityManager am = (ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        int mb = am.getMemoryClass();
        Log.d("SocketConnection", "New executor deployed creating new SocketConnectionManager");
        executor.execute(new SocketConnectionManager(this, inetSocketAddress, episode, doneSignal));
    }

}
