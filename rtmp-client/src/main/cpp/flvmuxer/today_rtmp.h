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

int startPublishWithUrl(char *url);

void stopPublish();

int write(char *buf, int type, int buflen, uint64_t timestamp);

int playerWithUrl(char *url);

int replayerWithUrl(char *url);

void stopPlay();

void playAudioWaitingToLong();

void sendCollectedPCMData(char *data);

void playAudioWithBuffer(char *data);

RTMPPacket read();

int rtmpIsConnected();

#ifdef __cplusplus
}
#endif
#endif
