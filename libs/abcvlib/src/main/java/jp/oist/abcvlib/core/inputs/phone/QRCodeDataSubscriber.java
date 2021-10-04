package jp.oist.abcvlib.core.inputs.phone;

import jp.oist.abcvlib.core.inputs.Subscriber;

public interface QRCodeDataSubscriber extends Subscriber {
    void onQRCodeDetected(String qrDataDecoded);
}
