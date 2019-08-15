package com.takusemba.rtmppublisher;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;

public class RtmpPuller implements Puller, LifecycleObserver {
    private StreamerPuller streamerPuller;

    private String url;
    private int audioBitrate;

    RtmpPuller(AppCompatActivity activity,
               String url,
               int audioBitrate,
               PublisherListener publisherListener, AudioManager audioManager) {

        activity.getLifecycle().addObserver(this);

        this.url = url;
        this.audioBitrate = audioBitrate;

        this.streamerPuller = new StreamerPuller(audioManager);
        this.streamerPuller.setMuxerListener(publisherListener);
    }

    @Override
    public void startPulling() {
        streamerPuller.open(url);
        streamerPuller.startStreaming(audioBitrate);
    }

    @Override
    public void stopPulling() {
        if (streamerPuller.isStreaming()) {
            streamerPuller.stopStreaming();
        }
    }

    @Override
    public boolean isPulling() {
        return streamerPuller.isStreaming();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume(LifecycleOwner owner) {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause(LifecycleOwner owner) {
        if (streamerPuller.isStreaming()) {
            streamerPuller.stopStreaming();
        }
    }
}
