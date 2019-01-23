package jp.oist.abcvlib.rl;

/**
 * Created by iffor_000 on 12/16/2016.
 */
public interface agtInterface {

    void agent_init();
    int[] agent_start(observation o);
    int[] agent_step(observation obrt);
    void agent_end();

}
