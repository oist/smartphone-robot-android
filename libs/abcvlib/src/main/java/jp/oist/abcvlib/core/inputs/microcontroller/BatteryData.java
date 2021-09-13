package jp.oist.abcvlib.core.inputs.microcontroller;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.Publisher;

public class BatteryData extends Publisher<BatteryDataSubscriber> {
    private final AbcvlibLooper abcvlibLooper;

    public BatteryData(Context context, PublisherManager publisherManager, AbcvlibLooper abcvlibLooper){
        super(context, publisherManager);
        this.abcvlibLooper = abcvlibLooper;
    }

    public static class Builder{
        private final Context context;
        private final PublisherManager publisherManager;
        private final AbcvlibLooper abcvlibLooper;

        public Builder(Context context, PublisherManager publisherManager, AbcvlibLooper abcvlibLooper){
            this.context = context;
            this.publisherManager = publisherManager;
            this.abcvlibLooper = abcvlibLooper;
        }

        public BatteryData build(){
            return new BatteryData(context, publisherManager, abcvlibLooper);
        }
    }

    public void onBatteryVoltageUpdate(double voltage, long timestamp) {
        for (BatteryDataSubscriber subscriber: subscribers){
            handler.post(() -> {
                if (!paused){
                    subscriber.onBatteryVoltageUpdate(voltage, timestamp);
                }
            });
        }
    }

    public void onChargerVoltageUpdate(double chargerVoltage, double coilVoltage, long timestamp) {
        for (BatteryDataSubscriber subscriber: subscribers){
            handler.post(() -> {
                if (!paused){
                    subscriber.onChargerVoltageUpdate(chargerVoltage, coilVoltage, timestamp);
                }
            });
        }
    }

    @Override
    public void start() {
        mHandlerThread = new HandlerThread("batteryThread");
        mHandlerThread.start();
        handler = new Handler(mHandlerThread.getLooper());
        abcvlibLooper.setBatteryData(this);
        publisherManager.onPublisherInitialized();
    }

    @Override
    public void stop() {
        abcvlibLooper.setBatteryData(null);
        mHandlerThread.quitSafely();
        handler = null;
    }

    @Override
    public ArrayList<String> getRequiredPermissions() {
        return new ArrayList<>();
    }
}