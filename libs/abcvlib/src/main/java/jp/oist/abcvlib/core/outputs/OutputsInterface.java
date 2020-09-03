package jp.oist.abcvlib.core.outputs;

import org.json.JSONObject;

public interface OutputsInterface {

    FileOps fileOps = null;
    JSONObject controls = null;

    void setControls(JSONObject controls);
    void setAudioFile();
    void setWheelOutput(int left, int right);
    void setPID();
}
