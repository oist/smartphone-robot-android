package jp.oist.abcvlib.rl;

/**
 * Created by iffor_000 on 12/16/2016.
 */
public class rlCore {

    double ret=0;
    int[] lastAction=new int[2];
    agtInterface A=null;
    envInterface E=null;
    int oldtimer=0;

    public rlCore(agtInterface a, envInterface e){
        this.A=a;
        this.E=e;
    }

    public void RL_init() {

        A.agent_init();
        E.env_init();


    }

    public observation RL_start(int timer) {

        observation o=null;

        if(timer==1){
            o=E.env_start();
            lastAction=A.agent_start(o);

        }
        return o;

    }

    public boolean RL_Step() {

        observation o=E.env_step();
        ret+=o.getReward();

        if(o.isTerminal()){
            RL_end();
        }else{
            lastAction=A.agent_step(o);
        }

        return o.isTerminal();
    }


    public void RL_end(){

        A.agent_end();
        E.env_end();
    }

    public double RL_Return(){
        return ret;
    }

    public void resetReturn() { ret=0;}

    public int[] getLastAction() { return lastAction; }
}
