package jp.oist.abcvlib.core.inputs;

import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;

public interface AbcvlibInput {
    void setRecording(boolean recording);
    void setTimeStepDataBuffer(TimeStepDataBuffer timeStepDataBuffer);
    TimeStepDataBuffer getTimeStepDataBuffer();
}
