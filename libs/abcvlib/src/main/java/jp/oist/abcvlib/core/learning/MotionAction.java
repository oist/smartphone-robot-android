package jp.oist.abcvlib.core.learning;

public class MotionAction {
    private String actionName;
    private float leftWheelPWM;
    private float rightWheelPWM;
    private byte actionByte;
    private boolean leftWheelBrake;
    private boolean rightWheelBrake;

    public MotionAction(String actionName, byte actionByte, float leftWheelPWM, float rightWheelPWM,
                        boolean leftWheelBrake, boolean rightWheelBrake){
        this.actionName = actionName;
        this.actionByte = actionByte;
        this.leftWheelPWM = leftWheelPWM;
        this.rightWheelPWM = rightWheelPWM;
        this.leftWheelBrake = leftWheelBrake;
        this.rightWheelBrake = rightWheelBrake;
    }

    public float getLeftWheelPWM(){return leftWheelPWM;}
    public float getRightWheelPWM(){return rightWheelPWM;}
    public boolean getLeftWheelBrake(){return leftWheelBrake;}
    public boolean getRightWheelBrake(){return rightWheelBrake;}
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
