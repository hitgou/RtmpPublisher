package com.takusemba.rtmppublisher;

import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.today.im.IMMuxer;
import com.today.im.PacketInfo;

class StreamerPuller {
    private final static String TAG = "StreamerPuller";

    private AudioHandler audioHandler;
    private IMMuxer imMuxer = new IMMuxer();
    private boolean isPlaying = false;
    private PublisherListener publisherListener;
    private AudioManager audioManager;

    StreamerPuller(AudioManager audioManager) {
        this.audioHandler = new AudioHandler();
        this.audioManager = audioManager;
    }

    void open(String url) {
        imMuxer.pullWithUrl(url);
    }

    void startStreaming(int audioBitrate) {
        isPlaying = true;

        final Handler uiHandler = new Handler(Looper.getMainLooper());
        if (publisherListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    publisherListener.onStarted();
                }
            });
        }

        drain();
    }

    void stopStreaming() {
        isPlaying = false;
        audioHandler.stop();
        imMuxer.stopPull();

        final Handler uiHandler = new Handler(Looper.getMainLooper());
        if (publisherListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    publisherListener.onStopped();
                }
            });
        }
    }

    boolean isStreaming() {
        return isPlaying;
//        return imMuxer.isConnected() == 1;
    }

    private void drain() {
        final AudioPlayerHandler audioPlayerHandler = new AudioPlayerHandler(audioManager);
        audioPlayerHandler.prepare();

        HandlerThread handlerThread = new HandlerThread("AudioEncoder-drain");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                while (isPlaying) {
                    PacketInfo packet = imMuxer.read();
                    if (packet == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    Log.d(TAG, "接收到消息包：size=" + packet.bodySize);
                    if (packet.packetType == Muxer.MSG_SEND_AUDIO) {
                        audioPlayerHandler.onPlaying(packet.body, 0, packet.body.length);
                    }
                }
                release();

                audioPlayerHandler.release();
            }
        });
    }

    private void release() {

    }

    void setMuxerListener(PublisherListener listener) {
        this.publisherListener = listener;
    }

}
