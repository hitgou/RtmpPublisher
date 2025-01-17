package com.takusemba.rtmppublisher;

import android.opengl.EGLContext;

import com.today.im.IMMuxer;

class Streamer
        implements VideoHandler.OnVideoEncoderStateListener, AudioHandler.OnAudioEncoderStateListener {

    private VideoHandler videoHandler;
    private AudioHandler audioHandler;
    private Muxer muxer = new Muxer();
    private IMMuxer imMuxer = new IMMuxer();

    Streamer() {
        this.videoHandler = new VideoHandler();
        this.audioHandler = new AudioHandler();
    }

    void open(String url, int width, int height) {
        muxer.open(url, width, height);
    }

    void startStreaming(EGLContext context, int width, int height, int audioBitrate,
                        int videoBitrate) {
        if (imMuxer.isConnected() == 1) {
            long startStreamingAt = System.currentTimeMillis();
            videoHandler.setOnVideoEncoderStateListener(this);
            audioHandler.setOnAudioEncoderStateListener(this);
            videoHandler.start(width, height, videoBitrate, context, startStreamingAt);
            audioHandler.start(audioBitrate, startStreamingAt);
        }
    }

    void stopStreaming() {
        videoHandler.stop();
        audioHandler.stop();
        muxer.close();
    }

    boolean isStreaming() {
        return imMuxer.isConnected() == 1;
    }

    @Override
    public void onVideoDataEncoded(byte[] data, int size, int timestamp) {
        muxer.sendVideo(data, size, timestamp);
    }

    @Override
    public void onAudioDataEncoded(byte[] data, int size, int timestamp) {
        muxer.sendAudio(data, size, timestamp);
    }

    CameraSurfaceRenderer.OnRendererStateChangedListener getVideoHandlerListener() {
        return videoHandler;
    }

    void setMuxerListener(PublisherListener listener) {
        muxer.setOnMuxerStateListener(listener);
    }

}
