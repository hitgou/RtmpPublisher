package com.today.im.opus;

public interface PullerListener {

    /**
     * Called when {@link PullerListener} started publishing
     */
    void onPullStarted();

    /**
     * Called when {@link PullerListener} stopped publishing
     */
    void onPullStopped();

    /**
     * Called when stream is disconnected
     */
    void onPullDisconnected();

    /**
     * Called when failed to connect
     */
    void onPullFailedToConnect();

}
