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

int publish(char *host, int port, char *app, char *path, char *guid, char *md5);

int pull(char *host, int port, char *app, char *path, char *guid, char *md5);

int publish1();

//int publishWithUrl(char *url);

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

char *newGUID();

char *dataWithHexString(char *voipCode);

char *getCheckCodeData(char *data, int length);

void checkCodeAudioData(char *data, int length);

#ifdef __cplusplus
}
#endif
#endif
