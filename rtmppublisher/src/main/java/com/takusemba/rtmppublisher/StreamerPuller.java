package com.takusemba.rtmppublisher;

import android.os.Handler;
import android.os.HandlerThread;

import com.today.im.IMMuxer;
import com.today.im.PacketInfo;

class StreamerPuller {

    private AudioHandler audioHandler;
    private IMMuxer imMuxer = new IMMuxer();
    private boolean isPlaying = false;

    StreamerPuller() {
        this.audioHandler = new AudioHandler();
    }

    void open(String url) {
        imMuxer.playWithUrl(url);
    }

    void startStreaming(int audioBitrate) {
        isPlaying = true;
        drain();
    }

    void stopStreaming() {
        isPlaying = false;
        audioHandler.stop();
        imMuxer.stopPlay();
        imMuxer.stopPull();
    }

    boolean isStreaming() {
        return imMuxer.isConnected() == 1;
    }

    private void drain() {
        final AudioPlayerHandler audioPlayerHandler = new AudioPlayerHandler();
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
                    audioPlayerHandler.onPlaying(packet.body, 0, packet.body.length);
                }
                release();

                audioPlayerHandler.release();
            }
        });
    }

    private void release() {

    }
}
