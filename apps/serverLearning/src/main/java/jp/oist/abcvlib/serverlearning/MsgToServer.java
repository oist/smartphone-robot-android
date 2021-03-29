package jp.oist.abcvlib.serverlearning;

import android.media.AudioTimestamp;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MsgToServer extends JSONObject{

    WheelCounts wheelCounts = new WheelCounts();
    ChargerData chargerData = new ChargerData();
    BatteryData batteryData = new BatteryData();
    JSONObject imageData = new JSONObject();
    SoundData soundData = new SoundData();

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

    static class WheelCounts{
        ArrayList<Long> timestamps = new ArrayList<Long>();
        ArrayList<Double> left = new ArrayList<Double>();
        ArrayList<Double> right = new ArrayList<Double>();
        public void put(double _left, double _right){
            timestamps.add(System.nanoTime());
            left.add(_left);
            right.add(_right);
        }
    }

    static class ChargerData{
        ArrayList<Long> timestamps = new ArrayList<Long>();
        ArrayList<Double> voltage = new ArrayList<Double>();
        public void put(double _voltage){
            timestamps.add(System.nanoTime());
            voltage.add(_voltage);
        }
    }

    static class BatteryData{
        ArrayList<Long> timestamps = new ArrayList<Long>();
        ArrayList<Double> voltage = new ArrayList<Double>();
        public void put(double _voltage){
            timestamps.add(System.nanoTime());
            voltage.add(_voltage);
        }
    }

    static class SoundData{
        AudioTimestamp StartTime;
        AudioTimestamp EndTime;
        double TotalTime;
        int SampleRate;
        int TotalSamples;
        ArrayList<Float> level = new ArrayList<Float>();

        public SoundData(){
        }

        public void add(float[] _levels, int _numSamples){
            for (float _level : _levels){
                level.add(_level);
            }
            TotalSamples += _numSamples;
        }

        public void setMetaData(int sampleRate, AudioTimestamp startTime, AudioTimestamp endTime){
            this.StartTime = startTime;
            this.EndTime = endTime;
            double totalTime = (endTime.nanoTime - startTime.nanoTime) * 10e-10;
            this.TotalTime = totalTime;
            this.SampleRate = sampleRate;
        }
    }
}
