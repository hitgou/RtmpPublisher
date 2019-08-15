package com.takusemba.rtmppublisher;

import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

public interface Puller {

    /**
     * start publishing video and audio data
     */
    void startPulling();

    /**
     * stop publishing video and audio data.
     */
    void stopPulling();

    /**
     * @return if the Publisher is publishing data.
     */
    boolean isPulling();


    class Builder {

        /**
         * Default Values
         */
        public static final int DEFAULT_WIDTH = 720;
        public static final int DEFAULT_HEIGHT = 1280;
        public static final int DEFAULT_AUDIO_BITRATE = 6400;
        public static final int DEFAULT_VIDEO_BITRATE = 100000;
        public static final CameraMode DEFAULT_MODE = CameraMode.BACK;

        /**
         * Required Parameters
         */
        private AppCompatActivity activity;
        private String url;

        private int audioBitrate;
        private PublisherListener publisherListener;
        private AudioManager audioManager;


        /**
         * Constructor of the {@link Builder}
         */
        public Builder(@NonNull AppCompatActivity activity) {
            this.activity = activity;
        }


        /**
         * Set the RTMP url
         * this parameter is required
         */
        public Builder setUrl(@NonNull String url) {
            this.url = url;
            return this;
        }


        /**
         * Set the audio bitrate used for RTMP Streaming
         * this parameter is optional
         */
        public Builder setAudioBitrate(int audioBitrate) {
            this.audioBitrate = audioBitrate;
            return this;
        }


        /**
         * Set the {@link PublisherListener}
         * this parameter is optional
         */
        public Builder setPublisherListener(PublisherListener publisherListener) {
            this.publisherListener = publisherListener;
            return this;
        }

        public Builder setAutoManager(AudioManager audioManager) {
            this.audioManager = audioManager;
            return this;
        }

        /**
         * @return the created RtmpPublisher
         */
        public RtmpPuller build() {
            if (activity == null) {
                throw new IllegalStateException("activity should not be null");
            }
            if (url == null || url.isEmpty()) {
                throw new IllegalStateException("url should not be empty or null");
            }
            if (url == null || audioBitrate <= 0) {
                audioBitrate = DEFAULT_AUDIO_BITRATE;
            }
            return new RtmpPuller(activity, url, audioBitrate, publisherListener, audioManager);
        }

    }
}
