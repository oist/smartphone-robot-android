package jp.oist.abcvlib.backandforth;

import static java.lang.Thread.sleep;

import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.util.SerialCommManager;
import jp.oist.abcvlib.util.SerialReadyListener;
import jp.oist.abcvlib.util.SerialResponseListener;
import jp.oist.abcvlib.util.UsbSerial;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements SerialResponseListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);
    }

    Runnable backAndForth = new Runnable() {
        float speed = 0.50f;
        float increment = 0.1f;

        @Override
        public void run() {
            System.out.println("Current speed: " + speed);
            serialCommManager.setMotorLevels(speed, speed);
            if (speed >= 1.00f || speed <= -1.00f) {
                increment = -increment;
            }
            speed += increment;
        }
    };

    @Override
    public void onSerialReady(UsbSerial usbSerial) {
        serialCommManager = new SerialCommManager(usbSerial, backAndForth);
        serialCommManager.start();
        super.onSerialReady(usbSerial);
    }
}
