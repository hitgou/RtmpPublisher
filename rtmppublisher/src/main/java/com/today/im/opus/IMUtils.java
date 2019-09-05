package com.today.im.opus;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import kotlin.jvm.internal.Intrinsics;

public class IMUtils {
    public static final IMUtils INSTANCE;

    @NotNull
    public final short[] byteArrayToShortArray(@NotNull byte[] byteArray) {
        Intrinsics.checkParameterIsNotNull(byteArray, "byteArray");
        short[] shortArray = new short[byteArray.length / 2];
        ByteBuffer.wrap(byteArray).order(ByteOrder.nativeOrder()).asShortBuffer().get(shortArray);
        return shortArray;
    }

    @NotNull
    public final byte[] shortArrayToByteArray(@NotNull short[] shortArray) {
        Intrinsics.checkParameterIsNotNull(shortArray, "shortArray");
        int count = shortArray.length;
        byte[] dest = new byte[count << 1];
        int i = 0;

        for (int var5 = count; i < var5; ++i) {
            int var10001 = i * 2;
            short var6 = shortArray[i];
            short var7 = (short) '\uffff';
            int var10 = var10001;
            boolean var8 = false;
            short var11 = (short) (var6 & var7);
            dest[var10] = (byte) ((int) ((long) var11 >> 0));
            var10001 = i * 2 + 1;
            var6 = shortArray[i];
            var7 = (short) '\uffff';
            var10 = var10001;
            var8 = false;
            var11 = (short) (var6 & var7);
            dest[var10] = (byte) ((int) ((long) var11 >> 8));
        }

        return dest;
    }

    private IMUtils() {
    }

    static {
        IMUtils var0 = new IMUtils();
        INSTANCE = var0;
    }


}
