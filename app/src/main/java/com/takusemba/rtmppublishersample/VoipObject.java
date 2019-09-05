package com.takusemba.rtmppublishersample;

import com.google.gson.Gson;

public class VoipObject {
    private int status_code;
    private String msg;
    private int did;
    private String call_type;
    private String push_uri;
    private String sub_uri;

    public int getStatusCode() {
        return status_code;
    }

    public void setStatusCode(int status_code) {
        this.status_code = status_code;
    }

    public String getCallType() {
        return call_type;
    }

    public void setCallType(String call_type) {
        this.call_type = call_type;
    }

    public String getPushUri() {
        return push_uri;
    }

    public void setPushUri(String push_uri) {
        this.push_uri = push_uri;
    }

    public String getSubUri() {
        return sub_uri;
    }

    public void setSubUri(String sub_uri) {
        this.sub_uri = sub_uri;
    }

    public int getDid() {
        return did;
    }

    public void setDid(int did) {
        this.did = did;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return (new Gson()).toJson(this);
    }
}
