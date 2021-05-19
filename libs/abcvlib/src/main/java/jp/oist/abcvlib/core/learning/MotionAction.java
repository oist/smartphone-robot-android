package jp.oist.abcvlib.core.learning;

public interface MotionAction {
    String actionName = null;
    int leftWheelPWM = 0;
    int rightWheelPWM = 0;
    byte actionByte = 0;

    int getLeftWheelPWM();
    int getRightWheelPWM();
    String getActionName();
    int getActionByte();

    void setLeftWheelPWM(int leftWheelPWM);
    void setRightWheelPWM(int rightWheelPWM);
    void setActionName(String actionName);
    void setActionByte(byte actionByte);
}
