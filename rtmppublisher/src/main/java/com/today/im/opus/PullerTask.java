package com.today.im.opus;

import android.media.AudioAttributes;
import android.media.AudioFormat;
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

import static com.today.im.opus.PublisherTask.NUM_CHANNELS;
import static com.today.im.opus.PublisherTask.SAMPLE_RATE;

public class PullerTask {
    private final static String TAG = "PullerTask";

    private IMMuxer imMuxer = new IMMuxer();
    private boolean isPlaying = false;
    private PullerListener pullerListener;
    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private LinkedBlockingDeque<short[]> dataQueue = new LinkedBlockingDeque<>();
    private String url;
    private String voipCode;

    private int pullRequestCount = 0;
//    private AudioTrack audioTrack = null;

    HandlerThread handlerCollectThread;
    HandlerThread handlerPublishThread;

    private int bufferSize = AudioTrack.getMinBufferSize(Constants.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private AudioAttributes audioAttributes = new AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build();
    private AudioFormat audioFormat = new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(Constants.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
    private int sessionId;


    public PullerTask(AudioManager audioManager, PullerListener pullerListener, String url, String voipCode) {
        this.audioManager = audioManager;
        this.pullerListener = pullerListener;
        this.url = url;
        this.voipCode = voipCode;

//        bufferSize = AudioTrack.getMinBufferSize(Constants.SAMPLE_RATE, Constants.CHANNEL_OUT_MONO,
//                Constants.AUDIO_FORMAT);
        sessionId = audioManager.generateAudioSessionId();
//        audioTrack = new AudioTrack(audioAttributes, audioFormat, bufferSize, AudioTrack.MODE_STREAM, sessionId);

//        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, Constants.SAMPLE_RATE,
//                Constants.CHANNEL_OUT_MONO, Constants.AUDIO_FORMAT, bufferSize,
//                AudioTrack.MODE_STREAM);
    }

    public PullerTask(AudioManager audioManager, PullerListener pullerListener, String voipCode) {
        this(audioManager, pullerListener, "", voipCode);
    }

    public void start() {
        isPlaying = true;
//        audioTrack.play();

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
                int result = imMuxer.pull(ipHost[0], port, urlArray[1], urlArray[2], id.toString(), md5);
                Log.d(TAG, "imMuxer.pull result = " + result);

                if (result == 1) {
                    collectData();
                    uiUpdate();
                } else {
                    imMuxer.stopPull();
                }
            }
        });

//        handlerPublishThread = new HandlerThread("play");
//        handlerPublishThread.start();
//        Handler handlerPublish = new Handler(handlerPublishThread.getLooper());
//        handlerPublish.post(new Runnable() {
//            @Override
//            public void run() {
//                playerData();
//            }
//        });

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
//        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
//            audioTrack.stop();
//        }
//        audioTrack.release();
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
        File fileOpus = new File(Constants.pullRecorderOpusFilePath);
        File filePCM = new File(Constants.pullRecorderPcmFilePath);
        File fileDir = fileOpus.getParentFile();
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        if (fileOpus.exists()) {
            fileOpus.delete();
        }
        if (filePCM.exists()) {
            filePCM.delete();
        }

        try {
            fileOpus.createNewFile();
            filePCM.createNewFile();
            FileOutputStream fileOpusOutputStream = new FileOutputStream(fileOpus, true);
            FileOutputStream filePCMOutputStream = new FileOutputStream(filePCM, true);
            BufferedOutputStream fileOpusBufferedOutputStream = new BufferedOutputStream(fileOpusOutputStream);
            BufferedOutputStream filePCMBufferedOutputStream = new BufferedOutputStream(filePCMOutputStream);

            int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);
            // init audio track
            AudioTrack track = new AudioTrack(AudioManager.STREAM_SYSTEM,
                    SAMPLE_RATE,
                    NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize,
                    AudioTrack.MODE_STREAM);
            track.play();

            int times = 0;
//            final Long createDecoder = OpusUtils.instance().createDecoder(Constants.SAMPLE_RATE, Constants.CHANEL_IN_OPUS, 8);
            final Long createDecoder = OpusUtils.instance().initDecoder(Constants.SAMPLE_RATE, Constants.CHANEL_IN_OPUS);
            while (isPlaying) {
                try {
                    PacketInfo packet = imMuxer.read(voipCode);
                    if (packet == null) {
                        Log.d(TAG, "接收到消息为空");
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    times++;
                    if (packet.packetType == Constants.MSG_SEND_AUDIO) {
                        byte[] data = packet.body;
//                        fileOpusBufferedOutputStream.write(data);//写入OPUS

                        short[] decodeBufferArray = new short[640];
                        int size = OpusUtils.instance().decode(createDecoder, data, decodeBufferArray);
//                        int size = OpusUtils.instance().de.decode(createDecoder, data, decodeBufferArray);
                        if (size > 0) {
                            short[] decodeArray = new short[size];
                            System.arraycopy(decodeBufferArray, 0, decodeArray, 0, size);
//                            dataQueue.push(decodeArray);

                            track.write(decodeArray, 0, decodeArray.length * NUM_CHANNELS);

//                            fileOpusBufferedOutputStream.write(IMUtils.INSTANCE.shortArrayToByteArray(decodeBufferArray));//写入OPUS
//                            filePCMBufferedOutputStream.write(IMUtils.INSTANCE.shortArrayToByteArray(decodeArray));//写入OPUS
                            Log.d(TAG, "接收到消息包 decodeArray length=" + decodeArray.length + "：size=" + packet.bodySize + ", 队列 size =" + dataQueue.size());
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "接收消息异常：", e);
                }
            }

            track.stop();
            track.release();

            OpusUtils.instance().destroyDecoder(createDecoder);

            fileOpusBufferedOutputStream.close();
            fileOpusOutputStream.close();
            filePCMBufferedOutputStream.close();
            filePCMOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playerData() {
        int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        // init audio track
        AudioTrack track = new AudioTrack(AudioManager.STREAM_SYSTEM,
                SAMPLE_RATE,
                NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize,
                AudioTrack.MODE_STREAM);
        track.play();

        while (isPlaying) {
            try {
                short[] data = dataQueue.take();
                track.write(data, 0, data.length * NUM_CHANNELS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        track.stop();
        track.release();
    }


}
