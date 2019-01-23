package jp.oist.abcvlib.vision;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import jp.oist.abcvlib.basic.AbcvlibLooper;
import jp.oist.abcvlib.basic.AbcvlibMotion;
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

    // Create objects of each module class
    AbcvlibSensors abcvlibSensors = new AbcvlibSensors(this);
    AbcvlibMotion abcvlibMotion = new AbcvlibMotion(abcvlibSensors);
    Camera mCamera = new Camera(abcvlibSensors);

    public IOIOLooper createIOIOLooper() {
        return new AbcvlibLooper(abcvlibSensors, abcvlibMotion);
    }

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
