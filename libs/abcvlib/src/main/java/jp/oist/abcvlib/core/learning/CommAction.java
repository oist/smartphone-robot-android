package jp.oist.abcvlib.core.learning;

public interface CommAction {
    String actionName = null;
    byte actionByte = 0;

    String getActionName();
    int getActionByte();

    void setActionName(String actionName);
    void setActionByte(byte actionByte);
}
