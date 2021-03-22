package jp.oist.abcvlib.compoundcontroller;

import android.os.Bundle;

import org.json.JSONException;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.outputs.AbcvlibController;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * Shows how to setup custom controller in conjunction with the the PID balance controller.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Various switches are available to turn on/off core functionality.
        switches.balanceApp = true;
        switches.pythonControlledPIDBalancer = true;

        // Initializer your custom controller before the general initializer such that it can be
        // passed upward.
        CustomController customController = new CustomController();

        initialzer(this,"192.168.20.26", 65434, customController);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        // Only start your controller after everything else is setup such that it avoids trying to
        // access objects that have not yet been initialized
        new Thread(customController).start();

    }

    /**
     * Simple proportional controller trying to achieve some setSpeed set by python server GUI.
     */
    public class CustomController extends AbcvlibController {

        double actualSpeed = 0;
        double errorSpeed = 0;

        double setSpeed = 0; // mm/s.
        double d_s = 0; // derivative controller for speed of wheels

        public void run(){

            while(appRunning) {

                actualSpeed = inputs.quadEncoders.getWheelSpeedL_LP();

                errorSpeed = setSpeed - actualSpeed;

                if (outputs.socketClient.socketMsgIn != null) {

                    try {
                        setSpeed = Double.parseDouble(outputs.socketClient.socketMsgIn.get("wheelSpeedL").toString());
                        d_s = Double.parseDouble(outputs.socketClient.socketMsgIn.get("wheelSpeedControl").toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                // Note the use of the same output for controlling both wheels. Due to various errors
                // that build up over time, controling individual wheels has so far led to chaos
                // and unstable controllers.
                setOutput( (errorSpeed * d_s),
                        (errorSpeed * d_s));
                Thread.yield();

            }
        }
    }
}
