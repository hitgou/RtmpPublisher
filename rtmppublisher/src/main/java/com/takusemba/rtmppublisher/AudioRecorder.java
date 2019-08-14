package com.takusemba.rtmppublisher;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

class AudioRecorder {
    private final static String TAG = "AudioRecorder";
    public static final int SAMPLE_RATE = 16000;
    public static final int CHANEL_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANEL_OUT = AudioFormat.CHANNEL_OUT_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT;

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
        try {
            final int bufferSize = AudioRecord.getMinBufferSize(AudioRecorder.SAMPLE_RATE, AudioRecorder.CHANEL_IN, AudioRecorder.AUDIO_FORMAT);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioRecorder.CHANEL_IN, AudioRecorder.AUDIO_FORMAT, bufferSize);

            audioRecord.startRecording();

            HandlerThread handlerThread = new HandlerThread("AudioRecorder-record");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    int bufferReadResult;
                    byte[] data = new byte[bufferSize];
                    // keep running... so use a different thread.
                    while (isRecording() && (bufferReadResult = audioRecord.read(data, 0, bufferSize)) > 0) {
                        Log.d(TAG, "消息采样包：size=" + data.length);
                        listener.onAudioRecorded(data, bufferReadResult);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "启动播放出错", e);
            e.printStackTrace();
        }
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
