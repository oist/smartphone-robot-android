package jp.oist.abcvlib.core.outputs;

import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;

public interface ActionSelector {
    void forward(TimeStepDataBuffer.TimeStepData data);
}
