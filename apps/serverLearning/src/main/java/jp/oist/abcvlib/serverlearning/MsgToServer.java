package jp.oist.abcvlib.serverlearning;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MsgToServer extends JSONObject{

    JSONObject wheelCounts = new JSONObject();
    JSONObject chargerData = new JSONObject();
    JSONObject batteryData = new JSONObject();
    JSONObject imageData = new JSONObject();
    JSONArray soundData = new JSONArray();

    public MsgToServer(){

    }

    public void assembleEpisode(){
        try {
            this.put("WheelCounts", wheelCounts);
            this.put("ChargerData", chargerData);
            this.put("BatteryData", batteryData);
            this.put("SoundData", soundData);
            this.put("ImageData", imageData);
        } catch (JSONException e) {
            Log.e("datagatherer", "assembling went wrong");
            e.printStackTrace();
        }
    }
}
