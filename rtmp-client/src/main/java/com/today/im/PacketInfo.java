package com.today.im;

public class PacketInfo {
    public int headerType;
    public int packetType;
    public int hasAbsTimestamp;
    public int timeStamp;

    public int infoField2;
    public int bodySize;
    public byte[] body;

    public PacketInfo(int headerType, int packetType, int hasAbsTimestamp, int timeStamp, int infoField2, int bodySize, byte[] body) {
        this.headerType = headerType;
        this.packetType = packetType;
        this.hasAbsTimestamp = hasAbsTimestamp;
        this.timeStamp = timeStamp;
        this.infoField2 = infoField2;
        this.bodySize = bodySize;
        this.body = body;
    }

}
