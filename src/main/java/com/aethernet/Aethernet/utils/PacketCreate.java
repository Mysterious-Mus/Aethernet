package com.aethernet.Aethernet.utils;

import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.util.LinkLayerAddress;
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

    public static EthernetPacket createArpRequest(
            Byte srcMac, Inet4Address srcIp, Inet4Address dstIp
    ) {
        MacAddress ethernetAddr = MacAddress.getByAddress(new byte[]{0, 0, 0, 0, 0, srcMac});
        MacAddress broadcastAddr = MacAddress.getByAddress(new byte[]{-1, -1, -1, -1, -1, -1});
        ArpPacket.Builder arpBuilder = new ArpPacket.Builder();
        arpBuilder
            .hardwareType(ArpHardwareType.ETHERNET)
            .protocolType(EtherType.IPV4)
            .hardwareAddrLength((byte) MacAddress.SIZE_IN_BYTES)       // mac addr 
            .protocolAddrLength((byte) 4)       // ip addr
            .operation(ArpOperation.REQUEST)
            .srcHardwareAddr(ethernetAddr)
            .srcProtocolAddr(srcIp)
            .dstHardwareAddr(broadcastAddr)
            .dstProtocolAddr(dstIp);

        EthernetPacket.Builder etherBuilder = new EthernetPacket.Builder();
        etherBuilder
            .dstAddr(broadcastAddr)
            .srcAddr(ethernetAddr)
            .type(EtherType.ARP)
            .payloadBuilder(arpBuilder)
            .paddingAtBuild(true);

        return etherBuilder.build();
    }

    public static EthernetPacket createArpReply(EthernetPacket requestPacket, Byte replyMac) {

        MacAddress ethernetAddr = MacAddress.getByAddress(new byte[]{0, 0, 0, 0, 0, replyMac});
        ArpPacket arpRequest = requestPacket.get(ArpPacket.class);
        ArpPacket.ArpHeader arpRequestHeader = arpRequest.getHeader();

        ArpPacket.Builder arpReplyBuilder = new ArpPacket.Builder();
        arpReplyBuilder
            .hardwareType(arpRequestHeader.getHardwareType())
            .protocolType(arpRequestHeader.getProtocolType())
            .hardwareAddrLength(arpRequestHeader.getHardwareAddrLength())
            .protocolAddrLength(arpRequestHeader.getProtocolAddrLength())
            .operation(ArpOperation.REPLY)
            .srcHardwareAddr(ethernetAddr)
            .srcProtocolAddr(arpRequestHeader.getDstProtocolAddr())
            .dstHardwareAddr(arpRequestHeader.getSrcHardwareAddr())
            .dstProtocolAddr(arpRequestHeader.getSrcProtocolAddr());

        EthernetPacket.Builder etherReplyBuilder = new EthernetPacket.Builder();
        etherReplyBuilder
            .dstAddr(requestPacket.getHeader().getSrcAddr())
            .srcAddr(ethernetAddr)
            .type(requestPacket.getHeader().getType())
            .payloadBuilder(arpReplyBuilder)
            .paddingAtBuild(true);

        return etherReplyBuilder.build();
    }

    public static EthernetPacket correctIpV4Checksum(EthernetPacket original) {
        IpV4Packet ipV4Packet = (IpV4Packet) original.getPayload();
        
        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder()
            .correctChecksumAtBuild(true);

        EthernetPacket.Builder ethBuilder = original.getBuilder()
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }

    public static EthernetPacket changeSrcIp(EthernetPacket original, Inet4Address newSrc) {
        IpV4Packet ipV4Packet = (IpV4Packet) original.getPayload();
        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder();
        ipV4Builder = ipV4Builder
            .srcAddr(newSrc);

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
            .dstAddr(newDst);

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }

    public static EthernetPacket changeSrcMac(EthernetPacket original, LinkLayerAddress newSrcMac) {
        MacAddress newSrcMacAddress = MacAddress.getByName(newSrcMac.toString());

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .srcAddr(newSrcMacAddress);

        return ethBuilder.build();
    }

    public static EthernetPacket changeDstMac(EthernetPacket original, LinkLayerAddress newDstMac) {
        MacAddress newSrcMacAddress = MacAddress.getByName(newDstMac.toString());

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .dstAddr(newSrcMacAddress);

        return ethBuilder.build();
    }

    public static EthernetPacket changeIcmpPingId(EthernetPacket original, short newId) {
        IpV4Packet ipV4Packet = (IpV4Packet) original.getPayload();
        if (!ipV4Packet.contains(IcmpV4EchoPacket.class)) {
            System.out.println("not icmp echo!");
        }
        IcmpV4CommonPacket icmpPacket = ipV4Packet.get(IcmpV4CommonPacket.class);

        IcmpV4EchoPacket.Builder icmpEchoBuilder = ((IcmpV4EchoPacket)icmpPacket.getPayload()).getBuilder();
        icmpEchoBuilder = icmpEchoBuilder.identifier(newId);

        IcmpV4CommonPacket.Builder icmpBuilder = icmpPacket.getBuilder();
        icmpBuilder = icmpBuilder
            .payloadBuilder(icmpEchoBuilder);

        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder();
        ipV4Builder = ipV4Builder
            .payloadBuilder(icmpBuilder);

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }

    public static EthernetPacket changeIcmpEchoReplyId(EthernetPacket original, short newId) {
        IpV4Packet ipV4Packet = (IpV4Packet) original.getPayload();
        if (!ipV4Packet.contains(IcmpV4EchoReplyPacket.class)) {
            System.out.println("not icmp echo reply!");
        }
        IcmpV4CommonPacket icmpPacket = ipV4Packet.get(IcmpV4CommonPacket.class);

        IcmpV4EchoReplyPacket.Builder icmpEchoBuilder = ((IcmpV4EchoReplyPacket)icmpPacket.getPayload()).getBuilder();
        icmpEchoBuilder = icmpEchoBuilder.identifier(newId);

        IcmpV4CommonPacket.Builder icmpBuilder = icmpPacket.getBuilder();
        icmpBuilder = icmpBuilder
            .payloadBuilder(icmpEchoBuilder);

        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder();
        ipV4Builder = ipV4Builder
            .payloadBuilder(icmpBuilder);

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }

    public static EthernetPacket changeIcmpPingPayload(EthernetPacket original, String newPayload) {
        IpV4Packet ipV4Packet = (IpV4Packet) original.getPayload();
        assert ipV4Packet.contains(IcmpV4EchoPacket.class);
        IcmpV4CommonPacket icmpPacket = ipV4Packet.get(IcmpV4CommonPacket.class);

        IcmpV4CommonPacket.Builder icmpBuilder = icmpPacket.getBuilder();
        icmpBuilder = icmpBuilder
            .payloadBuilder(new UnknownPacket.Builder().rawData(newPayload.getBytes()));

        IpV4Packet.Builder ipV4Builder = ipV4Packet.getBuilder();
        ipV4Builder = ipV4Builder
            .payloadBuilder(icmpBuilder)
            .totalLength(
                (short)(ipV4Packet.length() + 
                (newPayload.length() - icmpPacket.getPayload().length()))
            );

        EthernetPacket.Builder ethBuilder = original.getBuilder();
        ethBuilder = ethBuilder
            .payloadBuilder(ipV4Builder);

        return ethBuilder.build();
    }

    public static EthernetPacket changeDstIp(EthernetPacket original, String newDst) {
        return changeDstIp(original, IPAddr.buildV4FromStr(newDst));
    }
}