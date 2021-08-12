package jp.oist.abcvlib.core.inputs.phone;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.RequiresApi;

import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

public class MicrophoneData implements AudioRecord.OnRecordPositionUpdateListener, AbcvlibInput {

    private AudioTimestamp startTime = new AudioTimestamp();
    private AudioTimestamp endTime = new AudioTimestamp();
    private ScheduledExecutorServiceWithException audioExecutor;

    private AudioRecord recorder;
    private boolean isRecording = false;
    private TimeStepDataBuffer timeStepDataBuffer;
    private MicrophoneDataListener microphoneDataListener = null;

    /**
     * Lazy start. This constructor sets everything up, but you must call {@link #start()} for the
     * buffer to begin filling.
     */
    public MicrophoneData(TimeStepDataBuffer timeStepDataBuffer) {

        Log.i("abcvlib", "In MicInput run method");

        this.timeStepDataBuffer = timeStepDataBuffer;

        audioExecutor = new ScheduledExecutorServiceWithException(1, new ProcessPriorityThreadFactory(10, "dataGatherer"));
        HandlerThread handlerThread = new HandlerThread("audioHandlerThread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        int mAudioSource = MediaRecorder.AudioSource.UNPROCESSED;
        int mSampleRate = 8000;
        int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
        int mAudioFormat = AudioFormat.ENCODING_PCM_FLOAT;
        int bufferSize = 3 * AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, // needed to be 3 or more times or would internally increase it within Native lib.
                mAudioFormat);

        try{
            recorder = new AudioRecord(
                    mAudioSource,
                    mSampleRate,
                    mChannelConfig,
                    mAudioFormat,
                    bufferSize);
        }catch (SecurityException e){
            throw new RuntimeException("You must grant audio record access to use this app");
        }

        int bytesPerSample = 32 / 8; // 32 bits per sample (Float.size), 8 bytes per bit.
        int bytesPerFrame = bytesPerSample * recorder.getChannelCount(); // Need this as setPositionNotificationPeriod takes num of frames as period and you want it to fire after each full cycle through the buffer.
        int framePerBuffer = bufferSize / bytesPerFrame; // # of frames that can be kept in a bufferSize dimension
        int framePeriod = framePerBuffer / 2; // Read from buffer two times per full buffer.
        recorder.setPositionNotificationPeriod(framePeriod);
        recorder.setRecordPositionUpdateListener(this, handler);
    }

    public MicrophoneData() {
        this(null);
    }


    public void start(){
        recorder.startRecording();

        while (recorder.getTimestamp(startTime, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.ERROR_INVALID_OPERATION){
//            Log.i("microphone_start", "waiting for start timestamp");
            int successOrNot = recorder.getTimestamp(startTime, AudioTimestamp.TIMEBASE_MONOTONIC);
//            Log.i("microphone_start", "successOrNot (0=success, -3=invalid-op):" + successOrNot);
        }
        Log.i("microphone_start", "StartFrame:" + startTime.framePosition + " NanoTime: " + startTime.nanoTime);
    }
    
    public AudioTimestamp getStartTime(){return startTime;}

    public AudioRecord getRecorder() {
        return recorder;
    }

    public void setStartTime(){
        recorder.getTimestamp(startTime, AudioTimestamp.TIMEBASE_MONOTONIC);
//        startTime = endTime;
    }

    public AudioTimestamp getEndTime(){
        recorder.getTimestamp(endTime, AudioTimestamp.TIMEBASE_MONOTONIC);
        return endTime;
    }

    public int getSampleRate(){return recorder.getSampleRate();}

    public void stop(){
        recorder.stop();
    }

    public void close(){
        recorder.setRecordPositionUpdateListener(null);
        audioExecutor.shutdownNow();
        recorder.release();
        recorder = null;
    }

    public synchronized void setRecording(boolean recording) throws RecordingWithoutTimeStepBufferException {
        if (this.timeStepDataBuffer == null){
            throw new RecordingWithoutTimeStepBufferException();
        }
        isRecording = recording;
    }

    @Override
    public void setTimeStepDataBuffer(TimeStepDataBuffer timeStepDataBuffer) {
        this.timeStepDataBuffer = timeStepDataBuffer;
    }

    public void setMicrophoneDataListener(MicrophoneDataListener microphoneDataListener) {
        this.microphoneDataListener = microphoneDataListener;
    }

    @Override
    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }

