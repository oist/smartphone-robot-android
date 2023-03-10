package jp.oist.abcvlib.core.inputs.phone;

import org.tensorflow.lite.task.vision.detector.Detection;
import java.util.List;
import jp.oist.abcvlib.core.inputs.Subscriber;

public interface ObjectDetectorDataSubscriber extends Subscriber {

    void onResults(List<Detection> results, long inferenceTime, int height, int width);
}
