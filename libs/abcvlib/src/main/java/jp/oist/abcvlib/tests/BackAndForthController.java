package jp.oist.abcvlib.tests;

import jp.oist.abcvlib.core.outputs.AbcvlibController;

public class BackAndForthController extends AbcvlibController {
    float speed;
    float currentSpeed;

    public BackAndForthController(float speed){
        this.speed = speed;
        this.currentSpeed = speed;
    }

    @Override
    public void run() {
        if (currentSpeed == speed){
            currentSpeed = -speed;
        }else {
            currentSpeed = speed;
        }
        setOutput(currentSpeed, currentSpeed);
    }
}
