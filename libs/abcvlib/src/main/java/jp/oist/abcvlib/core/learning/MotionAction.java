package jp.oist.abcvlib.core.learning;

public class MotionAction {
    private String actionName;
    private int leftWheelPWM;
    private int rightWheelPWM;
    private byte actionByte;

    public MotionAction(String actionName, byte actionByte, int leftWheelPWM, int rightWheelPWM){
        this.actionName = actionName;
        this.actionByte = actionByte;
        this.leftWheelPWM = leftWheelPWM;
        this.rightWheelPWM = rightWheelPWM;
    }

    public int getLeftWheelPWM(){return leftWheelPWM;}
    public int getRightWheelPWM(){return rightWheelPWM;}
    public String getActionName(){return actionName;}
    public byte getActionByte(){return actionByte;}

    public void setLeftWheelPWM(int leftWheelPWM){this.leftWheelPWM = leftWheelPWM;};
    public void setRightWheelPWM(int rightWheelPWM){this.rightWheelPWM = rightWheelPWM;};
    public void setActionName(String actionName){this.actionName = actionName;}
    public void setActionByte(byte actionByte){this.actionByte = actionByte;}
    public void setAction(int leftWheelPWM, int rightWheelPWM, String actionName, byte actionByte){
        this.actionName = actionName;
        this.actionByte = actionByte;
        this.leftWheelPWM = leftWheelPWM;
        this.rightWheelPWM = rightWheelPWM;
    };
}
