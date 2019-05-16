package jp.oist.abcvlib.vision;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import jp.oist.abcvlib.basic.AbcvlibLooper;
import jp.oist.abcvlib.basic.AbcvlibMotion;
import jp.oist.abcvlib.basic.AbcvlibQuadEncoders;
import jp.oist.abcvlib.basic.AbcvlibSensors;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

/**
 * Created by iffor_000 on 12/116.
 */
public class CvActivity extends IOIOActivity {

    private CameraBridgeViewBase mOpenCvCameraView;
    TextView tv;

    /**
     * Enable/disable sensor and IO logging. Only set to true when debugging as it uses a lot of
     * memory/disk space on the phone and may result in memory failure if run for a long time.
     */
    protected boolean loggerOn = false;

    /**
     * Enable/disable this
     */
    protected boolean wheelPolaritySwap = false;

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

    // Create objects of each module class
    AbcvlibSensors abcvlibSensors = new AbcvlibSensors(this);
    AbcvlibQuadEncoders abcvlibQuadEncoders = new AbcvlibQuadEncoders();
    AbcvlibMotion abcvlibMotion = new AbcvlibMotion(abcvlibSensors, abcvlibQuadEncoders, PWM_FREQ);
    Camera mCamera = new Camera(abcvlibSensors);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView.setCvCameraViewListener(mCamera);

//        final Handler mHandler = new Handler() {
//
//
//            public void handleMessage(Message msg) {
//                if (msg.what == 1) {
//                    StringBuilder sb=new StringBuilder();
//
//
//                    sb.append("area=" + mCamera.getArea());
//                    sb.append("\ndistance="+String.format(Locale.ROOT, "%.2f", mCamera.getDistance()));
//                    sb.append("\nncx=" + String.format(Locale.ROOT, "%.2f",mCamera.getNcx()));
//                    sb.append("\ncontourdiff=" + String.format(Locale.ROOT, "%.2f", mCamera.getContourdiff()));
//                    sb.append("\nheight="+mOpenCvCameraView.getHeight()+"width="+mOpenCvCameraView.getWidth());
//                    sb.append("\ntheta =" + abcvlibSensors.getThetaRad());
//                    sb.append("\nthetadot=" + abcvlibSensors.getThetaRadDotGyro());
//                    sb.append("\nwheelL=" + abcvlibSensors.getwheelL());
//                    sb.append("\nwheelR=" +abcvlibSensors.getwheelR());
//                    tv.setText(sb.toString());
//
//                }
//
//
//            }
//
//
//        };
//
//        new Timer().schedule(new TimerTask() {
//
//            public void run() {
//                Message msg = new Message();
//                msg.what = 1;
//                mHandler.sendMessage(msg);
//            }
//        }, 0, 50);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        abcvlibSensors.register();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onStop() {
        super.onStop();
        abcvlibSensors.unregister();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    //Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(mCamera);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
}
