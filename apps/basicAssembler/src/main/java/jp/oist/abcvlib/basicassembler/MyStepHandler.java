package jp.oist.abcvlib.basicassembler;

import android.content.Context;
import android.os.Handler;

import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.learning.CommAction;
import jp.oist.abcvlib.core.learning.MotionAction;
import jp.oist.abcvlib.core.outputs.StepHandler;

public class MyStepHandler extends StepHandler {
    private int reward = 0;
    private final Handler mainHandler;
    private final GuiUpdater guiUpdater;

    public MyStepHandler(Context context, GuiUpdater guiUpdater){
        mainHandler = new Handler(context.getMainLooper());
        this.guiUpdater = guiUpdater;
    }

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

        if (reward >= getMaxReward()){
            setLastTimestep(true);
            // reseting reward after each episode
            reward = 0;
        }

        // Note this will never be called when the myStepHandler.getTimeStep() >= myStepHandler.getMaxTimeStep() as the forward method will no longer be called
        mainHandler.post(() -> guiUpdater.updateGUIValues(data, getTimeStep(), getEpisodeCount()));

        return actionSet;
    }
}
