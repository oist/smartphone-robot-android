package jp.oist.abcvlib.core;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import jp.oist.abcvlib.core.outputs.Outputs;
import jp.oist.abcvlib.util.SerialCommManager;
import jp.oist.abcvlib.util.SerialResponseListener;
import jp.oist.abcvlib.util.UsbSerial;
import jp.oist.abcvlib.util.SerialReadyListener;

/**
 * AbcvlibActivity is where all of the other classes are initialized into objects. The objects
 * are then passed to one another in order to coordinate the various shared values between them.
 *
 * Android app MainActivity can start Motion by extending AbcvlibActivity and then running
 * any of the methods within the object instance Motion within an infinite threaded loop
 * e.g:
 *
 * @author Christopher Buckley https://github.com/topherbuckley
 *
 */
public abstract class AbcvlibActivity extends AppCompatActivity implements SerialResponseListener {

    private Outputs outputs;
    private Switches switches = new Switches();
    protected AbcvlibLooper abcvlibLooper;
    private static final String TAG = "abcvlib";
    private IOReadyListener ioReadyListener;
    protected UsbSerial usbSerial;
    private SerialResponseListener serialResponseListener;
    protected SerialCommManager serialCommManager;
    private Runnable android2PiWriter = null;
    private Runnable pi2AndroidReader = null;
    AlertDialog alertDialog = null;

    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        usbInitialize();
        super.onCreate(savedInstanceState);
    }

    private void usbInitialize(){
        try {
            this.usbSerial = new UsbSerial(this,
                    (UsbManager) getSystemService(Context.USB_SERVICE),
                    this);
        } catch (IOException e) {
            e.printStackTrace();
            showCustomDialog();
        }
    }

    public void onSerialReady(UsbSerial usbSerial) {
    }

    protected void setAndroi2PiWriter(Runnable android2PiWriter){
        this.android2PiWriter = android2PiWriter;
    }

    protected void setPi2AndroidReader(Runnable pi2AndroidReader){
        this.pi2AndroidReader = pi2AndroidReader;
    }

    private void showCustomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.missing_robot, null);
        builder.setView(dialogView);

        // Find the TextView and Button in the dialog layout
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        // Set a click listener for the Confirm button
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss the dialog
                if (alertDialog != null){
                    alertDialog.dismiss();
                }

                usbInitialize();
            }
        });

        // Create the AlertDialog
        alertDialog = builder.create();

        // Show the dialog
        alertDialog.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "End of AbcvlibActivity.onStop");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (abcvlibLooper != null){
//            abcvlibLooper.setDutyCycle(0, 0);
            abcvlibLooper.shutDown();
        }
        Log.i(TAG, "End of AbcvlibActivity.onPause");
    }

    public Outputs getOutputs() {
        return outputs;
    }

    public Switches getSwitches() {
        return switches;
    }

    public void setSwitches(Switches switches){
        this.switches = switches;
    }

    private void initializeOutputs(){
        outputs = new Outputs(switches, abcvlibLooper);
    }
}
