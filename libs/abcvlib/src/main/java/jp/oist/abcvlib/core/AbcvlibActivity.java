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

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import jp.oist.abcvlib.core.outputs.Outputs;
import jp.oist.abcvlib.util.SerialCommManager;
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
public abstract class AbcvlibActivity extends AppCompatActivity implements SerialReadyListener {

    private Outputs outputs;
    private Switches switches = new Switches();
    private static final String TAG = "abcvlib";
    private IOReadyListener ioReadyListener;
    protected UsbSerial usbSerial;
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

    public void onEncoderCountsRec(int left, int right){
        Log.d("serial", "Left encoder count: " + left);
        Log.d("serial", "Right encoder count: " + right);
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
        if (serialCommManager != null){
            serialCommManager.setMotorLevels(0, 0, true, true);
            serialCommManager.stop();
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
        outputs = new Outputs(switches, serialCommManager);
    }
}
