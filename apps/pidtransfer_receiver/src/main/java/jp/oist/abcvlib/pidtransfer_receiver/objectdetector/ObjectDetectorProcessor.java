/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.oist.abcvlib.pidtransfer_receiver.objectdetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.media.SoundPool ;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import jp.oist.abcvlib.pidtransfer_receiver.GraphicOverlay;
import jp.oist.abcvlib.pidtransfer_receiver.R;
import jp.oist.abcvlib.pidtransfer_receiver.VisionProcessorBase;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Queue;

/**
 * A processor to run object detector.
 */
public class ObjectDetectorProcessor extends VisionProcessorBase<List<DetectedObject>> {

    private static final String TAG = "ObjectDetectorProcessor";

    private final ObjectDetector detector;

    private Queue<List<DetectedObject>> objectDetectQueue_;

    private long timer = 0;
    private int cnt = 0;
    SoundPool soundPool;
    int shutterSound;
    private String[] categoriesRobot = {"barber chair", "binoculars", "bobsled", "chain saw",
            "combination lock", "corkscrew", "forklift", "go-kart", "golfcart", "half track", "lawn mower",
            "limousine", "mousetrap", "plunger", "Polaroid camera", "racer",
            "rocking chair", "shovel", "seat belt", "toilet tissue", "toilet seat", "tricycle", "vacuum"};
    private String[] categoriesPuck = {"bottlecap,", "buckle", "chain mail", "croquet ball", "face powder", "pill bottle", "ping-pong ball", "puck", "switch" };

    public ObjectDetectorProcessor(Context context, ObjectDetectorOptionsBase options, Queue<List<DetectedObject>> objectDetectQueue) {
        super(context);
        detector = ObjectDetection.getClient(options);
        objectDetectQueue_ = objectDetectQueue;
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).build();
        shutterSound = soundPool.load(context, R.raw.shutter, 1);
    }

    public ObjectDetectorProcessor(Context context, ObjectDetectorOptionsBase options) {
        super(context);
        detector = ObjectDetection.getClient(options);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).build();
        shutterSound = soundPool.load(context, R.raw.shutter, 1);
    }

    @Override
    public void stop() {
        super.stop();
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close object detector!", e);
        }
    }

    @Override
    protected Task<List<DetectedObject>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @Override
    protected void onSuccess(
            @NonNull List<DetectedObject> results, @NonNull GraphicOverlay graphicOverlay, Bitmap originalCameraImage) {

        for (DetectedObject object : results) {
            graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
            // Just take the first in the list as it will have the highest confidence
            if (!object.getLabels().isEmpty()){
                if (System.nanoTime() > timer){
                    double framerate = 1;
                    long frameRate = (long)(framerate * 1000000000); // 1 second in nanoseconds
                    timer = System.nanoTime() + frameRate;
                    String label = object.getLabels().get(0).getText();
                    Rect bb = object.getBoundingBox();
                    Log.i(TAG, "id:" + object.getTrackingId() + ", tag:" + label);
                    Bitmap croppedImage = cropBitmap(originalCameraImage, bb);
                    saveBitmap(croppedImage, label);
                    playsound();
                }
            }
        }
        if (objectDetectQueue_ != null && results.size() > 0){
            objectDetectQueue_.offer(results);
//            Log.i("rect234", results.get(0).getBoundingBox().flattenToString());
//            Log.i("rect234", "image width: " + graphicOverlay.getImageWidth());
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Object detection failed!", e);
    }

    private void playsound(){
        if (soundPool != null){
            soundPool.play(shutterSound,1,1,1,0,1);
        }
    };

    private void saveBitmap(Bitmap bitmap, String label) {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/dataCollection/");
        String photoName = label + "_" + Long.toString(System.nanoTime());
        boolean success = true;
        if (!directory.exists()) {
            success = directory.mkdirs();
        }
        if (success) {
            try (FileOutputStream out = new FileOutputStream(directory + "/" + photoName)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (IOException e) {
                Log.e(TAG,"Error", e);
            }
        }
    }

    private Bitmap cropBitmap(Bitmap bitmap, Rect bb){
        return Bitmap.createBitmap(bitmap, bb.left, bb.top, bb.width(), bb.height());
    }
}
