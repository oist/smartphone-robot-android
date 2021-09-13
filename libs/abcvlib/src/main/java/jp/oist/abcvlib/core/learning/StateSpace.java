package jp.oist.abcvlib.core.learning;

import jp.oist.abcvlib.core.inputs.PublisherManager;

public class StateSpace {
    public final PublisherManager publisherManager;
    public StateSpace(PublisherManager publisherManager){
        this.publisherManager = publisherManager;
    }
}
