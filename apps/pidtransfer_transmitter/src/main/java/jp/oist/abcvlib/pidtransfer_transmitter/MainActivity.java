package jp.oist.abcvlib.pidtransfer_transmitter;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.Objects;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.outputs.AbcvlibController;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    private Button showQRCode;
    private boolean isQRDisplayed = false;
    private PID_GUI pid_view;
    private QRCodeDisplay qrCodeDisplay;
    private CustomController customController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Various switches are available to turn on/off core functionality.
        switches.balanceApp = true;
        switches.pythonControlApp = true;
        switches.wheelPolaritySwap = false;

        customController = new CustomController(this);

        //Todo pass outputs to PID fragment somehow

        // Note the previously optional parameters that handle the connection to the python server
        initialzer(this,"192.168.20.195", 3000, customController);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).hide();

        showQRCode = findViewById(R.id.show_qr_button);
        showQRCode.setOnClickListener(qrCodeButtonClickListener);

        displayPID_GUI();
    }

    private final View.OnClickListener qrCodeButtonClickListener = v -> {
        if (!isQRDisplayed) {
            qrCodeDisplay = QRCodeDisplay.newInstance(pid_view);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.main_fragment, qrCodeDisplay).commit();
            showQRCode.setText(R.string.back_button);
            isQRDisplayed = true;
        } else {
            displayPID_GUI();
        }

    };

    public void displayPID_GUI(){
        pid_view = PID_GUI.newInstance(outputs);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment, pid_view).commit();
        showQRCode.setText(R.string.qr_button_show);
        isQRDisplayed = false;
    }

}
