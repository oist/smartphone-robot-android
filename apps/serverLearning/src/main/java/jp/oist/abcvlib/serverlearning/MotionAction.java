package jp.oist.abcvlib.serverlearning;

import org.jetbrains.annotations.NotNull;

public enum MotionAction {
    FORWARD("forward", 100, 100),
    BACKWARD("backward", -100, -100),
    LEFT("left", 0, 100),
    RIGHT("right", 100, 0),
    STOP("stop", 0, 0);

    private final String actionName;
    private final int leftWheel;
    private final int rightWheel;

    /**
     *
     * @param actionName String of action name (e.g. forward, backward, .etc)
     * @param leftWheel PWM from -100 to 100 on left wheel
     * @param rightWheel PWM from -100 to 100 on right wheel
     */
    MotionAction(String actionName, int leftWheel, int rightWheel) {
        this.actionName = actionName;
        this.leftWheel = leftWheel;
        this.rightWheel = rightWheel;
    }

    @NotNull
    @Override
    public  String toString(){
        return actionName;
    }

    public int getLeftWheelPWM(){
        return leftWheel;
    }

    public int getRightWheelPWM(){
        return rightWheel;
    }
}
