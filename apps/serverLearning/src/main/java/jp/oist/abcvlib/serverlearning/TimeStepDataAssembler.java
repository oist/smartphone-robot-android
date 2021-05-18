package jp.oist.abcvlib.serverlearning;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.ImageAnalysis;

import com.google.flatbuffers.FlatBufferBuilder;

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
import jp.oist.abcvlib.core.inputs.phone.audio.MicrophoneInput;
import jp.oist.abcvlib.core.learning.fbclasses.AudioTimestamp;
import jp.oist.abcvlib.core.learning.fbclasses.ChargerData;
import jp.oist.abcvlib.core.learning.fbclasses.Episode;
import jp.oist.abcvlib.core.learning.fbclasses.ImageData;
import jp.oist.abcvlib.core.learning.fbclasses.RobotAction;
import jp.oist.abcvlib.core.learning.fbclasses.SoundData;
import jp.oist.abcvlib.core.learning.fbclasses.TimeStep;
import jp.oist.abcvlib.core.learning.fbclasses.WheelCounts;
import jp.oist.abcvlib.serverlearning.gatherers.BatteryDataGatherer;
import jp.oist.abcvlib.serverlearning.gatherers.ImageDataGatherer;
import jp.oist.abcvlib.serverlearning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.serverlearning.gatherers.WheelDataGatherer;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.SocketConnectionManager;

public class TimeStepDataAssembler implements Runnable{

    private int timeStepCount = 0;
    private final int maxTimeStep = 100;
    private FlatBufferBuilder builder;
    private int[] timeStepVector = new int[maxTimeStep + 1];
    private MyStepHandler myStepHandler;
    private int episodeCount = 0;
    private TimeStepDataBuffer timeStepDataBuffer;
    private String TAG = getClass().toString();
    private MicrophoneInput microphoneInput;
    private boolean pauseRecording = false;
    private ScheduledFuture<?> wheelDataGathererFuture;
    private ScheduledFuture<?> batteryDataGathererFuture;
    private ScheduledFuture<?> timeStepDataAssemblerFuture;
    private WheelDataGatherer wheelDataGatherer;
    private ChargerDataGatherer chargerDataGatherer;
    private BatteryDataGatherer batteryDataGatherer;
    private ImageDataGatherer imageDataGatherer;
    private SocketConnectionManager socketConnectionManager;
    private ScheduledExecutorService executor;
    private ExecutorService imageExecutor;
    private InetSocketAddress inetSocketAddress;
    private ImageAnalysis imageAnalysis;
    private AbcvlibActivity abcvlibActivity;

