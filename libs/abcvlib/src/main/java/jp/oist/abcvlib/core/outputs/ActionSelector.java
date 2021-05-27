package jp.oist.abcvlib.core.outputs;

import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;

public interface ActionSelector {
    ActionSet forward(TimeStepDataBuffer.TimeStepData data, int timeStepCount);
}
