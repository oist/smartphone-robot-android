package jp.oist.abcvlib.serverlearning;

import org.json.JSONException;
import org.json.JSONObject;

public class MsgToServer extends JSONObject{

    JSONObject wheelCounts = new JSONObject();
    JSONObject chargerData = new JSONObject();
    JSONObject batteryData = new JSONObject();
    JSONObject imageData = new JSONObject();
    JSONObject soundData = new JSONObject();

    public MsgToServer(){

    }

    public void assembleEpisode(){
        try {
            this.put("WheelCounts", wheelCounts);
            this.put("ChargerData", chargerData);
            this.put("BatteryData", batteryData);
            this.put("ImageData", imageData);
            this.put("SoundData", soundData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
