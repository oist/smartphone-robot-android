package jp.oist.abcvlib.core.inputs.phone;

import android.graphics.Bitmap;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import java.util.List;
import jp.oist.abcvlib.core.inputs.Subscriber;

public interface ObjectDetectorDataSubscriber extends Subscriber {

    void onObjectsDetected(Bitmap bitmap, TensorImage tensorImage, List<Detection> results, long inferenceTime, int height, int width);
}
