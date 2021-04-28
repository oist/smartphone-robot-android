package jp.oist.abcvlib.serverlearning;

import org.jetbrains.annotations.NotNull;

public enum MotionAction {
    STOP(0, "stop", 0, 0),
    FORWARD(1, "forward", 100, 100),
    BACKWARD(2, "backward", -100, -100),
    LEFT(3, "left", 0, 100),
    RIGHT(4, "right", 100, 0);

    private final String actionName;
    private final int leftWheel;
    private final int rightWheel;
    private final int actionByte;

    /**
     *  @param actionName String of action name (e.g. forward, backward, .etc)
     * @param leftWheel PWM from -100 to 100 on left wheel
     * @param rightWheel PWM from -100 to 100 on right wheel
     * @param actionByte
     */
    MotionAction(int actionByte, String actionName, int leftWheel, int rightWheel) {
        this.actionName = actionName;
        this.leftWheel = leftWheel;
        this.rightWheel = rightWheel;
        this.actionByte = actionByte;
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

    public String getActionName() {
        return actionName;
    }

    public int getActionByte() {
        return actionByte;
    }
}
