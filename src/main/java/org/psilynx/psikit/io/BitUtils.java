package org.psilynx.psikit.io;

import java.util.ArrayList;
import java.util.List;

public class BitUtils {

    public static BitRangeSlice sliceBits(byte[] input, int[] range) {
        int start = range[0];
        int end = range[1];

        if (start % 8 == 0 && end % 8 == 0) {
            return new BitRangeSlice(
                    java.util.Arrays.copyOfRange(input, start / 8, end / 8),
                    end - start
            );
        }

        List<Boolean> bools = toBoolArray(input);
        List<Boolean> slice = bools.subList(start, end);
        return new BitRangeSlice(toUint8Array(slice), end - start);
    }

    public static List<Boolean> toBoolArray(byte[] bytes) {
        List<Boolean> bits = new ArrayList<>();
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                bits.add(((b >> i) & 1) == 1);
            }
        }
        return bits;
    }

    public static byte[] toUint8Array(List<Boolean> bits) {
        byte[] result = new byte[(int) Math.ceil(bits.size() / 8.0)];
        for (int i = 0; i < bits.size(); i++) {
            if (bits.get(i)) {
                int byteIndex = i / 8;
                int bitIndex = i % 8;
                result[byteIndex] |= (1 << bitIndex);
            }
        }
        return result;
    }

    public static class BitRangeSlice {
        public final byte[] value;
        public final int bitLength;

        public BitRangeSlice(byte[] value, int bitLength) {
            this.value = value;
            this.bitLength = bitLength;
        }
    }
}