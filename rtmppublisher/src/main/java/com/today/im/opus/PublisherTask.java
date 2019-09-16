package com.today.im.opus;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.today.im.IMMuxer;

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
    private Object waitObject = new Object();
    private String voipCode;

    HandlerThread handlerCollectThread;
    HandlerThread handlerPublishThread;

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
                int result = imMuxer.publish(ipHost[0], port, urlArray[1], urlArray[2], id.toString(), md5, voipCode);
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
        synchronized (waitObject) {
            waitObject.notify();
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
        synchronized (waitObject) {
            waitObject.notify();
        }
    }

    private void collectData(int bufferSize) {
        final Long createEncoder = OpusUtils.instance().createEncoder(Constants.SAMPLE_RATE, Constants.CHANEL_IN_OPUS, 3);

        byte[] audioBuffer = new byte[640];
        while (isPublishing) {
            synchronized (waitObject) {
                try {
                    if (isMute) {
                        waitObject.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            int curShortSize = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            if (curShortSize > 0 && curShortSize <= audioBuffer.length) {
                try {
                    byte[] byteArray = new byte[bufferSize / 8]; //编码后大小减小8倍
                    int encodeSize = OpusUtils.instance().encode(createEncoder, IMUtils.INSTANCE.byteArrayToShortArray(audioBuffer), 0, byteArray);
                    if (encodeSize > 0) {
                        byte[] decodeArray = new byte[encodeSize];
                        System.arraycopy(byteArray, 0, decodeArray, 0, encodeSize);
//                    byte[] byteArray = {75, 65, 0, 45, 105, 46, -105, 38, 19, 83, 126, -27, 44, 34, 12, -112, -42, -105, -113
//                            , -96, 90, 16, 86, -68, -23, 87, -14, 70, 16, 125, -53, -5, 84, -53, 14, -33, -93, 0, -95, 35};
//                    byte[] byteArray = {75, 65, 0, 45, 105, 46, 105, 38, 19, 83, 126, 27, 44, 34, 12, 112, 42, 105, 113
//                            , 96, 90, 16, 86, 68, 23, 87, 14, 70, 16, 125, 53, 5, 84, 53, 14, 33, 93, 0, 95, 35};
//                    byte c = 50;
//                    for (int i = 0; i < byteArray.length; i++) {
//                        byte sss = byteArray[i];
//                        byte a = (byte) (sss ^ c ^ c);
//                        byteArray[i] = a;
//                    }
//                    String s = "";
//                    for (int i = 0; i < byteArray.length; i++) {
//                        s += byteToChar(byteArray[i]);
//                    }

//                    String input = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnop";
//                    byteArray = input.getBytes();

//                    byte[] ccc = charToByte('s');

                        Log.d(TAG, "消息采样包 size=" + decodeArray.length + "，decodeArray=" + Arrays.toString(decodeArray));

                        dataQueue.push(decodeArray);
//                    Log.d(TAG, "消息采样包：size=" + byteArray.length + ", 队列 size =" + dataQueue.size());
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
                    int result = imMuxer.write(data, type, data.length, timestamp);
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
