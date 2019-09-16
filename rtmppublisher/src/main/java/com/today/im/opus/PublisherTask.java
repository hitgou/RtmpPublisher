package com.today.im.opus;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.today.im.IMMuxer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

public class PublisherTask {
    private final static String TAG = "PublisherTask V4";

    private IMMuxer imMuxer = new IMMuxer();
    private boolean isPublishing = false;
    private PublisherListener publisherListener;
    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private LinkedBlockingDeque<byte[]> dataQueue = new LinkedBlockingDeque<>();
    private String url;
    private boolean isMute;
    private int timestamp;
    private int publishRequestCount = 0;
    private int pullRequestCount = 0;
    private String voipCode;

    HandlerThread handlerCollectThread;
    HandlerThread handlerPublishThread;

    static final int SAMPLE_RATE = 16000;
    static final int FRAME_SIZE = 160;
    static final int NUM_CHANNELS = 1;
    static final int BUFFER_LENGTH = 80;

    public PublisherTask(AudioManager audioManager, PublisherListener publisherListener, String url) {
        this.audioManager = audioManager;
        this.publisherListener = publisherListener;
        this.url = url;
    }

    public PublisherTask(AudioManager audioManager, PublisherListener publisherListener) {
        this(audioManager, publisherListener, "");
    }


    public void start() {
        isPublishing = true;
        timestamp = 0;

        handlerCollectThread = new HandlerThread("collect");
        handlerCollectThread.start();
        Handler handlerCollect = new Handler(handlerCollectThread.getLooper());
        final String publishUrl = this.url;
        // publishUrl = "rtmp://47.106.33.6:9935/voip_relay/47.75.13.156-to-47.106.33.6-81";
        handlerCollect.post(new Runnable() {
            @Override
            public void run() {
                String tempUrl = publishUrl.substring(publishUrl.indexOf("//") + 2);
                String[] urlArray = tempUrl.split("/");
                if (urlArray.length != 3) {
                    return;
                }
                String[] ipHost = urlArray[0].split(":");
                int port = Integer.parseInt(ipHost[1]);

                UUID id = UUID.randomUUID();
                publishRequestCount++;
                String hash = String.format("%s/%s-%s-%d", id.toString(), urlArray[2], imMuxer.getSalt(), publishRequestCount);
                String md5 = IMMuxer.md5(hash);
                int result = imMuxer.publish(ipHost[0], port, urlArray[1], urlArray[2], id.toString(), md5);
                Log.d(TAG, "imMuxer.publish result = " + result);
                if (result == 1) {
                    final int bufferSize = AudioRecord.getMinBufferSize(Constants.SAMPLE_RATE, Constants.CHANEL_IN, Constants.AUDIO_FORMAT);
                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, Constants.SAMPLE_RATE, Constants.CHANEL_IN, Constants.AUDIO_FORMAT, bufferSize);
                    audioRecord.startRecording();

                    collectData(bufferSize);
                } else {
                    imMuxer.stopPublish();
                }
            }
        });

        handlerPublishThread = new HandlerThread("publish");
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
                    publisherListener.onPublishStarted();
                }
            });
        }
    }

    public void stop() {
        isPublishing = false;
        isMute = false;
        timestamp = 0;
        dataQueue.clear();
        if (handlerPublishThread != null) {
            handlerPublishThread.interrupt();
        }
        if (handlerCollectThread != null) {
            handlerCollectThread.interrupt();
        }
        if (isRecording() && audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        // RTMP 最后延时 100 s停止，防止rtmp协议还在读取数据，但是 rtmp 已经为空的问题
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imMuxer.stopPublish();
            }
        }, 100);

        final Handler uiHandler = new Handler(Looper.getMainLooper());
        if (publisherListener != null) {
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    publisherListener.onPublishStopped();
                }
            }, 500);
        }
    }

    public boolean isPublishing() {
        return isPublishing;
    }

    public void setUrl(String url, String voipCode) {
        this.url = url;
        this.voipCode = voipCode;
    }

    public void setMute(boolean isMute) {
        this.isMute = isMute;
    }

    private void collectData(int bufferSize) {
        final Long createEncoder = OpusUtils.instance().initEncoder(Constants.SAMPLE_RATE, Constants.CHANEL_IN_OPUS, 2048);
        byte[] inBuf = new byte[BUFFER_LENGTH * 8];
        byte[] audioBuffer = new byte[BUFFER_LENGTH * 8];

        while (isPublishing) {
            int read = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            if (read > 0 && read <= inBuf.length) {
                try {
                    if (isMute) { // 构造全部为 0 的自己数组
                        inBuf = new byte[]{};
                    }

                    short[] shortData = IMUtils.INSTANCE.byteArrayToShortArray(audioBuffer);
                    byte[] byteArray = new byte[BUFFER_LENGTH]; //编码后大小减小8倍
                    int encodeSize = OpusUtils.instance().encode(createEncoder, shortData, 0, byteArray);
                    if (encodeSize > 0) {
                        byte[] encodeArray = new byte[encodeSize];
                        System.arraycopy(byteArray, 0, encodeArray, 0, encodeSize);
                        Log.d(TAG, "消息采样包 size=" + encodeArray.length);
                        dataQueue.push(encodeArray);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "启动发送出错", e);
                    e.printStackTrace();
                }
            }
        }

        OpusUtils.instance().destroyEncoder(createEncoder);
    }


    private void publishData() {
        while (isPublishing) {
            byte[] data = new byte[0];
            try {
                data = dataQueue.take();
                if (data != null) {
                    timestamp += 20;
                    Log.d(TAG, "消息采样包：size=" + data.length + ", 队列 size =" + dataQueue.size());
                    int type = Constants.MSG_SEND_AUDIO;
                    int result = imMuxer.write(data, type, data.length, timestamp, voipCode);
                    Log.d(TAG, "result is " + result);
                    if (result == -1 && imMuxer.isPublishConnected() != 1) {
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
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    boolean isRecording() {
        return audioRecord != null
                && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

}
