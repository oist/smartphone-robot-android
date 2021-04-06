package jp.oist.abcvlib.serverlearning;

import android.media.AudioTimestamp;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jp.oist.abcvlib.core.learning.ActionDistribution;

public class TimeStepDataBuffer {

    private int bufferLength;
    private int writeIndex;
    private int readIndex;
    private TimeStepData[] buffer;
    TimeStepData writeData;
    TimeStepData readData;

    public TimeStepDataBuffer(int bufferLength){
        if (bufferLength <= 1){
            Log.e("TimeStampDataBuffer", "bufferLength must be larger than 1");
        }

        this.bufferLength = bufferLength;
        buffer =  new TimeStepData[bufferLength];

        writeIndex = 1;
        readIndex = 0;

        for(int i = 0 ; i < bufferLength; i++){
            buffer[i] = new TimeStepData();
        }
        writeData = buffer[writeIndex];
        readData = buffer[readIndex];
    }

    public void nextTimeStep(){
        // Update index for read and write pointer
        writeIndex = ((writeIndex + 1) % bufferLength);
        readIndex = ((readIndex + 1) % bufferLength);

        // Clear the next TimeStepData object for new writing
        buffer[writeIndex].clear();

        // Move pointer for reading and writing objects one index forward;
        writeData = buffer[writeIndex];
        readData = buffer[readIndex];
    }

    class TimeStepData{
        WheelCounts wheelCounts;
        ChargerData chargerData;
        BatteryData batteryData;
        ImageData imageData;
        SoundData soundData;
        RobotAction actions;
        ReentrantReadWriteLock.WriteLock lock;

        public TimeStepData(){
            wheelCounts = new WheelCounts();
            chargerData = new ChargerData();
            batteryData = new BatteryData();
            imageData = new ImageData();
            soundData = new SoundData();
            actions = new RobotAction();
        }

        public void lock(){
            lock.lock();
        }

        public void unlock(){
            lock.unlock();
        }


        public void clear(){
            wheelCounts = new WheelCounts();
            chargerData = new ChargerData();
            batteryData = new BatteryData();
            imageData = new ImageData();
            soundData = new SoundData();
            actions = new RobotAction();
        }

        class WheelCounts{
            ArrayList<Long> timestamps = new ArrayList<Long>();
            ArrayList<Double> left = new ArrayList<Double>();
            ArrayList<Double> right = new ArrayList<Double>();
            public void put(double _left, double _right){
                timestamps.add(System.nanoTime());
                left.add(_left);
                right.add(_right);
            }
            public long[] getTimeStamps(){
                int size = timestamps.size();
                long[] timestampslong = new long[size];
                for (int i=0 ; i < size ; i++){
                    timestampslong[i] = timestamps.get(i);
                }
                return timestampslong;
            }
            public double[] getLeft(){
                int size = left.size();
                double[] leftLong = new double[size];
                for (int i=0 ; i < size ; i++){
                    leftLong[i] = left.get(i);
                }
                return leftLong;
            }
            public double[] getRight(){
                int size = right.size();
                double[] rightLong = new double[size];
                for (int i=0 ; i < size ; i++){
                    rightLong[i] = right.get(i);
                }
                return rightLong;
            }
        }

        class ChargerData{
            ArrayList<Long> timestamps = new ArrayList<Long>();
            ArrayList<Double> voltage = new ArrayList<Double>();
            public void put(double _voltage){
                timestamps.add(System.nanoTime());
                voltage.add(_voltage);
            }
            public long[] getTimeStamps(){
                int size = timestamps.size();
                long[] timestampslong = new long[size];
                for (int i=0 ; i < size ; i++){
                    timestampslong[i] = timestamps.get(i);
                }
                return timestampslong;
            }
            public double[] getVoltage(){
                int size = voltage.size();
                double[] voltageLong = new double[size];
                for (int i=0 ; i < size ; i++){
                    voltageLong[i] = voltage.get(i);
                }
                return voltageLong;
            }
        }

        class BatteryData{
            ArrayList<Long> timestamps = new ArrayList<Long>();
            ArrayList<Double> voltage = new ArrayList<Double>();
            public void put(double _voltage){
                timestamps.add(System.nanoTime());
                voltage.add(_voltage);
            }
            public long[] getTimeStamps(){
                int size = timestamps.size();
                long[] timestampslong = new long[size];
                for (int i=0 ; i < size ; i++){
                    timestampslong[i] = timestamps.get(i);
                }
                return timestampslong;
            }
            public double[] getVoltage(){
                int size = voltage.size();
                double[] voltageLong = new double[size];
                for (int i=0 ; i < size ; i++){
                    voltageLong[i] = voltage.get(i);
                }
                return voltageLong;
            }
        }

        class SoundData{
            HashMap<String, Long> StartTime = new HashMap<String, Long>();
            HashMap<String, Long> EndTime = new HashMap<String, Long>();
            double TotalTime;
            int SampleRate;
            long TotalSamples;
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
                this.StartTime.put("framePosition", startTime.framePosition);
                this.StartTime.put("nanotime", startTime.nanoTime);
                this.EndTime.put("framePosition", endTime.framePosition);
                this.EndTime.put("nanotime", endTime.nanoTime);
                double totalTime = (endTime.nanoTime - startTime.nanoTime) * 10e-10;
                this.TotalTime = totalTime;
                this.SampleRate = sampleRate;
                this.TotalSamples = endTime.framePosition - startTime.framePosition;
            }
        }

        class ImageData{
            ArrayList<SingleImage> images = new ArrayList<SingleImage>();

            public void add(long timestamp, int width, int height, int[][] pixels){
                SingleImage singleImage = new SingleImage(timestamp, width, height, pixels);
                images.add(singleImage);
            }

            class SingleImage{
                long timestamp;
                Pixels pixels;
                int width;
                int height;

                public SingleImage(long timestamp, int width, int height, int[][] pixels){
                    this.timestamp = timestamp;
                    this.width = width;
                    this.height = height;
                    this.pixels = new Pixels(pixels);
                }

                class Pixels{
                    int[] r;
                    int[] g;
                    int[] b;

                    public Pixels(int[][] pixels){
                        r = pixels[0];
                        g = pixels[1];
                        b = pixels[2];
                    }
                }
            }
        }

        class RobotAction{
            private int motionAction;
            private int commAction;
            public void add(int motionAction, int commAction){
                this.motionAction = motionAction;
                this.commAction = commAction;
            }
        }
    }

}
