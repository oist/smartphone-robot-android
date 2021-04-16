package jp.oist.abcvlib.util;

import org.json.JSONObject;

public interface SocketListener {
    // Call this in mainactivity such that one can reREAD from disk the model files read from server
    void onServerReadSuccess();
}
