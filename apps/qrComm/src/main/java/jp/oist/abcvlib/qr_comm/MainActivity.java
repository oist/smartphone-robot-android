package jp.oist.abcvlib.qr_comm;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.ImageDataSubscriber;
import jp.oist.abcvlib.core.inputs.phone.QRCodeData;
import jp.oist.abcvlib.core.inputs.phone.QRCodeDataSubscriber;
import jp.oist.abcvlib.util.QRCode;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements IOReadyListener, ImageDataSubscriber, QRCodeDataSubscriber {

    private Button showQRCode;
    private boolean isQRDisplayed = false;
    private QRCode qrCode;
    private ImageData imageData;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setIoReadyListener(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        showQRCode = findViewById(R.id.show_qr_button);
        showQRCode.setOnClickListener(qrCodeButtonClickListener);

        // create a new QRCode object with input args point to the FragmentManager and your layout fragment where you want to generate the qrcode image.
        qrCode = new QRCode(getSupportFragmentManager(), R.id.qrFragmentView);
    }

    @Override
    public void onIOReady(AbcvlibLooper abcvlibLooper) {
        PublisherManager publisherManager = new PublisherManager();
        imageData = new ImageData.Builder(this, publisherManager, this)
                .setPreviewView(findViewById(jp.oist.abcvlib.core.R.id.preview_view))
                .build();
        imageData.addSubscriber(this);

        QRCodeData qrCodeData = new QRCodeData.Builder(this, publisherManager, this).build();
        qrCodeData.addSubscriber(this);

        publisherManager.initializePublishers();
        publisherManager.startPublishers();
    }

    private final View.OnClickListener qrCodeButtonClickListener = v -> {
        if (!isQRDisplayed) {
            // generate new qrcode using the string you want to encode. Use JSONObject.toString for more complex data sets.
            qrCode.generate("Hello World!");
            showQRCode.setText(R.string.back_button_text);
            isQRDisplayed = true;
            imageData.setQrcodescanning(false);
        } else {
            // removes the fragment that holds the last generated qrcode.
            qrCode.close();
            isQRDisplayed = false;
            showQRCode.setText(R.string.qr_button_show);
            imageData.setQrcodescanning(true);
        }
    };

    @Override
    public void onImageDataUpdate(long timestamp, int width, int height, Bitmap bitmap, String qrDecodedData) {
    }

    @Override
    public void onQRCodeDetected(String qrDataDecoded) {
        if (!qrDataDecoded.equals("")){
            Log.i("qrcode", "QR Code Found and decoded: " + qrDataDecoded);
        }
    }
}
