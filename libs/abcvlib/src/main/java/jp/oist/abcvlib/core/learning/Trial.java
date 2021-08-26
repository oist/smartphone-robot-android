package jp.oist.abcvlib.core.learning;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.outputs.ActionSelector;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.FileOps;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;
import jp.oist.abcvlib.util.SocketListener;

public class Trial implements Runnable, ActionSelector, SocketListener {
    private Context context;
    private int timeStepLength = 50;
    private int maxTimeStepCount = 100;
    private int timeStep = 0;
    private boolean lastEpisode = false; // Use to trigger MainActivity to stop generating episodes
    private boolean lastTimestep = false; // Use to trigger MainActivity to stop generating timesteps for a single episode
    private int reward = 0;
    private int maxReward = 100;
    private int maxEpisodeCount = 3;
    private int episodeCount = 0;
    private final CommActionSet commActionSet;
    private final MotionActionSet motionActionSet;
    private final TimeStepDataBuffer timeStepDataBuffer;
    private final String TAG = getClass().toString();
    private ScheduledFuture<?> timeStepDataAssemblerFuture;
    private final ScheduledExecutorServiceWithException executor;
    private final ArrayList<AbcvlibInput> inputs;
    private FlatbufferAssembler flatbufferAssembler;

    public Trial(MetaParameters metaParameters, ActionSpace actionSpace,
                 StateSpace stateSpace){
        this.context = metaParameters.context;
        this.timeStepDataBuffer = metaParameters.timeStepDataBuffer;
        this.timeStepLength = metaParameters.timeStepLength;
        this.maxTimeStepCount = metaParameters.maxTimeStepCount;
        this.maxReward = metaParameters.maxReward;
        this.maxEpisodeCount = metaParameters.maxEpisodeCount;
        this.flatbufferAssembler = new FlatbufferAssembler(this,
                metaParameters.inetSocketAddress, this, timeStepDataBuffer);
        this.motionActionSet = actionSpace.motionActionSet;
        this.commActionSet = actionSpace.commActionSet;
        this.inputs = stateSpace.inputs;

        int threads = 5;
        executor = new ScheduledExecutorServiceWithException(threads, new ProcessPriorityThreadFactory(1, "trail"));
    }

    public void setFlatbufferAssembler(FlatbufferAssembler flatbufferAssembler){
        this.flatbufferAssembler = flatbufferAssembler;
    }
    
    protected void startTrail(){
        startGatherers();
    }

    protected void startEpisode(){
        if (flatbufferAssembler != null){
            flatbufferAssembler.startEpisode();
        }
    }

    protected void startGatherers() {
        CountDownLatch gatherersReady = new CountDownLatch(1);

        for (AbcvlibInput input:inputs){
            if (input != null){
                input.setRecording(true);
            }
        }

        timeStepDataAssemblerFuture = executor.scheduleAtFixedRate(this, 50, getTimeStepLength(), TimeUnit.MILLISECONDS);
        gatherersReady.countDown();
        try {
            gatherersReady.await();
        } catch (InterruptedException e) {
            ErrorHandler.eLog(TAG, "InterruptedException", e, true);
        }
    }

    @Override
    public void run() {
        // Choose action wte based on current timestep data
        forward(timeStepDataBuffer.getWriteData());

        // Moves timeStepDataBuffer.writeData to readData and nulls out the writeData for new data
        timeStepDataBuffer.nextTimeStep();

        // Add timestep and return int representing offset in flatbuffer
        flatbufferAssembler.addTimeStep();

        incrementTimeStep();

        // If some criteria met, end episode.
        if (isLastTimestep()){
            try {
                endEpisode();
                if(isLastEpisode()){
                    endTrail();
                }
            } catch (BrokenBarrierException | InterruptedException | IOException | RecordingWithoutTimeStepBufferException e) {
                ErrorHandler.eLog(TAG, "Error when trying to end episode or trail", e, true);
            }
        }
    }

