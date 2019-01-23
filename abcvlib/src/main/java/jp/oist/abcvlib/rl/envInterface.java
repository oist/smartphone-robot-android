package jp.oist.abcvlib.rl;

/**
 * Created by iffor_000 on 12/16/2016.
 */
public interface envInterface {

    void env_init();
    observation env_start();
    observation env_step();
    void env_end();

}
