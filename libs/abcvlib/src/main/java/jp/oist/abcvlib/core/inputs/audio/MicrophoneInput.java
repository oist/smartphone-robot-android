package jp.oist.abcvlib.core.inputs.audio;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibActivity;

import static android.content.Context.MODE_PRIVATE;

public class MicrophoneInput implements Runnable{

    private static final String TAG = "abcvlib";
    private AbcvlibActivity abcvlibActivity;
    private Thread mThread;

    private int mSampleRate = 16000;
    private double bufferLength = 20; //milliseconds
    private double bufferSampleCount = mSampleRate / bufferLength;
    // The Google ASR input requirements state that audio input sensitivity
    // should be set such that 90 dB SPL at 1000 Hz yields RMS of 2500 for
    // 16-bit samples, i.e. 20 * log_10(2500 / mGain) = 90.
    private double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
    private double mRmsSmoothed;  // Temporally filtered version of RMS.
    private double rms;
    private double rmsdB;

    // Leq Calcs
    private double leqLength = 5; // seconds
    private double leqArrayLength = (mSampleRate / bufferSampleCount) * leqLength;
    private long startTime; // nanoseconds
    private double[] leqBuffer = new double[(int) leqArrayLength];

    //todo add some Leq values for longer term averages
    private int mTotalSamples = 0;

    public MicrophoneInput(AbcvlibActivity abcvlibActivity){
        this.abcvlibActivity = abcvlibActivity;
    }

    public void start() {
        mThread = new Thread(this);
        mThread.start();
    }

    public void stop() {
        try {
            mThread.join(10);
        } catch (InterruptedException e) {
            Log.v(TAG, "InterruptedException.", e);
        }
    }

    @Override
    public void run() {
        startTime = System.nanoTime();
        // Buffer for 20 milliseconds of data, e.g. 320 samples at 16kHz.
        Log.i("abcvlib", "In MicInput run method");
        short[] buffer = new short[(int) bufferSampleCount];
        // Buffer size of AudioRecord buffer, which will be at least 1 second.
        int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
        int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int buffer1000msSize = bufferSize(mSampleRate, mChannelConfig,
                mAudioFormat);

        SharedPreferences preferences = abcvlibActivity.getSharedPreferences("LevelMeter", MODE_PRIVATE);
        int mSampleRate = preferences.getInt("SampleRate", 16000);
        int mAudioSource = preferences.getInt("AudioSource",
                MediaRecorder.AudioSource.VOICE_RECOGNITION);
        setSampleRate(mSampleRate);
        setAudioSource(mAudioSource);

        try {
            Log.i("abcvlib", "In MicInput try");

            AudioRecord recorder = new AudioRecord(
                    mAudioSource,
                    mSampleRate,
                    mChannelConfig,
                    mAudioFormat,
                    buffer1000msSize);
            recorder.startRecording();

            while (!abcvlibActivity.appRunning){
                try {
                    Log.i("abcvlib", this.toString() + "Waiting for appRunning to be true");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            while (abcvlibActivity.appRunning) {
                int numSamples = recorder.read(buffer, 0, buffer.length);
                mTotalSamples += numSamples;
                processAudioFrame(buffer);
            }

            recorder.stop();
            stop();
        } catch(Throwable x) {
            Log.v(TAG, "Error reading audio", x);
        } finally {
        }
    }

    /**
     * Helper method to find a buffer size for AudioRecord which will be at
     * least 1 second.
     *
     * @param sampleRateInHz the sample rate expressed in Hertz.
     * @param channelConfig describes the configuration of the audio channels.
     * @param audioFormat the format in which the audio data is represented.
     * @return buffSize the size of the audio record input buffer.
     */
    private int bufferSize(int sampleRateInHz, int channelConfig,
                           int audioFormat) {
        int buffSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig,
                audioFormat);
        if (buffSize < sampleRateInHz) {
            buffSize = sampleRateInHz;
        }
        return buffSize;
    }

    public void processAudioFrame(short[] audioFrame) {
        // Compute the RMS value. (Note that this does not remove DC).
        rms = 0;
        for (short value : audioFrame) {
            rms += value * value;
        }
        rms = Math.sqrt(rms / audioFrame.length);

        // Compute a smoothed version for less flickering of the display.
        // Coefficient of IIR smoothing filter for RMS.
        double mAlpha = 0.9;
        mRmsSmoothed = (mRmsSmoothed * mAlpha) + (1 - mAlpha) * rms;
        rmsdB = 20 + (20.0 * Math.log10(mGain * mRmsSmoothed));


    }

    private void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
    }

    private void setAudioSource(int audioSource) {
    }

    public int getTotalSamples() {
        return mTotalSamples;
    }

    public void setTotalSamples(int totalSamples) {
        mTotalSamples = totalSamples;
    }

    public double getRms() {
        return rms;
    }

    public double getRmsdB() {
        return rmsdB;
    }
}