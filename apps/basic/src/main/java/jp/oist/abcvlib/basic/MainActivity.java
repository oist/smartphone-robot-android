package jp.oist.abcvlib.basic;

import android.os.Bundle;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework, and a simple log output of
 * theta and angular velocity via Logcat using onboard Android sensors.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initalizes various objects in parent class.
        try {
            initializer(this);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        setContentView(R.layout.activity_main);

        // Executors preferred over runnables or threads for built in memory/cleanup/error handling.
        ScheduledExecutorServiceWithException executor = new ScheduledExecutorServiceWithException(1, new ProcessPriorityThreadFactory(5, "SimpleTest"));
        SimpleTest simpleTest = new SimpleTest(this);
        // Run the simpleTest every 1 second
        executor.scheduleAtFixedRate(simpleTest, 0, 1, TimeUnit.SECONDS);
    }

}

