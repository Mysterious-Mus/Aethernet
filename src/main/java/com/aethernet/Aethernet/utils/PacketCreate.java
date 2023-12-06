package com.aethernet.Aethernet.utils;

import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.util.MacAddress;

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

    public static EthernetPacket createPingPacket(
        Inet4Address src, Inet4Address dst,
        MacAddress srcMac
    ) {
        // Create ICMPv4 common header
        IcmpV4CommonPacket.Builder icmpV4CommonBuilder = new IcmpV4CommonPacket.Builder()
            .type(IcmpV4Type.ECHO)
            .code(IcmpV4Code.NO_CODE)
            .correctChecksumAtBuild(true);

        IpV4Packet.Builder ipV4Builder = new IpV4Packet.Builder()
            .version(IpVersion.IPV4)
            .srcAddr(src)
            .dstAddr(dst)
            .tos(IpV4Rfc791Tos.newInstance((byte)0)) // replace 0 with your desired ToS value
            .protocol(IpNumber.ICMPV4)
            .payloadBuilder(icmpV4CommonBuilder)
            .correctChecksumAtBuild(true);

        EthernetPacket.Builder ethBuilder = new EthernetPacket.Builder()
            .srcAddr(srcMac) // replace with your source MAC address
            .dstAddr(MacAddress.getByName("ff:ff:ff:ff:ff:ff")) // replace with your destination MAC address
            .type(EtherType.IPV4)
            .payloadBuilder(ipV4Builder)
            .paddingAtBuild(true);

        return ethBuilder.build();
    }

    public static EthernetPacket changeSrcIp(EthernetPacket original, Inet4Address newSrc) {
        IpV4Packet ipV4Packet = (IpV4Packet) original.getPayload();
        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder();
        ipV4Builder = ipV4Builder
            .srcAddr(newSrc)
            .correctChecksumAtBuild(true);

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }

    public static EthernetPacket changeSrcIp(EthernetPacket original, String newSrc) {
        return changeSrcIp(original, IPAddr.buildV4FromStr(newSrc));
    }

    public static EthernetPacket changeDstIp(EthernetPacket original, Inet4Address newDst) {
        IpV4Packet ipV4Packet = (IpV4Packet) original.getPayload();
        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder();
        ipV4Builder = ipV4Builder
            .dstAddr(newDst)
            .correctChecksumAtBuild(true);

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }

    public static EthernetPacket changeIcmpPingId(EthernetPacket original, short newId) {
        IpV4Packet ipV4Packet = (IpV4Packet) original.getPayload();
        IcmpV4EchoPacket icmpPacket = (IcmpV4EchoPacket) ipV4Packet.getPayload();

        IcmpV4EchoPacket.Builder icmpBuilder = icmpPacket.getBuilder();
        icmpBuilder = icmpBuilder
            .identifier(newId);

        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder();
        ipV4Builder = ipV4Builder
            .payloadBuilder(icmpBuilder)
            .correctChecksumAtBuild(true);

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }

    public static EthernetPacket changeIcmpPingPayload(EthernetPacket original, String newPayload) {
        IpV4Packet ipV4Packet = (IpV4Packet) original.getPayload();
        IcmpV4EchoPacket icmpPacket = (IcmpV4EchoPacket) ipV4Packet.getPayload();

        IcmpV4EchoPacket.Builder icmpBuilder = icmpPacket.getBuilder();
        icmpBuilder = icmpBuilder
            .payloadBuilder(new UnknownPacket.Builder().rawData(newPayload.getBytes()));

        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder();
        ipV4Builder = ipV4Builder
            .payloadBuilder(icmpBuilder);

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }

    public static EthernetPacket changeDstIp(EthernetPacket original, String newDst) {
        return changeDstIp(original, IPAddr.buildV4FromStr(newDst));
    }
}