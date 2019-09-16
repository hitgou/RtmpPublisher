#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "rtmp.h"
#include "log.h"
#include "today_rtmp.h"
#include <android/log.h>
#include <time.h>
#include <string.h>

#define AAC_ADTS_HEADER_SIZE 7
#define FLV_TAG_HEAD_LEN 11
#define FLV_PRE_TAG_LEN 4

//#define  LOG_TAG    "rtmp-muxer"

//#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static AVal av_onMetaData = AVC("onMetaData");
static AVal av_duration = AVC("duration");
static AVal av_width = AVC("width");
static AVal av_height = AVC("height");
static AVal av_videocodecid = AVC("videocodecid");
static AVal av_avcprofile = AVC("avcprofile");
static AVal av_avclevel = AVC("avclevel");
static AVal av_videoframerate = AVC("videoframerate");
static AVal av_audiocodecid = AVC("audiocodecid");
static AVal av_audiosamplerate = AVC("audiosamplerate");
static AVal av_audiochannels = AVC("audiochannels");
static AVal av_avc1 = AVC("avc1");
static AVal av_mp4a = AVC("mp4a");
static AVal av_onPrivateData = AVC("onPrivateData");
static AVal av_record = AVC("record");


static FILE *g_file_handle = NULL;
static uint64_t g_time_begin;


//RTMP *rtmp;
bool isMuted;
char *checkCodeData;
float upLoadDataSize;
float downLoadDataSize;

//AudioUnitPlayer *mAudioUnitPlayer;
RTMP *publishRTMP;
RTMP *pullRTMP;
bool isPushing;
bool isPulling;
bool isPlaying;
bool isStartPull;
int pubTs;
int publishCount;
int pullCount;


int publish(char *host, int port, char *app, char *path, char *guid,
            char *md5) {
    publishRTMP = RTMP_Alloc();
    if (publishRTMP == NULL) {
        return -1;
    }
    RTMP_Init(publishRTMP);

    char *sockhost;
    sockhost = "";

    AVal av_host = {host, strlen(host)};
    AVal av_pageUrl = {md5, strlen(md5)};
    AVal av_playpath = {path, strlen(path)};
    AVal av_app = {app, strlen(app)};
    AVal av_tcUrl = {guid, strlen(guid)};
    AVal av_sockhost = {sockhost, 0};
    RTMP_SetupStream(publishRTMP, RTMP_PROTOCOL_RTMP, &av_host, port, &av_sockhost, &av_playpath,
                     &av_tcUrl, &av_pageUrl, 0, &av_app, 0, 0, 0, 0, 0, 0, 0, 0, 1, 5);

    RTMP_EnableWrite(publishRTMP);

    int ret = RTMP_Connect(publishRTMP, NULL);
    if (!ret) {
        RTMP_Free(publishRTMP);
        publishRTMP = NULL;
        return -3;
    }

    RTMPPacket rtmp_pakt = {0};
    ret = RTMP_ReadPacket(publishRTMP, &rtmp_pakt);

    ret = RTMP_ConnectStream(publishRTMP, 0);
    if (!ret) {
        return -4;
    }

    return 1;
}

int pull(char *host, int port, char *app, char *path, char *guid, char *md5) {
    pullRTMP = RTMP_Alloc();
    if (pullRTMP == NULL) {
        return -1;
    }
    RTMP_Init(pullRTMP);

    char *sockhost;
    sockhost = "";

    AVal av_host = {host, strlen(host)};
    AVal av_pageUrl = {md5, strlen(md5)};
    AVal av_playpath = {path, strlen(path)};
    AVal av_app = {app, strlen(app)};
    AVal av_tcUrl = {guid, strlen(guid)};
    AVal av_sockhost = {sockhost, 0};
    RTMP_SetupStream(pullRTMP, RTMP_PROTOCOL_RTMP, &av_host, port, &av_sockhost, &av_playpath,
                     &av_tcUrl, &av_pageUrl, 0, &av_app, 0, 0, 0, 0, 0, 0, 0, 0, 1, 5);

    if (!RTMP_Connect(pullRTMP, NULL) || !RTMP_ConnectStream(pullRTMP, 0)) {
        RTMP_Free(pullRTMP);
        pullRTMP = NULL;
        return -1;
    }

    RTMP_SetBufferMS(pullRTMP, 100);
    RTMP_UpdateBufferMS(pullRTMP);

    isPulling = true;

    return 1;
}


int publish1() {
    RTMP_EnableWrite(publishRTMP);

    int ret = RTMP_Connect(publishRTMP, NULL);
    if (!ret) {
        RTMP_Free(publishRTMP);
        publishRTMP = NULL;
        return -3;
    }

    RTMPPacket rtmp_pakt = {0};
    ret = RTMP_ReadPacket(publishRTMP, &rtmp_pakt);

    ret = RTMP_ConnectStream(publishRTMP, 0);
    if (!ret) {
        return -4;
    }

    return 1;
}

void stopPublish() {
    isPushing = false;
    if (publishRTMP) {
        if (RTMP_IsConnected(publishRTMP)) {
            RTMP_Close(publishRTMP);
        }
        RTMP_Free(publishRTMP);
        publishRTMP = NULL;
    }
    checkCodeData = NULL;
}

int publishRtmpIsConnected() {
    if (publishRTMP) {
        if (RTMP_IsConnected(publishRTMP)) {
            return 1;
        }
    }
    return 0;
}

