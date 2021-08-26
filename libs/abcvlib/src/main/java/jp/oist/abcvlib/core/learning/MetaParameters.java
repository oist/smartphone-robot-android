package jp.oist.abcvlib.core.learning;

import android.content.Context;

import java.net.InetSocketAddress;

import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.util.SocketListener;

public class MetaParameters {
    public final Context context;
    public final int timeStepLength;
    public final int maxTimeStepCount;
    public final int maxReward;
    public final int maxEpisodeCount;
    public final InetSocketAddress inetSocketAddress;
    public final TimeStepDataBuffer timeStepDataBuffer;

    public MetaParameters(Context context, int timeStepLength, int maxTimeStepCount, int maxReward, int maxEpisodeCount,
                          InetSocketAddress inetSocketAddress, TimeStepDataBuffer timeStepDataBuffer){
        this.context = context;
        this.timeStepLength = timeStepLength;
        this.maxTimeStepCount = maxTimeStepCount;
        this.maxReward = maxReward;
        this.maxEpisodeCount = maxEpisodeCount;
        this.inetSocketAddress = inetSocketAddress;
        this.timeStepDataBuffer= timeStepDataBuffer;
    }
}
