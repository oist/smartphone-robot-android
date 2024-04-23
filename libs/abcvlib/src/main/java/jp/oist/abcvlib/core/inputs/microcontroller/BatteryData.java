package jp.oist.abcvlib.core.inputs.microcontroller;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.Publisher;

public class BatteryData extends Publisher<BatteryDataSubscriber> {

    public BatteryData(Context context, PublisherManager publisherManager){
        super(context, publisherManager);
    }

    public static class Builder{
        private final Context context;
        private final PublisherManager publisherManager;

        public Builder(Context context, PublisherManager publisherManager){
            this.context = context;
            this.publisherManager = publisherManager;
        }

        public BatteryData build(){
            return new BatteryData(context, publisherManager);
        }
    }

    public void onBatteryVoltageUpdate(long timestamp, double voltage) {
        for (BatteryDataSubscriber subscriber: subscribers){
            handler.post(() -> {
                if (!paused){
                    subscriber.onBatteryVoltageUpdate(timestamp, voltage);
                }
            });
        }
    }

    public void onChargerVoltageUpdate(long timestamp, double chargerVoltage, double coilVoltage) {
        for (BatteryDataSubscriber subscriber: subscribers){
            handler.post(() -> {
                if (!paused){
                    subscriber.onChargerVoltageUpdate(timestamp, chargerVoltage, coilVoltage);
                }
            });
        }
    }

    @Override
    public void start() {
        mHandlerThread = new HandlerThread("batteryThread");
        mHandlerThread.start();
        handler = new Handler(mHandlerThread.getLooper());
        publisherManager.onPublisherInitialized();
    }

    @Override
    public void stop() {
        mHandlerThread.quitSafely();
        handler = null;
    }

    @Override
    public ArrayList<String> getRequiredPermissions() {
        return new ArrayList<>();
    }
}