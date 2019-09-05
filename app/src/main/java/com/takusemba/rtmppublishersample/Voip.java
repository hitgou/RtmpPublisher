package com.takusemba.rtmppublishersample;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * 网络语音ip业务类
 * Created by Colin on 2019/8/16.
 */
public class Voip {
    public static String TAG = "Voip";
    public static String VOIP_ADDRESS = "";
    public static String SALT = "6291D258227040D0A53203C1C5225275";

    public static int Trying = 100;
    public static int Ringing = 180;
    public static int OK = 200;
    public static int UseProxy = 305;
    public static int AlternativeService = 380;
    public static int BadRequest = 400;
    public static int Unauthorized = 401;
    public static int PaymentRequired = 402;
    public static int forbidden = 403;
    public static int NotFound = 404;
    public static int MethodNotAllowed = 405;
    public static int RequestTimeout = 408;
    public static int BusyHere_NotAcceptableHere = 486;
    public static int ServerInternalError = 500;
    public static int NotImplemented = 501;
    public static int BadGateway = 502;
    public static int ServiceUnavailable = 503;
    public static int Decline = 603;


    static {
//        VOIP_ADDRESS = "https://" + getIp("imtest.testby.xyz") + ":9980/";
        VOIP_ADDRESS = "https://imtest.testby.xyz";
    }

    /**
     * 根据域名获取ip地址
     *
     * @param url
     * @return ip 地址
     * @throws UnknownHostException
     */
    private static String getIp(String url) {
        try {
            return new NetTask().execute(url).get();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return "";
    }

    public static String getHashToken(String caller, String callerId, String callee, String callerUserId, long timestamp) {
        String tokenString = String.format(Locale.getDefault(), "%s-%s-%s-%s-%s-%s", caller, callerId, callee, callerUserId, timestamp, SALT);
        return md5(tokenString);
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

    public static class NetTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(params[0]);
                return addr.getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            return "";
        }
    }
}
