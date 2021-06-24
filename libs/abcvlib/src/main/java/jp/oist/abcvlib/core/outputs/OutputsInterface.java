package jp.oist.abcvlib.core.outputs;

import org.json.JSONObject;

import jp.oist.abcvlib.util.FileOps;

public interface OutputsInterface {

    FileOps fileOps = null;
    JSONObject controls = null;

    void setControls(JSONObject controls);
    void setAudioFile();
    void setWheelOutput(float left, float right);
    void setPID();
}
