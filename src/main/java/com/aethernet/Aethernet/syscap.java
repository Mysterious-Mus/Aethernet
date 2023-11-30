package com.aethernet.Aethernet;

import com.aethernet.utils.CyclicBuffer;

import org.pcap4j.packet.Packet;

/**
 * this class is used to capture all system packages.
 * the system TCP/IP stack will accept packets from all adapters。
 * for athernet, it will do the same.
 */
public class syscap {
    
    public static CyclicBuffer<Packet> buffer = new CyclicBuffer<Packet>(1000);
    
    /**
     * start capturing all packets from all adapters
     */
    public static void start() {
        
    }
}
