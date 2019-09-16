//
// Created by faraklit on 08.02.2016.
//

#ifndef _XIECC_RTMP_H_
#define _XIECC_RTMP_H_

#include <stdint.h>
#include <stdbool.h>
#include <jni.h>
#include<android/log.h>

#ifdef __cplusplus
extern "C"{
#endif

#define RTMP_STREAM_PROPERTY_PUBLIC      0x00000001
#define RTMP_STREAM_PROPERTY_ALARM       0x00000002
#define RTMP_STREAM_PROPERTY_RECORD      0x00000004

#define TAG "rtmp-jni" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型

typedef unsigned char Byte;

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

char *getCheckCodeData(char *voipCode);

void checkCodeAudioData(char *data, int length, char *voipCode);


#ifdef __cplusplus
}
#endif
#endif
