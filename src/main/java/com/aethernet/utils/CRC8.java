package com.aethernet.utils;

public class CRC8 {
    private static final byte[] table = new byte[256];

    static {
        for (int i = 0; i < 256; i++) {
            byte curr = (byte) i;
            for (int j = 0; j < 8; j++) {
                if ((curr & 0x80) != 0) {
                    curr = (byte) ((curr << 1) ^ 0x07);
                } else {
                    curr <<= 1;
                }
            }
            table[i] = curr;
        }
    }

    public static byte compute(byte[] bytes) {
        byte crc = 0;
        for (byte b : bytes) {
            crc = table[((crc ^ b) & 0xff)];
        }
        return crc;
    }
}
