package com.today.im.opus;

public interface PublisherListener {

    /**
     * Called when {@link PublisherTask} started publishing
     */
    void onPublishStarted();

    /**
     * Called when {@link PublisherTask} stopped publishing
     */
    void onPublishStopped();

    /**
     * Called when stream is disconnected
     */
    void onPublishDisconnected();

    /**
     * Called when failed to connect
     */
    void onPublishFailedToConnect();

}
