package com.aethernet.Aethernet.utils;

import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;

import java.net.*;
import java.util.Random;

public class PacketCreate {
    public static EthernetPacket createReplyPacket(EthernetPacket requestPacket) {
        IpV4Packet ipV4Packet = (IpV4Packet) requestPacket.getPayload();
        IcmpV4CommonPacket icmpV4CommonPacket = (IcmpV4CommonPacket) ipV4Packet.getPayload();
        
        // Create ICMPv4 common header
        IcmpV4CommonPacket.Builder icmpV4CommonBuilder = icmpV4CommonPacket.getBuilder();
        icmpV4CommonBuilder = icmpV4CommonBuilder
            .type(IcmpV4Type.ECHO_REPLY)
            .code(IcmpV4Code.NO_CODE)
            .correctChecksumAtBuild(true);

        // Swap source and destination addresses in IPv4 header
        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder();
        ipV4Builder = ipV4Builder
            .srcAddr(ipV4Packet.getHeader().getDstAddr())
            .dstAddr(ipV4Packet.getHeader().getSrcAddr())
            .payloadBuilder(icmpV4CommonBuilder)
            .correctChecksumAtBuild(true);

        // Swap source and destination MAC addresses in Ethernet header
        EthernetPacket.Builder ethBuilder = requestPacket.getBuilder();
        ethBuilder = ethBuilder
            .dstAddr(requestPacket.getHeader().getSrcAddr())
            .srcAddr(requestPacket.getHeader().getDstAddr())
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }
}