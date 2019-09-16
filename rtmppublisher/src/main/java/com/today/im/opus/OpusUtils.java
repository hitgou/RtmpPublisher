package com.today.im.opus;

import org.jetbrains.annotations.NotNull;

public class OpusUtils {

    private static OpusUtils instance;

    private OpusUtils() {
        System.loadLibrary("opus-jni");
    }

    public static OpusUtils instance() {
        if (instance == null) {
            synchronized (OpusUtils.class) {
                if (instance == null) {
                    instance = new OpusUtils();
                }
            }
        }

        return instance;
    }

    public native long initEncoder(int samplingRate, int numberOfChannels, int application);

    public native long initDecoder(int samplingRate, int numberOfChannels);

    public final native long createEncoder(int sampleRateInHz, int channelConfig, int complexity);

    public final native long createDecoder(int sampleRateInHz, int channelConfig, int complexity);

    public final native int encode(long handle, @NotNull short[] lin, int offset, @NotNull byte[] encoded);

    public final native int encodeByte(long handle, @NotNull byte[] lin, int offset, @NotNull byte[] encoded);

    public final native int decode(long handle, @NotNull byte[] encoded, @NotNull short[] lin);

    public final native int decodeShorts(long handle, byte[] in, short[] out, int frames);

    public final native void destroyEncoder(long handle);

    public final native void destroyDecoder(long handle);
}
