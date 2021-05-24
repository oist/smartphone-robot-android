package jp.oist.abcvlib.basic;

import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.learning.CommActionSet;
import jp.oist.abcvlib.core.learning.MotionActionSet;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.core.outputs.StepHandler;

public class MyStepHandler extends StepHandler {
    public MyStepHandler(int maxTimeStepCount, int rewardCriterion, int maxEpisodeCount, CommActionSet commActionSet, MotionActionSet motionActionSet) {
        super(maxTimeStepCount, rewardCriterion, maxEpisodeCount, commActionSet, motionActionSet);
    }
}
