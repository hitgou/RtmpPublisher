package com.takusemba.rtmppublisher;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

import com.today.im.opus.OpusUtils;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

/**
 * 实时音频播放处理类<br/>
 * 使用示例代码如下:<br/>
 *
 * <pre>
 * audioPlayerHandler = new AudioPlayerHandler();
 * audioPlayerHandler.prepare();// 播放前需要prepare。可以重复prepare
 * // 直接将需要播放的数据传入即可
 * audioPlayerHandler.onPlaying(data, 0, data.length);
 * </pre>
 *
 * @author
 */
public class AudioPlayerHandler implements Runnable {
    private final static String TAG = "AudioPlayerHandler";
    private AudioTrack track = null;// 录音文件播放对象
    private boolean isPlaying = false;// 标记是否正在录音中
    private int bufferSize = -1;// 播放缓冲大小
    private LinkedBlockingDeque<Object> dataQueue = new LinkedBlockingDeque<>();
    // 互斥信号量
    private Semaphore semaphore = new Semaphore(1);
    // 是否释放资源的标志位
    private boolean release = false;

    public AudioPlayerHandler() {
        try {
            // 获取缓冲 大小
            bufferSize = AudioTrack.getMinBufferSize(AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANEL_OUT,
                    AudioRecorder.AUDIO_FORMAT);

            // 实例AudioTrack
            track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, AudioRecorder.SAMPLE_RATE,
                    AudioRecorder.CHANEL_OUT, AudioRecorder.AUDIO_FORMAT, bufferSize,
                    AudioTrack.MODE_STREAM);
            track.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
            // 默认需要抢占一个信号量。防止播放进程执行
            semaphore.acquire();

            // 开启播放线程
            new Thread(this).start();
        } catch (Exception e) {
            Log.e(TAG, "启动播放出错", e);
            e.printStackTrace();
        }
    }

    /**
     * 播放，当有新数据传入时，
     *
     * @param data       语音byte数组
     * @param startIndex 开始的偏移量
     * @param length     数据长度
     */
    public synchronized void onPlaying(byte[] data, int startIndex, int length) {
        if (AudioTrack.ERROR_BAD_VALUE == bufferSize) {// 初始化错误
            return;
        }
        try {
            byte[] newData = Arrays.copyOfRange(data, 1, data.length - 1);
            dataQueue.putLast(newData);
            semaphore.release();
        } catch (InterruptedException e) {
            Log.e(TAG, "启动播放出错", e);
            e.printStackTrace();
        }
    }

    /**
     * 准备播放
     */
    public void prepare() {
        if (track != null && !isPlaying) {
            track.play();
            isPlaying = true;
        }

    }

    /**
     * 停止播放
     */
    public void stop() {
        if (track != null) {
            track.stop();
            isPlaying = false;
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        release = true;
        semaphore.release();
        if (track != null) {
            track.release();
            track = null;
        }
    }

    @Override
    public void run() {
        final OpusUtils opusUtils = new OpusUtils();
        final Long createDecoder = opusUtils.createDecoder(AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANEL_IN_OPUS);
        byte[] bufferArray = new byte[80];

        while (true) {
            if (release) {
                return;
            }
            if (dataQueue.size() > 0) {
                byte[] data = (byte[]) dataQueue.pollFirst();
                short[] decodeBufferArray = new short[bufferArray.length * 4];
                int size = opusUtils.decode(createDecoder, data, decodeBufferArray);
                if (size > 0) {
                    short[] decodeArray = new short[size];
                    System.arraycopy(decodeBufferArray, 0, decodeArray, 0, size);
                    track.write(decodeArray, 0, decodeArray.length);
                }
            } else {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    Log.e(TAG, "启动播放出错", e);
                    e.printStackTrace();
                }
            }
        }
    }


}