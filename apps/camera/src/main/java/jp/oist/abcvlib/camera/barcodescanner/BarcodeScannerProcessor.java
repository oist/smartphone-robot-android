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

package jp.oist.abcvlib.camera.barcodescanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import jp.oist.abcvlib.camera.GraphicOverlay;
import jp.oist.abcvlib.camera.R;
import jp.oist.abcvlib.camera.VisionProcessorBase;

import java.util.List;
import java.util.Locale;

/**
 * Barcode Detector Demo.
 */
public class BarcodeScannerProcessor extends VisionProcessorBase<List<Barcode>>{

    private static final String TAG = "BarcodeProcessor";

    private final BarcodeScanner barcodeScanner;

    SoundPool soundPool;
    int mateSound;
    private long timer;
    public boolean speechReady = false;
    private String barcodeText = "";

    public boolean qrCodeVisible = false;

    public BarcodeScannerProcessor(Context context) {
        super(context);
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE)
                        .build();
        barcodeScanner = BarcodeScanning.getClient(options);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).build();
        mateSound = soundPool.load(context, R.raw.matesound, 1);
        timer = 0;
        qrCodeVisible = false;
    }

    @Override
    public void stop() {
        super.stop();
        barcodeScanner.close();
    }

    @Override
    protected Task<List<Barcode>> detectInImage(InputImage image) {
        return barcodeScanner.process(image);
    }

    @Override
    protected void onSuccess(
            @NonNull List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay, Bitmap originalCameraImage) {
        if (barcodes.isEmpty()) {
            Log.v(MANUAL_TESTING_LOG, "No barcode has been detected");
            qrCodeVisible = false;
            // Reset barcode text to allow new barcodes to be parsed
            barcodeText = "";
        }else{
            qrCodeVisible = true;

            for (int i = 0; i < barcodes.size(); ++i) {
                Barcode barcode = barcodes.get(i);
                graphicOverlay.add(new BarcodeGraphic(graphicOverlay, barcode));
                logExtrasForTesting(barcode);
                if (!barcodeText.equals(barcode.getRawValue())){
                    barcodeText = barcode.getRawValue();
                    // Play success/fireworks sound/animation only if new barcode text present
                    mateAnimation(barcodeText);
                }
            }
        }
    }

    private static void logExtrasForTesting(Barcode barcode) {
        if (barcode != null) {
            Log.v(
                    MANUAL_TESTING_LOG,
                    String.format(
                            "Detected barcode's bounding box: %s", barcode.getBoundingBox().flattenToString()));
            Log.v(
                    MANUAL_TESTING_LOG,
                    String.format(
                            "Expected corner point size is 4, get %d", barcode.getCornerPoints().length));
            for (Point point : barcode.getCornerPoints()) {
                Log.v(
                        MANUAL_TESTING_LOG,
                        String.format("Corner point is located at: x = %d, y = %d", point.x, point.y));
            }
            Log.v(MANUAL_TESTING_LOG, "barcode display value: " + barcode.getDisplayValue());
            Log.v(MANUAL_TESTING_LOG, "barcode raw value: " + barcode.getRawValue());
            Barcode.DriverLicense dl = barcode.getDriverLicense();
            if (dl != null) {
                Log.v(MANUAL_TESTING_LOG, "driver license city: " + dl.getAddressCity());
                Log.v(MANUAL_TESTING_LOG, "driver license state: " + dl.getAddressState());
                Log.v(MANUAL_TESTING_LOG, "driver license street: " + dl.getAddressStreet());
                Log.v(MANUAL_TESTING_LOG, "driver license zip code: " + dl.getAddressZip());
                Log.v(MANUAL_TESTING_LOG, "driver license birthday: " + dl.getBirthDate());
                Log.v(MANUAL_TESTING_LOG, "driver license document type: " + dl.getDocumentType());
                Log.v(MANUAL_TESTING_LOG, "driver license expiry date: " + dl.getExpiryDate());
                Log.v(MANUAL_TESTING_LOG, "driver license first name: " + dl.getFirstName());
                Log.v(MANUAL_TESTING_LOG, "driver license middle name: " + dl.getMiddleName());
                Log.v(MANUAL_TESTING_LOG, "driver license last name: " + dl.getLastName());
                Log.v(MANUAL_TESTING_LOG, "driver license gender: " + dl.getGender());
                Log.v(MANUAL_TESTING_LOG, "driver license issue date: " + dl.getIssueDate());
                Log.v(MANUAL_TESTING_LOG, "driver license issue country: " + dl.getIssuingCountry());
                Log.v(MANUAL_TESTING_LOG, "driver license number: " + dl.getLicenseNumber());
            }
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Barcode detection failed " + e);
    }

    private void mateAnimation(String barcodeText){
//        if (System.nanoTime() > timer){
//            // 3 seconds in nanoseconds
//            long mateRate = (long) (5.0 * 1000000000);
//            timer = System.nanoTime() + mateRate;
//            playsound(barcodeText);
//        }
        if(speechReady){
            playsound(barcodeText);
        }
    }

    private void playsound(String barcodeText){
        if (soundPool != null){
            soundPool.play(mateSound,1,1,1,0,1);
        }
    };

}