int write(char *buf, int type, int buflen, uint64_t timestamp) {
    int ret;
    RTMPPacket rtmp_pakt;
    RTMPPacket_Reset(&rtmp_pakt);
    RTMPPacket_Alloc(&rtmp_pakt, buflen);
    rtmp_pakt.m_packetType = type;
    rtmp_pakt.m_nBodySize = buflen;
    rtmp_pakt.m_nTimeStamp = timestamp;
    rtmp_pakt.m_nChannel = 4;
    rtmp_pakt.m_headerType = RTMP_PACKET_SIZE_LARGE;
    rtmp_pakt.m_nInfoField2 = publishRTMP->m_stream_id;
    memcpy(rtmp_pakt.m_body, buf, buflen);
    ret = RTMP_SendPacket(publishRTMP, &rtmp_pakt, 0);
    RTMPPacket_Free(&rtmp_pakt);

    return (ret > 0) ? 0 : -1;
}

int pullWithUrl(char *url) {
    pullCount = 0;

    pullRTMP = RTMP_Alloc();
    if (pullRTMP == NULL) {
        return -1;
    }

    RTMP_Init(pullRTMP);

    int ret = RTMP_SetupURL(pullRTMP, url);

    if (!RTMP_Connect(pullRTMP, NULL) || !RTMP_ConnectStream(pullRTMP, 0)) {
        return -1;
    }

    RTMP_SetBufferMS(pullRTMP, 100);
    RTMP_UpdateBufferMS(pullRTMP);

    isPulling = true;

    return 1;
}

int replayWithUrl(char *url) {
    return 1;
}

void stopPull() {
    isPulling = false;
    if (pullRTMP) {
        RTMP_Close(pullRTMP);
        RTMP_Free(pullRTMP);
        pullRTMP = NULL;
    }
    checkCodeData = NULL;
}

int pullRtmpIsConnected() {
    if (pullRTMP) {
        if (RTMP_IsConnected(pullRTMP)) {
            return 1;
        }
    }
    return 0;
}

RTMPPacket read() {
    RTMPPacket rtmp_pakt = {0};
    if (isPulling && pullRTMP) {
        RTMP_ReadPacket(pullRTMP, &rtmp_pakt);
        if (RTMPPacket_IsReady(&rtmp_pakt)) {
            if (!rtmp_pakt.m_nBodySize)
                return rtmp_pakt;

            if (rtmp_pakt.m_packetType == RTMP_PACKET_TYPE_AUDIO) {
                int headSize = 1;
                if (rtmp_pakt.m_body != NULL) {
//                    uint32_t size = rtmp_pakt.m_nBodySize - headSize;
//                    char *body = malloc(size);
//                    memcpy(body, rtmp_pakt.m_body + headSize, size);
                    return rtmp_pakt;
                }
            } else if (rtmp_pakt.m_packetType == RTMP_PACKET_TYPE_VIDEO) {
                // 处理视频数据包
            } else if (rtmp_pakt.m_packetType == RTMP_PACKET_TYPE_INVOKE) {
                // 处理invoke包
                RTMP_ClientPacket(pullRTMP, &rtmp_pakt);
            } else if (rtmp_pakt.m_packetType == RTMP_PACKET_TYPE_INFO) {
                // 处理信息包
                //JRZXVoipLog(@"RTMP_PACKET_TYPE_INFO");
            } else if (rtmp_pakt.m_packetType == RTMP_PACKET_TYPE_FLASH_VIDEO) {
                // 其他数据
            }
            RTMPPacket_Free(&rtmp_pakt);
        }
    }

    return rtmp_pakt;
}


void playAudioWaitingToLong() {

}

void sendCollectedPCMData(char *data) {

}

void playAudioWithBuffer(char *data) {

}

int rtmpSetupStream(RTMP *rtmp, char *url, int requestCount) {
    int ret = 0;
    char *uuid = newGUID();

    char *buff;
    buff = url;

    char *protocal = strsep(&url, "://");
    char *urlAddress = strsep(NULL, "://");
    if (urlAddress == NULL) {
        ret = 0;
    }

    free(urlAddress);
    free(protocal);

    ret = 1;

    return ret;
}

char *newGUID() {
    srand(time(NULL));
    static char buf[64] = {0};
    snprintf(buf, sizeof(buf),
             "{%08X-%04X-%04X-%04X-%04X%04X%04X}",
             rand() & 0xffffffff,
             rand() & 0xffff,
             rand() & 0xffff,
             rand() & 0xffff,
             rand() & 0xffff, rand() & 0xffff, rand() & 0xffff
    );
    return (const char *) buf;
}

char *dataWithHexString(char *voipCode) {
    int len = sizeof(voipCode);
    if (len == 0) return NULL;

    char *result = malloc(len / 2);
    unsigned char bytes;
    char str[3] = {'\0', '\0', '\0'};
    int i;
    for (i = 0; i < len / 2; i++) {
        str[0] = voipCode[i * 2];
        str[1] = voipCode[i * 2 + 1];
        bytes = strtol(str, NULL, 16);
        memcpy(result + i, &bytes, sizeof(char));
    }
    return result; // 7'u@
}

char *getCheckCodeData(char *voipCode) {
    if (!checkCodeData) {
        checkCodeData = dataWithHexString(voipCode);
    }
    return checkCodeData;
}

#define key_Length 4

void checkCodeAudioData(char *audioData, int audioLength, char *voipCode) {
    char *keyData = getCheckCodeData(voipCode);
    if (!audioLength && keyData && !audioLength) {
        return;
    }
    long i = 0;
    for (i = 0; i < audioLength; i++) {
        int l = i % key_Length;
        audioData[i] = audioData[i] ^ keyData[l];
//        LOGD("l=%d, c=%c, s=%c, result=%c, resultSecond=%c", l, c, s, result, resultSecond);
    }
}