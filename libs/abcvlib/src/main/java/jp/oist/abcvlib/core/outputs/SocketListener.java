package jp.oist.abcvlib.core.outputs;

import org.json.JSONObject;

public interface SocketListener {
    void onServerReadSuccess(JSONObject serverMsg);
}