    public TimeStepDataAssembler(AbcvlibActivity abcvlibActivity,
                                 InetSocketAddress inetSocketAddress){
        this.abcvlibActivity = abcvlibActivity;
        this.inetSocketAddress = inetSocketAddress;
        microphoneInput = new MicrophoneInput(abcvlibActivity);

        timeStepDataBuffer = new TimeStepDataBuffer(10);

        int threads = 5;
        executor = Executors.newScheduledThreadPool(threads, new ProcessPriorityThreadFactory(1, "dataGatherer"));
        imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "imageAnalysis"));

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(20)
                        .build();

        myStepHandler = new MyStepHandler(abcvlibActivity.getApplicationContext(), maxTimeStep,
                10000, 10);

        startEpisode();
    }

    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }

    public ImageAnalysis getImageAnalysis() {
        return imageAnalysis;
    }

    public void startEpisode(){
//            ByteBuffer bb = ByteBuffer.allocateDirect(4096);
        builder = new FlatBufferBuilder(1024);
        Log.v("flatbuff", "starting New Episode");
        // todo reload tflite models here for myStepHandler
    }

    protected void startGatherers() throws InterruptedException {
        CountDownLatch gatherersReady = new CountDownLatch(1);

        WheelDataGatherer wheelDataGatherer = new WheelDataGatherer(abcvlibActivity, timeStepDataBuffer);
        BatteryDataGatherer batteryDataGatherer = new BatteryDataGatherer(timeStepDataBuffer);
        ImageDataGatherer imageDataGatherer = new ImageDataGatherer(abcvlibActivity, timeStepDataBuffer);

        Log.d("SocketConnection", "Starting new runnable for gatherers");

        long initDelay = 0;
        microphoneInput.start();
        imageAnalysis.setAnalyzer(imageExecutor, imageDataGatherer);
        wheelDataGathererFuture = executor.scheduleAtFixedRate(wheelDataGatherer, initDelay, 10, TimeUnit.MILLISECONDS);
        batteryDataGathererFuture = executor.scheduleAtFixedRate(batteryDataGatherer, initDelay, 10, TimeUnit.MILLISECONDS);
        timeStepDataAssemblerFuture = executor.scheduleAtFixedRate(this, 50,50, TimeUnit.MILLISECONDS);
        gatherersReady.countDown();
        Log.d("SocketConnection", "Waiting for gatherers to finish");
        gatherersReady.await();
        Log.d("SocketConnection", "Gatherers finished initializing");
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
                timeStepDataBuffer.getReadData().getWheelCounts().getTimeStamps().length);
        int ts = WheelCounts.createTimestampsVector(builder,
                timeStepDataBuffer.getReadData().getWheelCounts().getTimeStamps());
        int left = WheelCounts.createLeftVector(builder,
                timeStepDataBuffer.getReadData().getWheelCounts().getLeft());
        int right = WheelCounts.createLeftVector(builder,
                timeStepDataBuffer.getReadData().getWheelCounts().getRight());
        return WheelCounts.createWheelCounts(builder, ts, left, right);
    }

    private int addChargerData(){
        Log.v("flatbuff", "STEP chargerData TimeStamps Length: " +
                timeStepDataBuffer.getReadData().getChargerData().getTimeStamps().length);
        int ts = WheelCounts.createTimestampsVector(builder,
                timeStepDataBuffer.getReadData().getChargerData().getTimeStamps());
        int voltage = WheelCounts.createLeftVector(builder,
                timeStepDataBuffer.getReadData().getChargerData().getVoltage());
        return ChargerData.createChargerData(builder, ts, voltage);
    }

    private int addBatteryData(){
        Log.v("flatbuff", "STEP batteryData TimeStamps Length: " +
                timeStepDataBuffer.getReadData().getBatteryData().getTimeStamps().length);
        int ts = WheelCounts.createTimestampsVector(builder,
                timeStepDataBuffer.getReadData().getBatteryData().getTimeStamps());
        int voltage = WheelCounts.createLeftVector(builder,
                timeStepDataBuffer.getReadData().getBatteryData().getVoltage());
        return ChargerData.createChargerData(builder, ts, voltage);
    }

    private int addSoundData(){

        TimeStepDataBuffer.TimeStepData.SoundData soundData = timeStepDataBuffer.getReadData().getSoundData();

        Log.v("flatbuff", "Sound Data TotalSamples: " +
                soundData.getTotalSamples());

        int _startTime = AudioTimestamp.createAudioTimestamp(builder,
                soundData.getStartTime().framePosition,
                soundData.getStartTime().nanoTime);
        int _endTime = AudioTimestamp.createAudioTimestamp(builder,
                soundData.getStartTime().framePosition,
                soundData.getStartTime().nanoTime);
        int _levels = SoundData.createLevelsVector(builder,
                timeStepDataBuffer.getReadData().getSoundData().getLevels());

        SoundData.startSoundData(builder);
        SoundData.addStartTime(builder, _startTime);
        SoundData.addEndTime(builder, _endTime);
        SoundData.addTotalTime(builder, soundData.getTotalTime());
        SoundData.addSampleRate(builder, soundData.getSampleRate());
        SoundData.addTotalSamples(builder, soundData.getTotalSamples());
        SoundData.addLevels(builder, _levels);

        return SoundData.endSoundData(builder);
    }

    private int addImageData(){
        TimeStepDataBuffer.TimeStepData.ImageData imageData = timeStepDataBuffer.getReadData().getImageData();

        // Offset for all image data to be returned from this method
        int _imageData = 0;

        int numOfImages = imageData.getImages().size();

        Log.v("flatbuff", numOfImages + " images gathered");
        Log.v("flatbuff", "Step:" + timeStepCount);

        int[] _images = new int[numOfImages];

        for (int i = 0; i < numOfImages ; i++){
            TimeStepDataBuffer.TimeStepData.ImageData.SingleImage image = imageData.getImages().get(i);

            int _webpImage = jp.oist.abcvlib.core.learning.fbclasses.Image.createWebpImageVector(builder, image.getWebpImage());
            jp.oist.abcvlib.core.learning.fbclasses.Image.startImage(builder);
            jp.oist.abcvlib.core.learning.fbclasses.Image.addWebpImage(builder, _webpImage);
            jp.oist.abcvlib.core.learning.fbclasses.Image.addTimestamp(builder, image.getTimestamp());
            jp.oist.abcvlib.core.learning.fbclasses.Image.addHeight(builder, image.getHeight());
            jp.oist.abcvlib.core.learning.fbclasses.Image.addWidth(builder, image.getWidth());
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
        CommAction ca = timeStepDataBuffer.getReadData().getActions().getCommAction();
        MotionAction ma = timeStepDataBuffer.getReadData().getActions().getMotionAction();
        Log.v("flatbuff", "CommAction : " + ca.getActionNumber());
        Log.v("flatbuff", "MotionAction : " + ma.getActionName());

        return RobotAction.createRobotAction(builder, (byte) ca.getActionNumber(), (byte) ma.getActionByte());
    }

    @Override
    public void run() {

        // Choose action wte based on current timestep data
        ActionSet actionSet = myStepHandler.forward(timeStepDataBuffer.getWriteData(), timeStepCount);

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
        timeStepDataBuffer.getWriteData().getSoundData().setMetaData(sampleRate, startTime, endTime);

        microphoneInput.setStartTime();
    }

    protected void startGatherers() throws InterruptedException {
        CountDownLatch gatherersReady = new CountDownLatch(1);

        wheelDataGatherer = new WheelDataGatherer(abcvlibActivity, timeStepDataBuffer);
        chargerDataGatherer = new ChargerDataGatherer(abcvlibActivity, timeStepDataBuffer);
        batteryDataGatherer = new BatteryDataGatherer(abcvlibActivity, timeStepDataBuffer);
        imageDataGatherer = new ImageDataGatherer(abcvlibActivity, timeStepDataBuffer);

        ExecutorService sequentialExecutor = Executors.newSingleThreadExecutor();
        Log.d("SocketConnection", "Starting new runnable for gatherers");

        long initDelay = 0;
        microphoneInput.start();
        imageAnalysis.setAnalyzer(imageExecutor, imageDataGatherer);
        wheelDataGathererFuture = executor.scheduleAtFixedRate(wheelDataGatherer, initDelay, 10, TimeUnit.MILLISECONDS);
        chargerDataGathererFuture = executor.scheduleAtFixedRate(chargerDataGatherer, initDelay, 10, TimeUnit.MILLISECONDS);
        batteryDataGathererFuture = executor.scheduleAtFixedRate(batteryDataGatherer, initDelay, 10, TimeUnit.MILLISECONDS);
        timeStepDataAssemblerFuture = executor.scheduleAtFixedRate(this, 50,50, TimeUnit.MILLISECONDS);
        gatherersReady.countDown();
        Log.d("SocketConnection", "Waiting for gatherers to finish");
        gatherersReady.await();
        Log.d("SocketConnection", "Gatherers finished initializing");
    }

    public void stopRecordingData(){

        pauseRecording = true;

        wheelDataGathererFuture.cancel(true);
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
        CyclicBarrier doneSignal = new CyclicBarrier(2, new CyclicBarrierHandler(this){

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

    private class CyclicBarrierHandler implements Runnable {

        private TimeStepDataAssembler timeStepDataAssembler;

        public CyclicBarrierHandler(TimeStepDataAssembler timeStepDataAssembler){
            this.timeStepDataAssembler = timeStepDataAssembler;
        }

        @Override
        public void run() {
            builder.clear();
            builder = null;
            timeStepDataAssembler.startEpisode();
        }
    }

    private void endTrail(){
        Log.i(TAG, "Need to handle end of trail here");
        episodeCount = 0;
        stopRecordingData();
        microphoneInput.close();
    }

    private void sendToServer(ByteBuffer episode, CyclicBarrier doneSignal) throws IOException {
        Log.d("SocketConnection", "New executor deployed creating new SocketConnectionManager");
        executor.execute(new SocketConnectionManager(abcvlibActivity, inetSocketAddress, episode, doneSignal));
    }
}

