package com.today.im;

/**
 * Created by Colin Wang 2019-08-12
 */
public class IMMuxer {

    static {
        System.loadLibrary("rtmp-jni");
    }

    public native Object initWithSampleRate(int sampleRate, int audioEncoder);

    public native void stopCalled();

    public native int publishWithUrl(String rtmpURL);

    public native void stopPublish();

    public native int write(byte[] data, int type, int length, int timestamp);

    public native int pullWithUrl(String rtmpURL);

    public native void replayWithUrl(String rtmpURL);

    public native void stopPull();

//    public native void startPlay();

//    public native void stopPull();

    public native PacketInfo read();

    public native int isConnected();

}
