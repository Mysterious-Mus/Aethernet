package com.aethernet.Aethernet.utils;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.IcmpV4EchoReplyPacket.IcmpV4EchoReplyHeader;
import org.pcap4j.packet.UdpPacket.UdpHeader;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IcmpV4Type;

import com.aethernet.Aethernet.AetherRoute;
import com.aethernet.physical.transmit.AetherPacker;
import com.aethernet.Aethernet.SysRoute;

import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import java.net.Inet4Address;

import org.pcap4j.packet.DnsPacket;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV4EchoPacket;
import org.pcap4j.packet.IcmpV4EchoReplyPacket;

public class PacketResolve {
    public static Packet byteArr2Packet(byte[] data) {
        try {
            EthernetPacket ethernetPacket = EthernetPacket.newPacket(data, 0, data.length);
            return ethernetPacket;
        }
        catch (Exception e) {
            return null;
        }
    }

    public static boolean isIcmpPing(Packet packet) { 
        if(packet.contains(IcmpV4EchoPacket.class)) {
            return true;
        }
        return false;
    }

    public static boolean isIcmpReply(Packet packet) {
        return packet.contains(IcmpV4EchoReplyPacket.class);
    }

    public static boolean isPingReplyingMe(Packet packet, Inet4Address myIp) {
        if (isIcmpReply(packet)) {
            IpV4Packet.IpV4Header ipv4Header = packet.get(IpV4Packet.class).getHeader();
            Inet4Address dstAddr = ipv4Header.getDstAddr();
            if (dstAddr.equals(myIp)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDnsReplyingMe(Packet packet, Inet4Address myIp) {
        if (isDnsReply(packet)) {
            IpV4Packet.IpV4Header ipv4Header = packet.get(IpV4Packet.class).getHeader();
            Inet4Address dstAddr = ipv4Header.getDstAddr();
            if (dstAddr.equals(myIp)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIcmp(Packet packet) {
        return packet.contains(IcmpV4CommonPacket.class);
    }

    public static boolean isDnsQuery(Packet packet) {
        if(packet.contains(DnsPacket.class)) {
            DnsPacket dnsPacket = packet.get(DnsPacket.class);
            return (!dnsPacket.getHeader().isResponse()) && SysRoute.handleDns.contains
                (dnsPacket.getHeader().getQuestions().get(0).getQName().getName());
        }
        return false;
    }

    public static boolean isDnsReply(Packet packet) {
        if(packet.contains(DnsPacket.class)) {
            DnsPacket dnsPacket = packet.get(DnsPacket.class);
            if (dnsPacket.getHeader().isResponse()) {
                return SysRoute.handleDns.contains(dnsPacket.getHeader().getQuestions().get(0).getQName().getName());
            }
        }
        return false;
    }

    public static int DnsQueryPort(Packet packet) {
        if (isDnsQuery(packet)) {
            return packet.get(UdpPacket.class).getHeader().getSrcPort().valueAsInt();
        }
        return 0;
    }

    public static void printIcmpInfo(Packet packet) {
        if (!isIcmpPing(packet)) {
            return;
        }
        // print src, dst and type
        IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
        IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();
        Inet4Address srcAddr = ipV4Header.getSrcAddr();
        Inet4Address dstAddr = ipV4Header.getDstAddr();
        System.out.println("src: " + srcAddr);
        System.out.println("dst: " + dstAddr);
        IcmpV4CommonPacket icmpV4CommonPacket = packet.get(IcmpV4CommonPacket.class);
        System.out.println("type: " + icmpV4CommonPacket.getHeader().getType());
    }
    
    public static boolean isPingingMe(Packet packet, Inet4Address myIp) {
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();
            Inet4Address srcAddr = ipV4Header.getSrcAddr();
            Inet4Address dstAddr = ipV4Header.getDstAddr();
            if (dstAddr.equals(myIp)) {
                return packet.contains(IcmpV4EchoPacket.class);
            }
        }
        return false;
    }

    public static Inet4Address getSrcIP(Packet packet) {
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();
            Inet4Address srcAddr = ipV4Header.getSrcAddr();
            return srcAddr;
        }
        return null;
    }

    public static Inet4Address getDstIP(Packet packet) {
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();
            Inet4Address dstAddr = ipV4Header.getDstAddr();
            return dstAddr;
        }
        return null;
    }

    public static boolean isReplyingAethernetAgent(Packet packet) {
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            if (ipV4Packet.contains(IcmpV4CommonPacket.class)) {
                IcmpV4CommonPacket icmpPacket = ipV4Packet.get(IcmpV4CommonPacket.class);
                if (icmpPacket.contains(IcmpV4EchoReplyPacket.class)) {
                    IcmpV4EchoReplyPacket icmpEchoPacket = icmpPacket.get(IcmpV4EchoReplyPacket.class);
                    byte[] payload = icmpEchoPacket.getPayload().getRawData();
                    String payloadString = new String(payload);
                    return payloadString.equals(AetherRoute.internetAgentMagic);
                }
            }
        }
        return false;
    }

    public static short getIcmpId(Packet packet) {
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            if (ipV4Packet.contains(IcmpV4EchoPacket.class)) {
                IcmpV4EchoPacket icmpPacket = ipV4Packet.get(IcmpV4EchoPacket.class);
                return icmpPacket.getHeader().getIdentifier();
            }
            else if (ipV4Packet.contains(IcmpV4EchoReplyPacket.class)) {
                IcmpV4EchoReplyPacket icmpPacket = ipV4Packet.get(IcmpV4EchoReplyPacket.class);
                return icmpPacket.getHeader().getIdentifier();
            }
        }
        System.out.println("not an icmp packet");
        return 0;
    }

    public static boolean isTcpRequestPacket(Packet packet) {
        return packet.contains(TcpPacket.class) &&
            SysRoute.handleTcp.contains(getDstIP(packet));
    }

    public static boolean isTcpReplyPacket(Packet packet) {
        return packet.contains(TcpPacket.class) &&
            SysRoute.handleTcp.contains(getSrcIP(packet));
    }

    public static int getSrcPort(Packet packet) {
        TcpPacket tcpPacket = packet.get(TcpPacket.class);
        if (tcpPacket != null) {
            return tcpPacket.getHeader().getSrcPort().value() & 0xFFFF;
        }
    
        UdpPacket udpPacket = packet.get(UdpPacket.class);
        if (udpPacket != null) {
            return udpPacket.getHeader().getSrcPort().value() & 0xFFFF;
        }
    
        return -1; // Return -1 if the packet is not TCP or UDP
    }

    public static int getTcpSequenceNumber(Packet packet) {
        TcpPacket tcpPacket = packet.get(TcpPacket.class);
        if (tcpPacket != null) {
            return tcpPacket.getHeader().getSequenceNumber();
        }
    
        return -1; // Return -1 if the packet is not a TCP packet
    }

    public static boolean hasPort(Packet packet) {
        return packet.contains(TcpPacket.class) || packet.contains(UdpPacket.class);
    }

}
