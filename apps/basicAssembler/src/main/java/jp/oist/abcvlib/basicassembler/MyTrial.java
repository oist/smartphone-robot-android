package jp.oist.abcvlib.basicassembler;

import android.content.Context;
import android.os.Handler;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;

import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.learning.ActionSpace;
import jp.oist.abcvlib.core.learning.CommAction;
import jp.oist.abcvlib.core.learning.MetaParameters;
import jp.oist.abcvlib.core.learning.MotionAction;
import jp.oist.abcvlib.core.learning.StateSpace;
import jp.oist.abcvlib.core.learning.Trial;
import jp.oist.abcvlib.core.outputs.ActionSelector;
import jp.oist.abcvlib.util.RecordingWithoutTimeStepBufferException;
import jp.oist.abcvlib.util.SocketListener;

public class MyTrial extends Trial implements ActionSelector{
    private int reward = 0;
    private final Handler mainHandler;
    private final GuiUpdater guiUpdater;

    public MyTrial(Context context, GuiUpdater guiUpdater, MetaParameters metaParameters,
                   ActionSpace actionSpace, StateSpace stateSpace) {
        super(metaParameters, actionSpace, stateSpace);
        mainHandler = new Handler(context.getMainLooper());
        this.guiUpdater = guiUpdater;
    }

    @Override
    public void forward(TimeStepDataBuffer.TimeStepData data) {
        MotionAction motionAction;
        CommAction commAction;

        // Use data as input to your policy and select action here
        // Just using first actions of each set as an example but this should be replaced by your policy's decision process
        motionAction = getMotionActionSet().getMotionActions()[0];
        commAction = getCommActionSet().getCommActions()[0];

        // Add your selected actions to the TimeStepDataBuffer for record
        data.getActions().add(motionAction, commAction);
        // Note this will never be called when the myStepHandler.getTimeStep() >= myStepHandler.getMaxTimeStep() as the forward method will no longer be called
        mainHandler.post(() -> guiUpdater.updateGUIValues(data, getTimeStep(), getEpisodeCount()));
    }

    // If you want to do things at the start/end of the episode/trail you can override these methods from Trail

    @Override
    protected void startTrail() {
        // Do stuff here
        super.startTrail();
    }

    @Override
    public void startEpisode() {
        // Do stuff here
        super.startEpisode();
    }

    @Override
    protected void endEpisode() throws BrokenBarrierException, InterruptedException, IOException, RecordingWithoutTimeStepBufferException {
        // Do stuff here
        super.endEpisode();
    }

    @Override
    protected void endTrail() throws RecordingWithoutTimeStepBufferException {
        // Do stuff here
        super.endTrail();
    }
}