    public synchronized boolean isRecording() {
        return isRecording;
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {

    }

    /**
     * This method fires 2 times during each loop of the audio record buffer.
     * audioRecord.read(audioData) writes the buffer values (stored in the audioRecord) to a local
     * float array called audioData. It is set to read in non_blocking mode
     * (https://developer.android.com/reference/android/media/AudioRecord?hl=ja#READ_NON_BLOCKING)
     * You can verify it is not blocking by checking the log for "Missed some audio samples"
     * You can verify if the buffer writer is overflowing by checking the log for:
     * "W/AudioFlinger: RecordThread: buffer overflow"
     * @param audioRecord
     */
    @Override
    public void onPeriodicNotification(AudioRecord audioRecord) {
        try{
            audioExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    int writeBufferSizeFrames = audioRecord.getBufferSizeInFrames();
                    int readBufferSize = audioRecord.getPositionNotificationPeriod();
//                Log.d("microphone", "readBufferSize:" + readBufferSize);
                    float[] audioData = new float[readBufferSize];
                    int numSamples = audioRecord.read(audioData, 0,
                            readBufferSize, AudioRecord.READ_NON_BLOCKING);
//                Log.d("microphone", "numSamples:" + numSamples);
                    if (numSamples < readBufferSize){
                        Log.w("microphone", "Missed some audio samples");
                    }
//                Log.v("microphone", numSamples + " / " + writeBufferSizeFrames + " samples read");
                    onNewAudioData(audioData, numSamples);
                }
            });
        }catch (Exception e){
            ErrorHandler.eLog("onPeriodicNotification", "sadfkjsdhf", e, true);
        }
    }

    protected void onNewAudioData(float[] audioData, int numSamples){
        if (isRecording) {
            android.media.AudioTimestamp startTime = getStartTime();
            android.media.AudioTimestamp endTime = getEndTime();
            int sampleRate = getSampleRate();
            timeStepDataBuffer.getWriteData().getSoundData().setMetaData(sampleRate, startTime, endTime);
            setStartTime();
            timeStepDataBuffer.getWriteData().getSoundData().add(audioData, numSamples);
        }
        if (microphoneDataListener != null){
            microphoneDataListener.onMicrophoneDataUpdate(audioData, numSamples);
        }
    }

//
//    public void processAudioFrame(short[] audioFrame) {
//        final double bufferLength = 20; //milliseconds
//        final double bufferSampleCount = mSampleRate / bufferLength;
//        // The Google ASR input requirements state that audio input sensitivity
//        // should be set such that 90 dB SPL at 1000 Hz yields RMS of 2500 for
//        // 16-bit samples, i.e. 20 * log_10(2500 / mGain) = 90.
//        final double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
//        double mRmsSmoothed = 0;  // Temporally filtered version of RMS.
//
//        // Leq Calcs
//        double leqLength = 5; // seconds
//        double leqArrayLength = (mSampleRate / bufferSampleCount) * leqLength;
//        double[] leqBuffer = new double[(int) leqArrayLength];
//        // Compute the RMS value. (Note that this does not remove DC).
//        rms = 0;
//        for (short value : audioFrame) {
//            rms += value * value;
//        }
//        rms = Math.sqrt(rms / audioFrame.length);
//
//        // Compute a smoothed version for less flickering of the display.
//        // Coefficient of IIR smoothing filter for RMS.
//        double mAlpha = 0.9;
//        mRmsSmoothed = (mRmsSmoothed * mAlpha) + (1 - mAlpha) * rms;
//        rmsdB = 20 + (20.0 * Math.log10(mGain * mRmsSmoothed));
//
//    }
//
//    public int getTotalSamples() {
//        return mTotalSamples;
//    }
//
//    public void setTotalSamples(int totalSamples) {
//        mTotalSamples = totalSamples;
//    }
//
//    public double getRms() {
//        return rms;
//    }
//
//    public double getRmsdB() {
//        return rmsdB;
//    }
}