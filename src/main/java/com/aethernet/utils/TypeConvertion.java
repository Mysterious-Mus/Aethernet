package com.aethernet.utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class TypeConvertion {

    /**
     * Convert a boolean list to a byte array, padding 0s to the end
     * @param booleanList
     * @return
     */
    public static byte[] booleanList2ByteArray(ArrayList<Boolean> booleanList) {
        byte[] byteArray = new byte[(booleanList.size() + 7) / 8];
        for (int i = 0; i < booleanList.size(); i++) {
            if (booleanList.get(i)) {
                byteArray[i / 8] |= (1 << (7 - i % 8));
            }
        }
        return byteArray;
    }

    public static String byteArray2String(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteArray) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static byte[] Long2ByteArray(long value) {
        byte[] res = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(value).array();
        // remove the first 4 bytes (leading 0s)
        return Arrays.copyOfRange(res, 4, 8);
    }

    public static Long byteArray2Long(byte[] byteArray) {
        byte[] res = new byte[8];
        // add the first 4 bytes (leading 0s)
        System.arraycopy(byteArray, 0, res, 4, 4);
        return ByteBuffer.wrap(res).getLong();
    }

    public static ArrayList<Boolean> byteArray2BooleanList(byte[] byteArray) {
        ArrayList<Boolean> booleanList = new ArrayList<>();
        for (byte b : byteArray) {
            for (int i = 7 ; i >= 0 ; i--) {
                boolean bit = ((b >> i) & 1) == 1;
                booleanList.add(bit);
            }
        }
        return booleanList;
    }

    public static float[] floatList2Floatarray(ArrayList<Float> floatList) {
        float[] floatArray = new float[floatList.size()];
        for (int i = 0; i < floatList.size(); i++) {
            floatArray[i] = floatList.get(i);
        }
        return floatArray;
    }

    public static ArrayList<Boolean> concatList(ArrayList<Boolean> list1, ArrayList<Boolean> list2) {
        ArrayList<Boolean> res = new ArrayList<>();
        res.addAll(list1);
        res.addAll(list2);
        return res;
    }

    public static ArrayList<Float> floatArr2FloatList(float[] arr) {
        ArrayList<Float> res = new ArrayList<>();
        for (float f : arr) {
            res.add(f);
        }
        return res;
    }
}
