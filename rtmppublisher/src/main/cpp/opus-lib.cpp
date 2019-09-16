#include <jni.h>
#include <malloc.h>
#include <string.h>
#include "include/opus.h"

#ifndef _Included_com_aliyun_nls_transcription_opu_OpuCodec
#define _Included_com_aliyun_nls_transcription_opu_OpuCodec
#ifdef __cplusplus
extern "C" {
#endif


JNIEXPORT jlong JNICALL
Java_com_today_im_opus_OpusUtils_initEncoder(JNIEnv *env, jobject instance, jint samplingRate,
                                             jint numberOfChannels, jint application) {
    int error;
    int size;

    size = opus_encoder_get_size(1);
    OpusEncoder *enc = (OpusEncoder *) malloc(size);
    error = opus_encoder_init(enc, samplingRate, numberOfChannels, OPUS_APPLICATION_VOIP);
    if (error) {
        free(enc);
        return 0;
    }
//    opus_encoder_ctl(enc, OPUS_SET_BITRATE(32000));

    return (jlong) enc;
}


JNIEXPORT jlong JNICALL
Java_com_today_im_opus_OpusUtils_initDecoder(JNIEnv *env, jobject instance, jint samplingRate,
                                             jint numberOfChannels) {
    int size;
    int error;

    size = opus_decoder_get_size(numberOfChannels);
    OpusDecoder *dec = (OpusDecoder *) malloc(size);
    error = opus_decoder_init(dec, samplingRate, numberOfChannels);
    if (error) {
        free(dec);
        return 0;
    }

    return (jlong) dec;
}


JNIEXPORT jlong JNICALL Java_com_today_im_opus_OpusUtils_createEncoder
        (JNIEnv *env, jobject thiz, jint sampleRateInHz, jint channelConfig, jint complexity) {
    int error;
    OpusEncoder *pOpusEnc = opus_encoder_create(sampleRateInHz, channelConfig,
                                                OPUS_APPLICATION_VOIP, &error);
    return (jlong) pOpusEnc;
}
JNIEXPORT jlong JNICALL Java_com_today_im_opus_OpusUtils_createDecoder
        (JNIEnv *env, jobject thiz, jint sampleRateInHz, jint channelConfig, jint complexity) {
    int error;
    OpusDecoder *pOpusDec = opus_decoder_create(sampleRateInHz, channelConfig, &error);
    return (jlong) pOpusDec;
}

JNIEXPORT jint JNICALL Java_com_today_im_opus_OpusUtils_encode
        (JNIEnv *env, jobject thiz, jlong pOpusEnc, jshortArray buffer, jint offset,
         jbyteArray bytes) {
    OpusEncoder *pEnc = (OpusEncoder *) pOpusEnc;
    if (!pEnc || !buffer || !bytes)
        return 0;

    jshort *pSamples = env->GetShortArrayElements(buffer, 0);
    jsize nSampleSize = env->GetArrayLength(buffer);
    jbyte *pBytes = env->GetByteArrayElements(bytes, 0);
    jsize nByteSize = env->GetArrayLength(bytes);
    //if (nSampleSize - offset < 320 || nByteSize <= 0)
//    if (nSampleSize - offset < 320)
//        return 0;
    int nRet = opus_encode(pEnc, pSamples + offset, nSampleSize, (unsigned char *) pBytes,
                           nByteSize);
    env->ReleaseShortArrayElements(buffer, pSamples, 0);
    env->ReleaseByteArrayElements(bytes, pBytes, 0);
    return nRet;
}


JNIEXPORT jint JNICALL Java_com_today_im_opus_OpusUtils_encodeByte
        (JNIEnv *env, jobject thiz, jlong pOpusEnc, jbyteArray in, jint frames,
         jbyteArray out) {
    OpusEncoder *enc = (OpusEncoder *) pOpusEnc;
    if (!enc || !in || !out)
        return 0;

    jint outputArraySize = env->GetArrayLength(out);
    jbyte *audioSignal = env->GetByteArrayElements(in, 0);
    jbyte *encodedSignal = env->GetByteArrayElements(out, 0);

    if (((unsigned long) audioSignal) % 2) {
        // Unaligned...
        return OPUS_BAD_ARG;
    }

    int dataArraySize = opus_encode(enc, (const opus_int16 *) audioSignal, frames,
                                    (unsigned char *) encodedSignal, outputArraySize);

    env->ReleaseByteArrayElements(in, audioSignal, JNI_ABORT);
    env->ReleaseByteArrayElements(out, encodedSignal, 0);

    return dataArraySize;
}


JNIEXPORT jint JNICALL Java_com_today_im_opus_OpusUtils_decode
        (JNIEnv *env, jobject thiz, jlong pOpusDec, jbyteArray bytes,
         jshortArray samples) {
    OpusDecoder *pDec = (OpusDecoder *) pOpusDec;
    if (!pDec || !samples || !bytes)
        return 0;
    jshort *pSamples = env->GetShortArrayElements(samples, 0);
    jbyte *pBytes = env->GetByteArrayElements(bytes, 0);
    jsize nByteSize = env->GetArrayLength(bytes);
    jsize nShortSize = env->GetArrayLength(samples);
    if (nByteSize <= 0 || nShortSize <= 0) {
        return -1;
    }
    int nRet = opus_decode(pDec, (unsigned char *) pBytes, nByteSize, pSamples, nShortSize, 0);
    env->ReleaseShortArrayElements(samples, pSamples, 0);
    env->ReleaseByteArrayElements(bytes, pBytes, 0);
    return nRet;
}

JNIEXPORT jint JNICALL
Java_com_today_im_opus_OpusUtils_decodeShorts(JNIEnv *env, jobject instance, jlong pOpusDec,
                                              jbyteArray in_, jshortArray out_, jint frames) {
    OpusDecoder *dec = (OpusDecoder *) pOpusDec;

    jint inputArraySize = env->GetArrayLength(in_);

    jbyte *encodedData = env->GetByteArrayElements(in_, 0);
    jshort *decodedData = env->GetShortArrayElements(out_, 0);
    int samples = opus_decode(dec, (const unsigned char *) encodedData, inputArraySize,
                              decodedData, frames, 0);

    env->ReleaseByteArrayElements(in_, encodedData, JNI_ABORT);
    env->ReleaseShortArrayElements(out_, decodedData, 0);

    return samples;
}

JNIEXPORT void JNICALL Java_com_today_im_opus_OpusUtils_destroyEncoder
        (JNIEnv *env, jobject thiz, jlong pOpusEnc) {
    OpusEncoder *pEnc = (OpusEncoder *) pOpusEnc;
    if (!pEnc)
        return;
    opus_encoder_destroy(pEnc);
}

JNIEXPORT void JNICALL Java_com_today_im_opus_OpusUtils_destroyDecoder
        (JNIEnv *env, jobject thiz, jlong pOpusDec) {
    OpusDecoder *pDec = (OpusDecoder *) pOpusDec;
    if (!pDec)
        return;
    opus_decoder_destroy(pDec);
}
#ifdef __cplusplus
}
#endif
#endif