#include <jni.h>
#include <malloc.h>
#include <rtmp.h>
#include <string.h>

#include "flvmuxer/today_rtmp.h"
#include "com_today_im_IMMuxer.h"
#include "flvmuxer/xiecc_rtmp.h"

#ifdef __cplusplus
extern "C" {
#endif


JNIEXPORT jint JNICALL
Java_com_today_im_IMMuxer_publish(JNIEnv *env, jobject instance, jstring host_, jint port,
                                  jstring app_, jstring path_, jstring guid_, jstring md5_,
                                  jstring voipCode_) {
    char *host = (*env)->GetStringUTFChars(env, host_, 0);
    char *app = (*env)->GetStringUTFChars(env, app_, 0);
    char *path = (*env)->GetStringUTFChars(env, path_, 0);
    char *guid = (*env)->GetStringUTFChars(env, guid_, 0);
    char *md5 = (*env)->GetStringUTFChars(env, md5_, 0);
    char *voipCode = (*env)->GetStringUTFChars(env, voipCode_, 0);

    int ret = publish(host, port, app, path, guid, md5, voipCode);

    (*env)->ReleaseStringUTFChars(env, host_, host);
    (*env)->ReleaseStringUTFChars(env, app_, app);
    (*env)->ReleaseStringUTFChars(env, path_, path);
    (*env)->ReleaseStringUTFChars(env, guid_, guid);
    (*env)->ReleaseStringUTFChars(env, md5_, md5);
    (*env)->ReleaseStringUTFChars(env, voipCode_, voipCode);

    return ret;
}

JNIEXPORT jint JNICALL
Java_com_today_im_IMMuxer_publish1(JNIEnv *env, jobject instance) {
    return publish1();
}


JNIEXPORT jint JNICALL
Java_com_today_im_IMMuxer_pull(JNIEnv *env, jobject instance, jstring host_, jint port,
                               jstring app_, jstring path_, jstring guid_, jstring md5_,
                               jstring voipCode_) {
    char *host = (*env)->GetStringUTFChars(env, host_, 0);
    char *app = (*env)->GetStringUTFChars(env, app_, 0);
    char *path = (*env)->GetStringUTFChars(env, path_, 0);
    char *guid = (*env)->GetStringUTFChars(env, guid_, 0);
    char *md5 = (*env)->GetStringUTFChars(env, md5_, 0);
    char *voipCode = (*env)->GetStringUTFChars(env, voipCode_, 0);

    int ret = pull(host, port, app, path, guid, md5, voipCode);

    (*env)->ReleaseStringUTFChars(env, host_, host);
    (*env)->ReleaseStringUTFChars(env, app_, app);
    (*env)->ReleaseStringUTFChars(env, path_, path);
    (*env)->ReleaseStringUTFChars(env, guid_, guid);
    (*env)->ReleaseStringUTFChars(env, md5_, md5);
    (*env)->ReleaseStringUTFChars(env, voipCode_, voipCode);

    return ret;
}


JNIEXPORT jobject JNICALL Java_com_today_im_IMMuxer_initWithSampleRate
        (JNIEnv *env, jobject instance, jint sampleRate, jint audioEncoder) {
    return NULL;
}

JNIEXPORT jint JNICALL Java_com_today_im_IMMuxer_publishWithUrl
        (JNIEnv *env, jobject instance, jstring rtmpURL) {
    char *url = (*env)->GetStringUTFChars(env, rtmpURL, 0);

//    int result = publishWithUrl(url);

    (*env)->ReleaseStringUTFChars(env, rtmpURL, url);
    return 1;
}

JNIEXPORT void JNICALL Java_com_today_im_IMMuxer_stopPublish
        (JNIEnv *env, jobject instance) {
    stopPublish();
}


JNIEXPORT jint JNICALL Java_com_today_im_IMMuxer_isPublishConnected
        (JNIEnv *env, jobject instance) {
    return publishRtmpIsConnected();
}


JNIEXPORT jint JNICALL Java_com_today_im_IMMuxer_write
        (JNIEnv *env, jobject instance, jbyteArray data_, jint type, jint length,
         jint timestamp) {
    jbyte *bytes = (*env)->GetByteArrayElements(env, data_, NULL);
    const char speex_head = '\xB6';

    size_t opusLength = (*env)->GetArrayLength(env, data_);
    char *send_data = malloc(opusLength);
    memcpy(send_data, bytes, opusLength);

    LOGD("write audioLength=%d, audioData=%s", opusLength, send_data);
//    checkCodeAudioData(send_data, opusLength);

    int headSize = 1;
    int send_buf_lgth = (int) opusLength + headSize;
    char *send_buf = malloc(send_buf_lgth);

    memcpy(send_buf, &speex_head, 1);
    memcpy(send_buf + headSize, send_data, opusLength);

    int sendType = RTMP_PACKET_TYPE_AUDIO;
    if ((int) type == 3) {
        sendType = RTMP_PACKET_TYPE_AUDIO;
    }

    jint result = write(send_buf, sendType, send_buf_lgth, timestamp);

    (*env)->ReleaseByteArrayElements(env, data_, bytes, 0);

    free(send_data);
    free(send_buf);

    return result;
}

JNIEXPORT jint JNICALL Java_com_today_im_IMMuxer_pullWithUrl
        (JNIEnv *env, jobject instance, jstring rtmpURL) {
    char *url = (*env)->GetStringUTFChars(env, rtmpURL, 0);

    int result = pullWithUrl(url);

    (*env)->ReleaseStringUTFChars(env, rtmpURL, url);

    return result;
}

JNIEXPORT void JNICALL Java_com_today_im_IMMuxer_replayWithUrl
        (JNIEnv *, jobject, jstring);

JNIEXPORT void JNICALL Java_com_today_im_IMMuxer_stopPull
        (JNIEnv *env, jobject instance) {
    stopPull();
}


JNIEXPORT jint JNICALL Java_com_today_im_IMMuxer_isConnected
        (JNIEnv *env, jobject instance) {
    return pullRtmpIsConnected();
}


JNIEXPORT void JNICALL Java_com_today_im_IMMuxer_stopCalled
        (JNIEnv *, jobject);

JNIEXPORT jobject JNICALL Java_com_today_im_IMMuxer_read
        (JNIEnv *env, jobject instance) {
    RTMPPacket packet = read();
    if (packet.m_body == NULL) {
        return NULL;
    }

    // 再次对音频数据编码还原
    int headSize = 1;
    int size = packet.m_nBodySize - headSize;
    char *body = malloc(size);
    memcpy(body, packet.m_body + headSize, size);
    LOGD("read audioLength=%d, audioData=%s", size, body);
//    checkCodeAudioData(body, size);

    jclass objectClass = (*env)->FindClass(env, "com/today/im/PacketInfo");
    if (objectClass == NULL) {
        return NULL;
    }

    int type = 3;
    if (packet.m_packetType == RTMP_PACKET_TYPE_AUDIO) {
        type = 3;
    }

    jbyteArray byteA = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(env, byteA, 0, size, body);

    jmethodID constructor = (*env)->GetMethodID(env, objectClass, "<init>", "(IIIIII[B)V");

    jobject result = (*env)->NewObject(env, objectClass, constructor, packet.m_headerType,
                                       type, packet.m_hasAbsTimestamp,
                                       packet.m_nTimeStamp, packet.m_nInfoField2,
                                       size, byteA);

//    (*env)->ReleaseByteArrayElements(env, byteA, packet.m_body, 0);
    (*env)->DeleteLocalRef(env, objectClass);
    (*env)->DeleteLocalRef(env, byteA);
    free(body);
//    (*env)->DeleteLocalRef(env, byteA);
//    (*env)->DeleteLocalRef(env, constructor);

    return result;
}


#ifdef __cplusplus
}
#endif
