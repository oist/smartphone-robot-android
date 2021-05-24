package jp.oist.abcvlib.core.inputs;

import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;

public interface AbcvlibInput {
    void setRecording(boolean recording) throws RecordingWithoutTimeStepBufferException;
    void setTimeStepDataBuffer(TimeStepDataBuffer timeStepDataBuffer);
    TimeStepDataBuffer getTimeStepDataBuffer();
}
