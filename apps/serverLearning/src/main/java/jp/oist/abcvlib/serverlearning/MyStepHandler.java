package jp.oist.abcvlib.serverlearning;

import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.learning.CommAction;
import jp.oist.abcvlib.core.learning.MotionAction;
import jp.oist.abcvlib.core.outputs.StepHandler;

public class MyStepHandler extends StepHandler {

    public MyStepHandler(){}

    @Override
    public ActionSet forward(TimeStepDataBuffer.TimeStepData data) {
        ActionSet actionSet;
        MotionAction motionAction;
        CommAction commAction;

        // Set actions based on above results. e.g: the first index of each
        motionAction = getMotionActionSet().getMotionActions()[0];
        commAction = getCommActionSet().getCommActions()[0];

        // Bundle them into ActionSet so it can return both
        actionSet = new ActionSet(motionAction, commAction);

        // set your action to some ints
        data.getActions().add(motionAction, commAction);

        return actionSet;
    }
}
