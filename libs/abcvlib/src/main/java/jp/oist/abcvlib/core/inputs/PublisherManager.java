package jp.oist.abcvlib.core.inputs;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.Phaser;

/**
 * Manages the permission lifecycle of a group of publishers
 * In order to synchronize the lifecycle of all publishers, this creates a Phaser that waits for
 * each phase to finish for all publishers before allowing the next phase to start.
 * phase 0 = permissions of publisher objects
 * phase 1 = initialization of publisher object streams/threads
 * phase 2 = initialize publisher objects (i.e. initialize recording data)
 */
public class PublisherManager {
    private final ArrayList<Publisher<?>> publishers = new ArrayList<>();
    private final Phaser phaser = new Phaser(1);
    private final String TAG = getClass().getName();

    public PublisherManager(){
    }

    //========================================Phase 0===============================================
    public PublisherManager add(Publisher<?> publisher){
        publishers.add(publisher);
        phaser.register();
        return this;
    }
    public void onPublisherPermissionsGranted() {
        phaser.arriveAndDeregister();
    }

    //========================================Phase 1===============================================
    private void initialize(@NotNull Publisher<?> publisher){
        phaser.register();
        publisher.start();
    }

    public void onPublisherInitialized() {
        phaser.arriveAndDeregister();
    }

    public void initializePublishers(){
        phaser.arrive();
        Log.i(TAG, "Waiting on all publishers to initialize before starting");
        phaser.awaitAdvance(0); // Waits to initialize if not finished with initPhase
        for (Publisher<?> publisher: publishers){
            initialize(publisher);
        }
    }

    //========================================Phase 2===============================================
    public void startPublishers(){
        phaser.arrive();
        Log.i(TAG, "Waiting on all publishers to initialize before starting");
        phaser.awaitAdvance(1);
        for (Publisher<?> publisher: publishers){
            publisher.resume(); // set pause to false now that all publishers are initialized. Setting pause to false will initialize recording from the existing streams.
        }
    }

    //====================================Non-phase Related=========================================
    public void pausePublishers(){
        for (Publisher<?> publisher: publishers){
            publisher.pause();
        }
    }
    public void resumePublishers(){
        for (Publisher<?> publisher: publishers){
            publisher.resume();
        }
    }
    public void stopPublishers(){
        for (Publisher<?> publisher: publishers){
            publisher.stop();
        }
    }
    @SuppressWarnings("unused")
    public ArrayList<Publisher<?>> getPublishers() {
        return publishers;
    }
}
