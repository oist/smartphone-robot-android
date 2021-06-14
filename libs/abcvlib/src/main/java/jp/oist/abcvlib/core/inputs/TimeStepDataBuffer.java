package jp.oist.abcvlib.core.inputs;

import android.graphics.Bitmap;
import android.media.AudioTimestamp;

import java.util.ArrayList;

import jp.oist.abcvlib.core.learning.CommAction;
import jp.oist.abcvlib.core.learning.MotionAction;

public class TimeStepDataBuffer {

    private final int bufferLength;
    private int writeIndex;
    private int readIndex;
    private final TimeStepData[] buffer;
    private TimeStepData writeData;
    private TimeStepData readData;

    public TimeStepDataBuffer(int bufferLength){
        if (bufferLength <= 1){
            throw new RuntimeException("bufferLength must be larger than 1. " +
                    "bufferLength of " + bufferLength + " provided.");
        }
        this.bufferLength = bufferLength;
        buffer =  new TimeStepData[bufferLength];

        writeIndex = 1;
        readIndex = 0;

        // populate buffer with TimeStepData instances
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

    public synchronized TimeStepData getWriteData(){return writeData;}

    public synchronized TimeStepData getReadData(){return readData;}

    public static class TimeStepData{
        private WheelData wheelData;
        private ChargerData chargerData;
        private BatteryData batteryData;
        private ImageData imageData;
        private SoundData soundData;
        private RobotAction actions;
        private OrientationData orientationData;

        public TimeStepData(){
            wheelData = new WheelData();
            chargerData = new ChargerData();
            batteryData = new BatteryData();
            imageData = new ImageData();
            soundData = new SoundData();
            actions = new RobotAction();
            orientationData = new OrientationData();
        }

        public synchronized WheelData getWheelData(){return wheelData;}
        public synchronized ChargerData getChargerData(){return chargerData;}
        public synchronized BatteryData getBatteryData(){return batteryData;}
        public synchronized ImageData getImageData(){return imageData;}
        public synchronized SoundData getSoundData(){return soundData;}
        public synchronized RobotAction getActions(){return actions;}
        public synchronized OrientationData getOrientationData(){return orientationData;}

        public void clear(){
            wheelData = new WheelData();
            chargerData = new ChargerData();
            batteryData = new BatteryData();
            imageData = new ImageData();
            soundData = new SoundData();
            actions = new RobotAction();
            orientationData = new OrientationData();
        }

        public static class WheelData {
            IndividualWheelData left = new IndividualWheelData();
            IndividualWheelData right = new IndividualWheelData();

            public static class IndividualWheelData {
                ArrayList<Long> timestamps = new ArrayList<>();
                ArrayList<Integer> counts = new ArrayList<>();
                ArrayList<Double> distances = new ArrayList<>();
                ArrayList<Double> speeds = new ArrayList<>();

                public void put(long timestamp, int count, double distance, double speed){
                    timestamps.add(timestamp);
                    counts.add(count);
                    distances.add(distance);
                    speeds.add(speed);
                }
                public long[] getTimeStamps(){
                    int size = timestamps.size();
                    long[] timestampslong = new long[size];
                    for (int i=0 ; i < size ; i++){
                        timestampslong[i] = timestamps.get(i);
                    }
                    return timestampslong;
                }

                public int[] getCounts(){
                    int size = counts.size();
                    int[] countsArray = new int[size];
                    for (int i=0 ; i < size ; i++){
                        countsArray[i] = counts.get(i);
                    }
                    return countsArray;
                }

                public double[] getDistances(){
                    int size = distances.size();
                    double[] distancesArray = new double[size];
                    for (int i=0 ; i < size ; i++){
                        distancesArray[i] = distances.get(i);
                    }
                    return distancesArray;
                }

                public double[] getSpeeds(){
                    int size = speeds.size();
                    double[] speedsArray = new double[size];
                    for (int i=0 ; i < size ; i++){
                        speedsArray[i] = speeds.get(i);
                    }
                    return speedsArray;
                }
            }

            public IndividualWheelData getLeft() {
                return left;
            }

            public IndividualWheelData getRight() {
                return right;
            }
        }

        public static class ChargerData{
            ArrayList<Long> timestamps = new ArrayList<>();
            ArrayList<Double> chargerVoltage = new ArrayList<>();
            ArrayList<Double> coilVoltage = new ArrayList<>();
            public void put(double _chargerVoltage, double _coilVoltage){
                timestamps.add(System.nanoTime());
                chargerVoltage.add(_chargerVoltage);
                coilVoltage.add(_coilVoltage);
            }
            public long[] getTimeStamps(){
                int size = timestamps.size();
                long[] timestampslong = new long[size];
                for (int i=0 ; i < size ; i++){
                    timestampslong[i] = timestamps.get(i);
                }
                return timestampslong;
            }
            public double[] getChargerVoltage(){
                int size = chargerVoltage.size();
                double[] voltageLong = new double[size];
                for (int i=0 ; i < size ; i++){
                    voltageLong[i] = chargerVoltage.get(i);
                }
                return voltageLong;
            }
            public double[] getCoilVoltage(){
                int size = coilVoltage.size();
                double[] voltageLong = new double[size];
                for (int i=0 ; i < size ; i++){
                    voltageLong[i] = coilVoltage.get(i);
                }
                return voltageLong;
            }
        }

        public static class BatteryData{
            ArrayList<Long> timestamps = new ArrayList<>();
            ArrayList<Double> voltage = new ArrayList<>();
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

        public static class SoundData{
            private AudioTimestamp startTime = new AudioTimestamp();
            private AudioTimestamp endTime = new AudioTimestamp();
            private double totalTime;
            private int sampleRate;
            private long totalSamples = 0;
            private long totalSamplesCalculatedViaTime;
            private final ArrayList<Float> levels = new ArrayList<>();

            public SoundData(){
            }

            public void add(float[] _levels, int _numSamples){
                for (float _level : _levels){
                    levels.add(_level);
                }
                totalSamples += _numSamples;
            }

            public void setMetaData(int sampleRate, AudioTimestamp startTime, AudioTimestamp endTime){
//                Log.i("audioFrame", (this.endTime.nanoTime - startTime.nanoTime) + " missing nanoseconds between last frames");

                if (startTime.framePosition != 0){
                    this.startTime = startTime;
                }
                //todo add logic to test if timestamps overlap or have gaps.
                this.endTime = endTime;
                this.totalTime = (endTime.nanoTime - startTime.nanoTime) * 10e-10;
                this.sampleRate = sampleRate;
                this.totalSamplesCalculatedViaTime = endTime.framePosition - startTime.framePosition;
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

            public long getTotalSamplesCalculatedViaTime() {
                return totalSamplesCalculatedViaTime;
            }
        }

        public static class ImageData{
            private final ArrayList<SingleImage> images = new ArrayList<>();

            public void add(long timestamp, int width, int height, Bitmap bitmap, byte[] webpImage){
                SingleImage singleImage = new SingleImage(timestamp, width, height, bitmap, webpImage);
                images.add(singleImage);
            }

            public ArrayList<SingleImage> getImages() {
                return images;
            }

            public static class SingleImage{
                private final long timestamp;
                private final int width;
                private final int height;
                private final Bitmap bitmap;
                private final byte[] webpImage;

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

        public static class RobotAction{
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

        public static class OrientationData{
            ArrayList<Long> timestamps = new ArrayList<>();
            ArrayList<Double> tiltAngle = new ArrayList<>();
            ArrayList<Double> angularVelocity = new ArrayList<>();

            /**
             * @param timestamp long nanotime
             * @param _tiltAngle in radians
             * @param _angularVelocity in radians per second
             */
            public void put(long timestamp, double _tiltAngle, double _angularVelocity){
                timestamps.add(timestamp);
                tiltAngle.add(_tiltAngle);
                angularVelocity.add(_angularVelocity);
            }
            public long[] getTimeStamps(){
                int size = timestamps.size();
                long[] timestampslong = new long[size];
                for (int i=0 ; i < size ; i++){
                    timestampslong[i] = timestamps.get(i);
                }
                return timestampslong;
            }
            public double[] getTiltAngle(){
                int size = tiltAngle.size();
                double[] leftLong = new double[size];
                for (int i=0 ; i < size ; i++){
                    leftLong[i] = tiltAngle.get(i);
                }
                return leftLong;
            }
            public double[] getAngularVelocity(){
                int size = angularVelocity.size();
                double[] rightLong = new double[size];
                for (int i=0 ; i < size ; i++){
                    rightLong[i] = angularVelocity.get(i);
                }
                return rightLong;
            }
        }
    }
}
