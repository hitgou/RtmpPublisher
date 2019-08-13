#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "rtmp.h"
#include "log.h"
#include "today_rtmp.h"
#include <android/log.h>

#define AAC_ADTS_HEADER_SIZE 7
#define FLV_TAG_HEAD_LEN 11
#define FLV_PRE_TAG_LEN 4

#define  LOG_TAG    "rtmp-muxer"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

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


RTMP *rtmp;
bool isMuted;
char *voipCode;
float upLoadDataSize;
float downLoadDataSize;

//AudioUnitPlayer *mAudioUnitPlayer;
RTMP *pPubRtmp;
RTMP *pPlayRtmp;
bool isPushing;
bool isPulling;
bool isPlaying;
bool isStartPull;
int pubTs;

void initWithSampleRate(int sampleRate, int audioEncoder) {

}

int startPublishWithUrl(char *url) {
    rtmp = RTMP_Alloc();
    if (rtmp == NULL) {
        return -1;
    }

    RTMP_Init(rtmp);

    int ret = RTMP_SetupURL(rtmp, url);
    if (!ret) {
        RTMP_Free(rtmp);
        return -2;
    }

    RTMP_EnableWrite(rtmp);

    ret = RTMP_Connect(rtmp, NULL);
    if (!ret) {
        RTMP_Free(rtmp);
        return -3;
    }

    ret = RTMP_ConnectStream(rtmp, 0);
    if (!ret) {
        return -4;
    }

    return 1;
}

void stopPublish() {
    isPushing = false;
    if (pPubRtmp) {
        if (RTMP_IsConnected(pPubRtmp)) {
            RTMP_Close(pPubRtmp);
        }
        RTMP_Free(pPubRtmp);
    }
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
    rtmp_pakt.m_nInfoField2 = rtmp->m_stream_id;
    memcpy(rtmp_pakt.m_body, buf, buflen);
    ret = RTMP_SendPacket(rtmp, &rtmp_pakt, 0);
    RTMPPacket_Free(&rtmp_pakt);

    return (ret > 0) ? 0 : -1;
}

int playerWithUrl(char *rtmpURL) {
    pPlayRtmp = RTMP_Alloc();
    RTMP_Init(pPlayRtmp);

    if (!RTMP_Connect(pPlayRtmp, NULL) || !RTMP_ConnectStream(pPlayRtmp, 0)) {
        return -1;
    }

    RTMP_SetBufferMS(pPlayRtmp, 100);
    RTMP_UpdateBufferMS(pPlayRtmp);

    RTMPPacket rtmp_pakt = {0};
    isPulling = true;

    return 1;
}

int replayWithUrl(char *rtmpURL) {
    return 1;
}

void stopPlay() {
    isPulling = false;
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = NULL;
    }
}

RTMPPacket read() {
    RTMPPacket rtmp_pakt = {0};
    if (isPulling) {
        RTMP_ReadPacket(pPlayRtmp, &rtmp_pakt);
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
                RTMP_ClientPacket(pPlayRtmp, &rtmp_pakt);
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

int rtmpIsConnected() {
    if (rtmp) {
        if (RTMP_IsConnected(rtmp)) {
            return 1;
        }
    }
    return 0;
}

void playAudioWaitingToLong() {

}

void sendCollectedPCMData(char *data) {

}

void playAudioWithBuffer(char *data) {

}


