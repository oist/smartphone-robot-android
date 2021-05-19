package jp.oist.abcvlib.core.learning;

import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;

public interface StepHandler {
    int maxTimeStepCount = 0;
    boolean lastEpisode = false; // Use to trigger MainActivity to stop generating episodes
    boolean lastTimestep = false; // Use to trigger MainActivity to stop generating timesteps for a single episode
    int reward = 0;
    int rewardCriterion = 0;
    int maxEpisodecount = 0;
    int episodeCount = 0;

    ActionSet forward(TimeStepDataBuffer.TimeStepData data, int timeStepCount);

    int getEpisodeCount();
    boolean isLastEpisode();
    boolean isLastTimestep();
    int getMaxEpisodecount();
    int getMaxTimeStepCount();
    int getReward();
    int getRewardCriterion();

    void setLastTimestep(boolean lastTimestep);
}
