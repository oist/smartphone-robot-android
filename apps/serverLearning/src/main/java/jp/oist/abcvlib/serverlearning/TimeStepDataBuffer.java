package jp.oist.abcvlib.serverlearning;

import android.media.AudioTimestamp;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class TimeStepDataBuffer {

    private int bufferLength;
    private int writeIndex;
    private int readIndex;
    private TimeStepData[] buffer;
    static TimeStepData writeData;
    static TimeStepData readData;

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

        private final ReentrantLock lock = new ReentrantLock(true);

        public TimeStepData(){
            wheelCounts = new WheelCounts();
            chargerData = new ChargerData();
            batteryData = new BatteryData();
            imageData = new ImageData();
            soundData = new SoundData();
        }

        public void lock(){
            lock.lock();
        }

        public void unlock(){
            lock.unlock();
        }

        public synchronized boolean isLocked(){
            return lock.isLocked();
        }

        public void clear(){
            wheelCounts = new WheelCounts();
            chargerData = new ChargerData();
            batteryData = new BatteryData();
            imageData = new ImageData();
            soundData = new SoundData();
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
        }

        class ChargerData{
            ArrayList<Long> timestamps = new ArrayList<Long>();
            ArrayList<Double> voltage = new ArrayList<Double>();
            public void put(double _voltage){
                timestamps.add(System.nanoTime());
                voltage.add(_voltage);
            }
        }

        class BatteryData{
            ArrayList<Long> timestamps = new ArrayList<Long>();
            ArrayList<Double> voltage = new ArrayList<Double>();
            public void put(double _voltage){
                timestamps.add(System.nanoTime());
                voltage.add(_voltage);
            }
        }

        class SoundData{
            AudioTimestamp StartTime;
            AudioTimestamp EndTime;
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
                this.StartTime = startTime;
                this.EndTime = endTime;
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
    }


}
