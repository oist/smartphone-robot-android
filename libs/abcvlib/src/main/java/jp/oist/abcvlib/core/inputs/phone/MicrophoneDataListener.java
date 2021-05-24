package jp.oist.abcvlib.core.inputs.phone;

public interface MicrophoneDataListener {
    /**
     * Likely easier to pull data from a TimeStepDataBuffer as it handles concatenating all samples
     * to one object.
     * @param audioData most recent sampling from buffer
     * @param numSamples number of samples copied from buffer
     */
    void onMicrophoneDataUpdate(float[] audioData, int numSamples);
}
