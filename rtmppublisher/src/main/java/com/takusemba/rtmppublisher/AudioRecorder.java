package com.takusemba.rtmppublisher;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.today.im.PacketInfo;
import com.today.im.opus.OpusUtils;
import com.today.im.opus.Uilts;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

class AudioRecorder {
    private final static String TAG = "AudioRecorder";
    public static final int SAMPLE_RATE = 16000;
    public static final int CHANEL_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANEL_IN_OPUS = 1;
    public static final int CHANEL_OUT = AudioFormat.CHANNEL_OUT_MONO;

    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static final String APP_PATH = Environment.getExternalStorageDirectory().toString() + File.separator + "demo" + File.separator;
    //    public static final String APP_RECORDER_FILE_PATH = APP_PATH + "recorder_file" + File.separator;
    public static final String recorderFilePath = APP_PATH + "recorder.ops";
    public static final String recorderPcmFilePath = APP_PATH + "recorder.pcm";

    private AudioRecord audioRecord;
    private final int sampleRate;
    private OnAudioRecorderStateChangedListener listener;

    interface OnAudioRecorderStateChangedListener {
        void onAudioRecorded(byte[] data, int length);
    }

    void setOnAudioRecorderStateChangedListener(OnAudioRecorderStateChangedListener listener) {
        this.listener = listener;
    }

    AudioRecorder(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void start() {
        final int bufferSize = AudioRecord.getMinBufferSize(AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANEL_IN, AudioRecorder.AUDIO_FORMAT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioRecorder.CHANEL_IN, AudioRecorder.AUDIO_FORMAT, bufferSize);
        audioRecord.startRecording();

//        final AudioPlayerHandler audioPlayerHandler = new AudioPlayerHandler();
//        audioPlayerHandler.prepare();
//        HandlerThread handlerThread1 = new HandlerThread("AudioEncoder-drain");
//        handlerThread1.start();
//        Handler handler1 = new Handler(handlerThread1.getLooper());
//        handler1.post(audioPlayerHandler);

        final OpusUtils opusUtils = new OpusUtils();
        final Long createEncoder = opusUtils.createEncoder(AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANEL_IN_OPUS, 3);

        HandlerThread handlerThread = new HandlerThread("AudioRecorder-record");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
//                File file = new File(AudioRecorder.recorderFilePath);
//                File fileDir = file.getParentFile();
//                if (!fileDir.exists()) {
//                    fileDir.mkdirs();
//                }
//                if (file.exists()) {
//                    file.delete();
//                }
//                try {
//                    file.createNewFile();
//                    FileOutputStream fileOutputStream = new FileOutputStream(file, true);
//                    BufferedOutputStream fileOpusBufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                int bufferReadResult;
//                    byte[] data = new byte[bufferSize];
                byte[] audioBuffer = new byte[640];
                while (isRecording()) {
                    int curShortSize = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    if (curShortSize > 0 && curShortSize <= audioBuffer.length) {
                        try {
                            listener.onAudioRecorded(audioBuffer, audioBuffer.length);

//                            byte[] byteArray = new byte[bufferSize / 8];//编码后大小减小8倍
//                            int encodeSize = opusUtils.encode(createEncoder, Uilts.INSTANCE.byteArrayToShortArray(audioBuffer), 0, byteArray);
//                            if (encodeSize > 0) {
//                                byte[] decodeArray = new byte[encodeSize];
//                                System.arraycopy(byteArray, 0, decodeArray, 0, encodeSize);
//
////                                    fileOpusBufferedOutputStream.write(decodeArray);//写入OPUS
//
//                                Log.d(TAG, "消息采样包：size=" + decodeArray.length);
//                                listener.onAudioRecorded(decodeArray, encodeSize);
//
////                                audioPlayerHandler.onPlaying(decodeArray, 0, decodeArray.length);
//                            } else {
//
//                            }
                        } catch (Exception e) {
                            Log.e(TAG, "启动播放出错", e);
                            e.printStackTrace();
                        }
                    }
                }
//                    fileOpusBufferedOutputStream.close();
//                    fileOutputStream.close();
                opusUtils.destroyEncoder(createEncoder);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        });
    }

    void stop() {
        if (isRecording()) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    boolean isRecording() {
        return audioRecord != null
                && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }
}
