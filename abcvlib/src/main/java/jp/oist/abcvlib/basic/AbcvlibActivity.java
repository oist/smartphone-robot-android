package jp.oist.abcvlib.basic;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;

import java.util.ArrayList;
import java.util.List;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import static android.Manifest.permission.CAMERA;

/**
 * AbcvlibActivity is where all of the other classes are initialized into objects. The objects
 * are then passed to one another in order to coordinate the various shared values between them.
 *
 * Android app MainActivity can start motion by extending AbcvlibActivity and then running
 * any of the methods within the object instance abcvlibMotion within an infinite threaded loop
 * e.g:
 *
 * @author Christopher Buckley https://github.com/topherbuckley
 *
 */
public class AbcvlibActivity extends IOIOActivity {

    public AbcvlibSensors abcvlibSensors;
    public AbcvlibMotion abcvlibMotion;
    public AbcvlibQuadEncoders abcvlibQuadEncoders;
    public AbcvlibSaveData abcvlibSaveData;

    /**
     * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
     * memory/disk space on the phone and may result in memory failure if run for a long time.
     */
    public boolean loggerOn = false;

    /**
     * Enable/disable this
     */
    public boolean wheelPolaritySwap = false;

    /**
     *  Not sure why initial PWM_FREQ is 1000, but assume this can be modified as necessary.
     *  This may depend on the motor or microcontroller requirements/specs. <br><br>
     *
     *  If motor is just a DC motor, I guess this does not matter much, but for servos, this would
     *  be the control function, so would have to match the baud rate of the microcontroller. Note
     *  this library is not set up to control servos at this time. <br><br>
     *
     *  The microcontroller likely has a maximum frequency which it can turn ON/OFF the IO, so
     *  setting PWM_FREQ too high may cause issues for certain microcontrollers.
     */
    private final int PWM_FREQ = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize AbcvlibSensors and AbcvlibMotion objects.
        abcvlibSensors = new AbcvlibSensors(getBaseContext(), loggerOn);
        abcvlibQuadEncoders = new AbcvlibQuadEncoders(loggerOn);
        abcvlibMotion = new AbcvlibMotion(abcvlibSensors, abcvlibQuadEncoders, PWM_FREQ);
        abcvlibSaveData = new AbcvlibSaveData();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        Toast.makeText(this, "In onStop", Toast.LENGTH_LONG).show();
        Log.i("abcvlib", "onStop Log");
        abcvlibMotion.setWheelSpeed(0,0);
        super.onStop();
    }

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return new ArrayList<CameraBridgeViewBase>();
    }

    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     Overriding here passes the initialized AbcvlibLooper object to the IOIOLooper class which
     then connects the object to the actual IOIOBoard. There is no need to start a separate thread
     or create a global objects in the Android App MainActivity or otherwise.
      */
    @Override
    protected IOIOLooper createIOIOLooper() {
        return new AbcvlibLooper(abcvlibSensors, abcvlibMotion, abcvlibQuadEncoders,PWM_FREQ, loggerOn, wheelPolaritySwap);
    }

}
