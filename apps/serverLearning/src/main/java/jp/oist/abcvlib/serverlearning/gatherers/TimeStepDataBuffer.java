package jp.oist.abcvlib.serverlearning.gatherers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioTimestamp;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jp.oist.abcvlib.serverlearning.CommAction;
import jp.oist.abcvlib.serverlearning.MotionAction;

public class TimeStepDataBuffer {

    private int bufferLength;
    private int writeIndex;
    private int readIndex;
    private TimeStepData[] buffer;
    private TimeStepData writeData;
    private TimeStepData readData;

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

    public TimeStepData getWriteData(){return writeData;}

    public TimeStepData getReadData(){return readData;}

    public class TimeStepData{
        private WheelCounts wheelCounts;
        private ChargerData chargerData;
        private BatteryData batteryData;
        private ImageData imageData;
        private SoundData soundData;
        private RobotAction actions;
        private ReentrantReadWriteLock.WriteLock lock;

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

        public WheelCounts getWheelCounts(){return wheelCounts;}
        public ChargerData getChargerData(){return chargerData;}
        public BatteryData getBatteryData(){return batteryData;}
        public ImageData getImageData(){return imageData;}
        public SoundData getSoundData(){return soundData;}
        public RobotAction getActions(){return actions;}

        public void clear(){
            wheelCounts = new WheelCounts();
            chargerData = new ChargerData();
            batteryData = new BatteryData();
            imageData = new ImageData();
            soundData = new SoundData();
            actions = new RobotAction();
        }

        public class WheelCounts{
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

        public class ChargerData{
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

        public class BatteryData{
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

        public class SoundData{
            private AudioTimestamp startTime = new AudioTimestamp();
            private AudioTimestamp endTime = new AudioTimestamp();
            private double totalTime;
            private int sampleRate;
            private long totalSamples;
            private ArrayList<Float> levels = new ArrayList<Float>();

            public SoundData(){
            }

            public void add(float[] _levels, int _numSamples){
                for (float _level : _levels){
                    levels.add(_level);
                }
                totalSamples += _numSamples;
            }

            public void setMetaData(int sampleRate, AudioTimestamp startTime, AudioTimestamp endTime){
                this.startTime = startTime;
                this.endTime = endTime;
                this.totalTime = (endTime.nanoTime - startTime.nanoTime) * 10e-10;
                this.sampleRate = sampleRate;
                this.totalSamples = endTime.framePosition - startTime.framePosition;
            }

            public float[] getLevels(){
                int size = levels.size();
                float[] levelsFloat = new float[size];
                for (int i=0 ; i < size ; i++){
                    levelsFloat[i] = levels.get(i);
                }
                return levelsFloat;
            }

            public long getTotalSamples() {
                return totalSamples;
            }

            public AudioTimestamp getEndTime() {
                return endTime;
            }

            public AudioTimestamp getStartTime() {
                return startTime;
            }

            public double getTotalTime() {
                return totalTime;
            }

            public int getSampleRate() {
                return sampleRate;
            }
        }

        public class ImageData{
            private ArrayList<SingleImage> images = new ArrayList<SingleImage>();

            public void add(long timestamp, int width, int height, Bitmap bitmap, byte[] webpImage){
                SingleImage singleImage = new SingleImage(timestamp, width, height, bitmap, webpImage);
                images.add(singleImage);
            }

            public ArrayList<SingleImage> getImages() {
                return images;
            }

            public class SingleImage{
                private long timestamp;
                private int width;
                private int height;
                private Bitmap bitmap;
                private byte[] webpImage;

                public SingleImage(long timestamp, int width, int height, Bitmap bitmap,
                                   byte[] webpImage){
                    this.timestamp = timestamp;
                    this.width = width;
                    this.height = height;
                    this.bitmap = bitmap;
                    this.webpImage = webpImage;
                }

                public Bitmap getBitmap() {
                    return bitmap;
                }

                public byte[] getWebpImage() {
                    return webpImage;
                }

                public int getHeight() {
                    return height;
                }

                public int getWidth() {
                    return width;
                }

                public long getTimestamp() {
                    return timestamp;
                }
            }
        }

        public class RobotAction{
            private MotionAction motionAction;
            private CommAction commAction;
            public void add(MotionAction motionAction, CommAction commAction){
                this.motionAction = motionAction;
                this.commAction = commAction;
            }

            public MotionAction getMotionAction() {
                return motionAction;
            }

            public CommAction getCommAction() {
                return commAction;
            }
        }
    }

}
