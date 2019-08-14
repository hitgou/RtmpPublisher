#include <jni.h>
#include <malloc.h>
#include <string.h>
#include "include/opus.h"

#ifndef _Included_com_aliyun_nls_transcription_opu_OpuCodec
#define _Included_com_aliyun_nls_transcription_opu_OpuCodec
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_today_im_opus_OpusUtils_createEncoder
        (JNIEnv *env, jobject thiz, jint sampleRateInHz, jint channelConfig, jint complexity) {
    int error;
    OpusEncoder *pOpusEnc = opus_encoder_create(sampleRateInHz, channelConfig,
                                                OPUS_APPLICATION_VOIP, &error);
    if (pOpusEnc) {
        opus_encoder_ctl(pOpusEnc, OPUS_SET_VBR(0));//0:CBR, 1:VBR
        opus_encoder_ctl(pOpusEnc, OPUS_SET_VBR_CONSTRAINT(true));
        opus_encoder_ctl(pOpusEnc, OPUS_SET_BITRATE(16000));
        opus_encoder_ctl(pOpusEnc, OPUS_SET_COMPLEXITY(complexity));//8    0~10
        opus_encoder_ctl(pOpusEnc, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
        opus_encoder_ctl(pOpusEnc, OPUS_SET_LSB_DEPTH(16));
        opus_encoder_ctl(pOpusEnc, OPUS_SET_DTX(0));
        opus_encoder_ctl(pOpusEnc, OPUS_SET_INBAND_FEC(0));
        opus_encoder_ctl(pOpusEnc, OPUS_SET_PACKET_LOSS_PERC(0));
    }
    return (jlong) pOpusEnc;
}
JNIEXPORT jlong JNICALL Java_com_today_im_opus_OpusUtils_createDecoder
        (JNIEnv *env, jobject thiz, jint sampleRateInHz, jint channelConfig) {
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
    // jsize nByteSize = env->GetArrayLength(bytes);
    //if (nSampleSize - offset < 320 || nByteSize <= 0)
    if (nSampleSize - offset < 320)
        return 0;
    int nRet = opus_encode(pEnc, pSamples + offset, nSampleSize, (unsigned char *) pBytes,
                           1280);
    env->ReleaseShortArrayElements(buffer, pSamples, 0);
    env->ReleaseByteArrayElements(bytes, pBytes, 0);
    return nRet;


//    //读取参数 数组
//    (*env)->GetShortArrayRegion(env, buffer, offset, size, out);
//    nbBytes = opus_encode(pEnc, out, size, cbits, 480);
//    //设定压缩完的数据到数组中
//    (*env)->SetByteArrayRegion(env, buffer, 0, nbBytes, (jshort *) cbits);

//    return nbBytes;
//    jsize lengthOfShorts = env->GetArrayLength(buffer);
//    jint frame_size = lengthOfShorts / sizeof(short);
//    short input_frame[frame_size];
//    memcpy(input_frame, buffer, lengthOfShorts);

//    jshort *pSamples = env->GetShortArrayElements(buffer, 0);

//    jbyte *pBytes = env->GetByteArrayElements(bytes, 0);
//    jsize nByteSize = env->GetArrayLength(bytes);
//    if (lengthOfShorts - offset < 320 || nByteSize <= 0)
//        return 0;

//    int max_data_bytes = 2 * 640;
//    int ret = opus_encode(pEnc, input_frame, lengthOfShorts, (unsigned char *) pBytes,
//                          max_data_bytes);
//    env->ReleaseShortArrayElements(buffer, pSamples, 0);
//    env->ReleaseByteArrayElements(bytes, pBytes, 0);

//    return ret;

//    opus_encode(OpusEncoder *st, const opus_int16 *pcm, int frame_size, unsigned char *data, opus_int32 max_data_bytes
//
//(short *)pcmBuffer length:(NSInteger)lengthOfShorts
//    int frame_size = (int)lengthOfShorts / sizeof(short);//WB_FRAME_SIZE;
//    short input_frame[frame_size];
//    opus_int32 max_data_bytes = 2 * WB_FRAME_SIZE ;//随便设大,此时为原始PCM大小
//    memcpy(input_frame, pcmBuffer, lengthOfShorts );//frame_size * sizeof(short)
//    int encodeBack = opus_encode(enc, input_frame, frame_size, opus_data_encoder, max_data_bytes);
//    if (encodeBack > 0) {
//        NSData *decodedData = [NSData dataWithBytes:opus_data_encoder length:encodeBack];
//        return decodedData;
//    } else {
//        return nil;
//    }
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