package jp.oist.abcvlib.basicqrreceiver;

import android.os.Bundle;
import android.widget.TextView;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.phone.QRCodeData;
import jp.oist.abcvlib.core.inputs.phone.QRCodeDataSubscriber;
import jp.oist.abcvlib.util.QRCode;
import jp.oist.abcvlib.util.SerialCommManager;
import jp.oist.abcvlib.util.SerialReadyListener;
import jp.oist.abcvlib.util.UsbSerial;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements SerialReadyListener,
        QRCodeDataSubscriber {

    private QRCode qrCode;
    private PublisherManager publisherManager;
    private float speedL = 0;
    private float speedR = 0;
    private float speed = 0;
    TextView letterTextView;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        letterTextView = findViewById(R.id.letterTextView);
    }

    @Override
    public void onSerialReady(UsbSerial usbSerial) {
        publisherManager = new PublisherManager();

        QRCodeData qrCodeData = new QRCodeData.Builder(this, publisherManager, this).build();
        qrCodeData.addSubscriber(this);

        publisherManager.initializePublishers();
        publisherManager.startPublishers();

        setSerialCommManager(new SerialCommManager(usbSerial));
        super.onSerialReady(usbSerial);
    }

    @Override
    public void onOutputsReady() {
        publisherManager.initializePublishers();
        publisherManager.startPublishers();
    }

    @Override
    public void onQRCodeDetected(String qrDataDecoded) {
        if (qrDataDecoded.equals("L")){
            turnLeft();
        } else if (qrDataDecoded.equals("R")){
            turnRight();
        }
    }

    // Main loop for any application extending AbcvlibActivity. This is where you will put your main code
    @Override
    protected void abcvlibMainLoop(){
        outputs.setWheelOutput(speedL, speedR, false, false);
    }

    private void turnRight(){
        speedL = -speed;
        speedR = speed;
        letterTextView.setText("R");
    }

    private void turnLeft(){
        speedL = speed;
        speedR = -speed;
        letterTextView.setText("L");
        }
}
