package com.aethernet.utils;

import com.aethernet.mac.MacFrame;

public class AddrAlloc {
    private byte hasAlloced = 0;

    public AddrAlloc() {

    }

    public byte acquireAddr() {
        if (hasAlloced > MacFrame.Configs.addrUb) {
            System.out.println("Address out of range");
            return 0;
        }
        return hasAlloced ++;
    }
}
