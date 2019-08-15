package com.takusemba.rtmppublisher;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.today.im.IMMuxer;
import com.today.im.PacketInfo;
import com.today.im.opus.OpusUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;

public class PullerTask {
    private final static String TAG = "PullerTask";

    private AudioHandler audioHandler;
    private IMMuxer imMuxer = new IMMuxer();
    private boolean isPlaying = false;
    private PublisherListener publisherListener;
    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private LinkedBlockingDeque<byte[]> dataQueue = new LinkedBlockingDeque<>();
    private String url;

    private AudioTrack audioTrack = null;
    private int bufferSize = -1;

    public PullerTask(AudioManager audioManager, PublisherListener publisherListener, String url) {
        this.audioHandler = new AudioHandler();
        this.audioManager = audioManager;
        this.publisherListener = publisherListener;
        this.url = url;

        bufferSize = AudioTrack.getMinBufferSize(AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANNEL_OUT_MONO,
                AudioRecorder.AUDIO_FORMAT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, AudioRecorder.SAMPLE_RATE,
                AudioRecorder.CHANNEL_OUT_MONO, AudioRecorder.AUDIO_FORMAT, bufferSize,
                AudioTrack.MODE_STREAM);
    }

    public void start() {
        isPlaying = true;
        audioTrack.play();

        HandlerThread handlerCollectThread = new HandlerThread("read");
        handlerCollectThread.start();
        Handler handlerCollect = new Handler(handlerCollectThread.getLooper());
        final String playUrl = this.url;
        handlerCollect.post(new Runnable() {
            @Override
            public void run() {
                int result = imMuxer.pullWithUrl(playUrl);
                if (result == 1) {
                    collectData();
                }
            }
        });

        HandlerThread handlerPublishThread = new HandlerThread("play");
        handlerPublishThread.start();
        Handler handlerPublish = new Handler(handlerPublishThread.getLooper());
        handlerPublish.post(new Runnable() {
            @Override
            public void run() {
                playerData();
            }
        });

        final Handler uiHandler = new Handler(Looper.getMainLooper());
        if (publisherListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    publisherListener.onStarted();
                }
            });
        }
    }

    public void stop() {
        isPlaying = false;
        imMuxer.stopPull();

        audioTrack.stop();
        audioTrack.release();

        final Handler uiHandler = new Handler(Looper.getMainLooper());
        if (publisherListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    publisherListener.onStopped();
                }
            });
        }

        dataQueue.clear();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    private void collectData() {
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
            if (packet.packetType == Muxer.MSG_SEND_AUDIO) {
                byte[] data = packet.body;
                byte[] newData = Arrays.copyOfRange(data, 1, data.length);
                dataQueue.push(newData);
                Log.d(TAG, "接收到消息包：size=" + packet.bodySize + ", 队列 size =" + dataQueue.size());
            }
        }
    }

    private void playerData() {
        final OpusUtils opusUtils = new OpusUtils();
        final Long createDecoder = opusUtils.createDecoder(AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANEL_IN_OPUS);

        File file = new File(AudioRecorder.recorderFilePath);
        File fileDir = file.getParentFile();
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        if (file.exists()) {
            file.delete();
        }

        try {
            file.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            BufferedOutputStream fileOpusBufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            while (isPlaying) {
                byte[] data = dataQueue.poll();
                if (data != null) {
                    fileOpusBufferedOutputStream.write(data);//写入OPUS
                    short[] decodeBufferArray = new short[640];
                    int size = opusUtils.decode(createDecoder, data, decodeBufferArray);
                    if (size > 0) {
                        short[] decodeArray = new short[size];
                        System.arraycopy(decodeBufferArray, 0, decodeArray, 0, size);
                        audioTrack.write(decodeArray, 0, decodeArray.length);
                    }
//                    audioTrack.write(data, 0, data.length);
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            fileOpusBufferedOutputStream.close();
            fileOutputStream.close();

            opusUtils.destroyDecoder(createDecoder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
