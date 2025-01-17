/*
 * Coding by Zhonghua. from 18-9-14 下午5:56.
 */

package com.today.im.opus

/**
 * JNI操作
 */
public class OpusUtils {
    init {
        System.loadLibrary("opus-jni")
    }

    companion object {
        private var opusUtils: OpusUtils? = null
        fun getInstant(): OpusUtils {
            if (opusUtils == null) {
                synchronized(OpusUtils::class.java) {
                    if (opusUtils == null) {
                        opusUtils = OpusUtils()
                    }
                }
            }
            return opusUtils!!
        }
    }

    external fun createEncoder(sampleRateInHz: Int, channelConfig: Int, complexity: Int): Long
    external fun createDecoder(sampleRateInHz: Int, channelConfig: Int): Long
    external fun encode(handle: Long, lin: ShortArray, offset: Int, encoded: ByteArray): Int
    external fun decode(handle: Long, encoded: ByteArray, lin: ShortArray): Int
    external fun destroyEncoder(handle: Long)
    external fun destroyDecoder(handle: Long)
}