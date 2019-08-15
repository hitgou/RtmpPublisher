package com.today.im.opus;

public interface PublisherListener {

    /**
     * Called when {@link Publisher} started publishing
     */
    void onPublishStarted();

    /**
     * Called when {@link Publisher} stopped publishing
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
