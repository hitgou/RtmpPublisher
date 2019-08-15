package com.takusemba.rtmppublisher;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.today.im.IMMuxer;
import com.today.im.opus.OpusUtils;

import java.util.concurrent.LinkedBlockingDeque;

public class PublisherTask {
    private final static String TAG = "PublisherTask";

    private AudioHandler audioHandler;
    private IMMuxer imMuxer = new IMMuxer();
    private boolean isPublishing = false;
    private PublisherListener publisherListener;
    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private LinkedBlockingDeque<byte[]> dataQueue = new LinkedBlockingDeque<>();
    private String url;
    private int timestamp;

    public PublisherTask(AudioManager audioManager, PublisherListener publisherListener, String url) {
        this.audioHandler = new AudioHandler();
        this.audioManager = audioManager;
        this.publisherListener = publisherListener;
        this.url = url;
    }

    public void start() {
        isPublishing = true;
        timestamp = 0;

        HandlerThread handlerCollectThread = new HandlerThread("collect");
        handlerCollectThread.start();
        Handler handlerCollect = new Handler(handlerCollectThread.getLooper());
        final String publishUrl = this.url;
        handlerCollect.post(new Runnable() {
            @Override
            public void run() {
                int result = imMuxer.publishWithUrl(publishUrl);
                if (result == 1) {
                    final int bufferSize = AudioRecord.getMinBufferSize(AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANEL_IN, AudioRecorder.AUDIO_FORMAT);
                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANEL_IN, AudioRecorder.AUDIO_FORMAT, bufferSize);
                    audioRecord.startRecording();

                    collectData(bufferSize);
                }
            }
        });

        HandlerThread handlerPublishThread = new HandlerThread("publish");
        handlerPublishThread.start();
        Handler handlerPublish = new Handler(handlerPublishThread.getLooper());
        handlerPublish.post(new Runnable() {
            @Override
            public void run() {
                publishData();
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
        isPublishing = false;
        timestamp = 0;
        imMuxer.stopPublish();

        if (isRecording()) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

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

    public boolean isPublishing() {
        return isPublishing;
    }

    private void collectData(int bufferSize) {
        final OpusUtils opusUtils = new OpusUtils();
        final Long createEncoder = opusUtils.createEncoder(AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANEL_IN_OPUS, 3);

        byte[] audioBuffer = new byte[640];
        while (isPublishing) {
            int curShortSize = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            if (curShortSize > 0 && curShortSize <= audioBuffer.length) {
                try {
                    byte[] byteArray = new byte[bufferSize / 8]; //编码后大小减小8倍
//                    int encodeSize = opusUtils.encode(createEncoder, Uilts.INSTANCE.byteArrayToShortArray(audioBuffer), 0, byteArray);
//                    if (encodeSize > 0) {
//                        byte[] decodeArray = new byte[encodeSize];
//                        System.arraycopy(byteArray, 0, decodeArray, 0, encodeSize);
//                        dataQueue.push(decodeArray);
//                    Log.d(TAG, "消息采样包：size=" + decodeArray.length + ", 队列 size =" + dataQueue.size());
                    dataQueue.push(audioBuffer);
//                    } else {
//                    }
                } catch (Exception e) {
                    Log.e(TAG, "启动发送出错", e);
                    e.printStackTrace();
                }
            }
        }

        opusUtils.destroyEncoder(createEncoder);
    }

    private void publishData() {
        while (isPublishing) {
            byte[] data = dataQueue.poll();
            if (data != null) {
                timestamp += 20;
                Log.d(TAG, "消息采样包：size=" + data.length + ", 队列 size =" + dataQueue.size());
                int type = Muxer.MSG_SEND_AUDIO;
                int result = imMuxer.write(data, type, data.length, timestamp);
                Log.d(TAG, "result is " + result);
                if (result == -1 && imMuxer.isConnected() != 1) {
                    Log.d(TAG, "result is " + result);
                    this.stop();
                }
            } else { 
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    boolean isRecording() {
        return audioRecord != null
                && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

}
