//
// Created by faraklit on 08.02.2016.
//

#ifndef _XIECC_RTMP_H_
#define _XIECC_RTMP_H_

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C"{
#endif

#define RTMP_STREAM_PROPERTY_PUBLIC      0x00000001
#define RTMP_STREAM_PROPERTY_ALARM       0x00000002
#define RTMP_STREAM_PROPERTY_RECORD      0x00000004

void initWithSampleRate(int sampleRate, int audioEncoder);

int publishWithUrl(char *url);

void stopPublish();

int publishRtmpIsConnected();

int write(char *buf, int type, int buflen, uint64_t timestamp);

int pullWithUrl(char *url);

int replayWithUrl(char *url);

void stopPull();

int pullRtmpIsConnected();

void playAudioWaitingToLong();

void sendCollectedPCMData(char *data);

void playAudioWithBuffer(char *data);

RTMPPacket read();



#ifdef __cplusplus
}
#endif
#endif
