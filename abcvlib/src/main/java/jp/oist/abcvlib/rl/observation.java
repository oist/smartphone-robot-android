package jp.oist.abcvlib.rl;

/**
 * Created by iffor_000 on 12/16/2016.
 */
public class observation {

    //state, reward, isterminal
    public double[] state;
    public double reward;
    public boolean terminal = false;

    public observation(double[] s, double r, boolean t)
    {
        this.state = s;
        this.reward = r;
        this.terminal = t;
    }
    public double[] getState()
    {
        return state;
    }
    public double getReward()
    {
        return reward;
    }
    public void setTerminal(boolean t)
    {
        terminal = t;
    }
    public boolean isTerminal()
    {
        return terminal;
    }

}
