package com.today.im;

import java.security.MessageDigest;

/**
 * Created by Colin Wang 2019-08-12
 */
public class IMMuxer {

    static {
        System.loadLibrary("rtmp-jni");
    }

    public static String md5(String str) {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("String to encript cannot be null or zero length");
        }

        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = str.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++)
            byteArray[i] = (byte) charArray[i];
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    public static String getSalt() {
        return "6291D258227040D0A53203C1C5225275";
    }

//    public native String getSalt();

    public native Object initWithSampleRate(int sampleRate, int audioEncoder);

    public native void stopCalled();

    public native int publish(String host, int port, String app, String path, String id, String md5, String voipCode);

    public native int publish1();

    public native void stopPublish();

    public native int pull(String host, int port, String app, String path, String id, String md5, String voipCode);

//    public native int publish();

    public native int isPublishConnected();

    public native int write(byte[] data, int type, int length, int timestamp);

    public native int pullWithUrl(String rtmpURL);

    public native void replayWithUrl(String rtmpURL);

    public native void stopPull();

    public native int isPullConnected();

    public native PacketInfo read();

}
