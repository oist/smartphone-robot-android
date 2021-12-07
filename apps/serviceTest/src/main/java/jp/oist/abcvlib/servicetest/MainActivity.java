package jp.oist.abcvlib.servicetest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import org.jetbrains.annotations.NotNull;

import jp.oist.abcvlib.core.AbcvlibService;
import jp.oist.abcvlib.tests.BackAndForthService;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends Activity {

    private boolean mBound;

    private AbcvlibService abcvlibService;
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to AbcvlibService, cast the IBinder and get AbcvlibService instance
            AbcvlibService.LocalBinder binder = (AbcvlibService.LocalBinder) service;
            abcvlibService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = new Intent(this, BackAndForthService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        startService(intent);
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onPause() {
        abcvlibService.onPause();
        super.onPause();
    }
}