    protected void stopRecordingData() throws RecordingWithoutTimeStepBufferException {

        for (AbcvlibInput input:inputs){
            if (input != null){
                input.setRecording(false);
            }
        }
        setTimeStep(0);
        setLastTimestep(false);
        timeStepDataAssemblerFuture.cancel(false);
    }

    // End episode after some reward has been acheived or maxtimesteps has been reached
    protected void endEpisode() throws BrokenBarrierException, InterruptedException, IOException, RecordingWithoutTimeStepBufferException {

        Log.d("Episode", "End of episode:" + getEpisodeCount());

        flatbufferAssembler.endEpisode();

        // Stop all gathering threads momentarily.
        stopRecordingData();
        incrementEpisodeCount();
        timeStepDataBuffer.nextTimeStep();

        flatbufferAssembler.sendToServer();

        startGatherers();
    }

    protected void endTrail() throws RecordingWithoutTimeStepBufferException {
        Log.i(TAG, "Need to handle end of trail here");
        stopRecordingData();
        for (AbcvlibInput input:inputs){
            if (input.getClass() == ImageData.class){
                ((ImageData) input).getImageAnalysis().clearAnalyzer();
            }else if (input.getClass() == MicrophoneData.class){
                ((MicrophoneData) input).close();
            }
        }
    }

    /**
     * This method is called at the end of each timestep within your class extending Trail and implementing ActionSelector
     * @param data All the data collected from the most recent timestep
     */
    public void forward(TimeStepDataBuffer.TimeStepData data){};

    @Override
    public void onServerReadSuccess(JSONObject jsonHeader, ByteBuffer msgFromServer) {
        // Parse whatever you sent from python here
        //loadMappedFile...
        try {
            if (jsonHeader.get("content-encoding").equals("modelVector")){
                Log.d(TAG, "Writing model files to disk");
                JSONArray modelNames = (JSONArray) jsonHeader.get("model-names");
                JSONArray modelLengths = (JSONArray) jsonHeader.get("model-lengths");

                msgFromServer.flip();

                for (int i = 0; i < modelNames.length(); i++){
                    byte[] bytes = new byte[modelLengths.getInt(i)];
                    msgFromServer.get(bytes);
                    FileOps.savedata(context, bytes, "models", modelNames.getString(i) + ".tflite");
                }
            }else{
                Log.d(TAG, "Data from server does not contain modelVector content. Be sure to set content-encoding to \"modelVector\" in the python jsonHeader");
            }
        } catch (JSONException e) {
            ErrorHandler.eLog(TAG, "Something wrong with parsing the JSONheader from python", e, true);
        }
    }

    public int getTimeStepLength() {
        return timeStepLength;
    }

    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public int getTimeStep() {
        return timeStep;
    }

    public boolean isLastEpisode() {
        return (episodeCount >= maxEpisodeCount) | lastEpisode;
    }

    public boolean isLastTimestep() {
        return (timeStep >= maxTimeStepCount) | lastTimestep;
    }

    public int getMaxEpisodecount() {
        return maxEpisodeCount;
    }

    public int getMaxTimeStepCount() {
        return maxTimeStepCount;
    }

    public int getReward() {
        return reward;
    }

    public int getMaxReward() {
        return maxReward;
    }

    public void incrementEpisodeCount() {
        episodeCount++;
    }

    public void incrementTimeStep(){timeStep++;}

    public void setTimeStep(int timeStep) {
        this.timeStep = timeStep;
    }

    public void setLastEpisode(boolean lastEpisode) {
        this.lastEpisode = lastEpisode;
    }

    public MotionActionSet getMotionActionSet() {
        return motionActionSet;
    }

    public CommActionSet getCommActionSet() {
        return commActionSet;
    }

    public void setLastTimestep(boolean lastTimestep) {
        this.lastTimestep = lastTimestep;
    }
}
