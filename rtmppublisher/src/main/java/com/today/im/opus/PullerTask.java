package com.today.im.opus;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.today.im.IMMuxer;
import com.today.im.PacketInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

public class PullerTask {
    private final static String TAG = "PullerTask";

    private IMMuxer imMuxer = new IMMuxer();
    private boolean isPlaying = false;
    private PullerListener pullerListener;
    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private LinkedBlockingDeque<byte[]> dataQueue = new LinkedBlockingDeque<>();
    private String url;
    private String voipCode;

    private int pullRequestCount = 0;
    private AudioTrack audioTrack = null;
    private int bufferSize = -1;

    HandlerThread handlerCollectThread;
    HandlerThread handlerPublishThread;

    public PullerTask(AudioManager audioManager, PullerListener pullerListener, String url, String voipCode) {
        this.audioManager = audioManager;
        this.pullerListener = pullerListener;
        this.url = url;
        this.voipCode = voipCode;

        bufferSize = AudioTrack.getMinBufferSize(Constants.SAMPLE_RATE, Constants.CHANNEL_OUT_MONO,
                Constants.AUDIO_FORMAT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, Constants.SAMPLE_RATE,
                Constants.CHANNEL_OUT_MONO, Constants.AUDIO_FORMAT, bufferSize,
                AudioTrack.MODE_STREAM);
    }

    public PullerTask(AudioManager audioManager, PullerListener pullerListener, String voipCode) {
        this(audioManager, pullerListener, "", voipCode);
    }

    public void start() {
        isPlaying = true;
        audioTrack.play();

        handlerCollectThread = new HandlerThread("read");
        handlerCollectThread.start();
        Handler handlerCollect = new Handler(handlerCollectThread.getLooper());
        final String playUrl = this.url;
        handlerCollect.post(new Runnable() {
            @Override
            public void run() {
                String tempUrl = playUrl.substring(playUrl.indexOf("//") + 2);
                String[] urlArray = tempUrl.split("/");
                if (urlArray.length != 3) {
                    return;
                }
                String[] ipHost = urlArray[0].split(":");
                int port = Integer.parseInt(ipHost[1]);

                UUID id = UUID.randomUUID();
                pullRequestCount++;
                String hash = String.format("%s/%s-%s-%d", id.toString(), urlArray[2], imMuxer.getSalt(), pullRequestCount);
                String md5 = IMMuxer.md5(hash);
                int result = imMuxer.pull(ipHost[0], port, urlArray[1], urlArray[2], id.toString(), md5, voipCode);
                Log.d(TAG, "imMuxer.pull result = " + result);

                if (result == 1) {
                    collectData();
                    uiUpdate();
                } else {
                    imMuxer.stopPull();
                }
            }
        });

        handlerPublishThread = new HandlerThread("play");
        handlerPublishThread.start();
        Handler handlerPublish = new Handler(handlerPublishThread.getLooper());
        handlerPublish.post(new Runnable() {
            @Override
            public void run() {
                playerData();
            }
        });

        uiUpdate();
    }

    private void uiUpdate() {
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        if (pullerListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    pullerListener.onPullStarted();
                }
            });
        }
    }

    public void stop() {
        isPlaying = false;
        audioTrack.stop();
        audioTrack.release();
        if (handlerPublishThread != null) {
            handlerPublishThread.interrupt();
        }
        if (handlerCollectThread != null) {
            handlerCollectThread.interrupt();
        }
        dataQueue.clear();
        // RTMP 最后延时 100 s停止，防止rtmp协议还在读取数据，但是 rtmp 已经为空的问题
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imMuxer.stopPull();
            }
        }, 100);

        final Handler uiHandler = new Handler(Looper.getMainLooper());
        if (pullerListener != null) {
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pullerListener.onPullStopped();
                }
            }, 500);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setUrl(String url, String voipCode) {
        this.url = url;
        this.voipCode = voipCode;
    }

    private void collectData() {
        int times = 0;
        while (isPlaying && times < 3) {
            times++;
            try {
                PacketInfo packet = imMuxer.read();
                if (packet == null) {
                    Log.d(TAG, "接收到消息为空");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                if (packet.packetType == Constants.MSG_SEND_AUDIO) {
                    byte[] data = packet.body;
                    byte[] newData = Arrays.copyOfRange(data, 0, data.length);
                    dataQueue.push(newData);
                    Log.d(TAG, "接收到消息包 times=" + times + "：size=" + packet.bodySize + ", 队列 size =" + dataQueue.size() + "，data=" + Arrays.toString(data));
                }
            } catch (Exception e) {
                Log.d(TAG, "接收消息异常：", e);
            }
        }
    }

    private void playerData() {
        final Long createDecoder = OpusUtils.instance().createDecoder(Constants.SAMPLE_RATE, Constants.CHANEL_IN_OPUS);

        File file = new File(Constants.recorderFilePath);
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
                    int size = OpusUtils.instance().decode(createDecoder, data, decodeBufferArray);
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

            OpusUtils.instance().destroyDecoder(createDecoder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
