package com.aethernet.Aethernet.utils;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.IcmpV4EchoReplyPacket.IcmpV4EchoReplyHeader;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IcmpV4Type;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import java.net.Inet4Address;
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
        if(packet.contains(IcmpV4CommonPacket.class)) {
            return true;
        }
        return false;
    }

    public static boolean isIcmpReply(Packet packet) {
        return packet.contains(IcmpV4EchoReplyPacket.class);
    }

    public static boolean isReplyingMe(Packet packet, Inet4Address myIp) {
        if (isIcmpReply(packet)) {
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
                if (packet.contains(IcmpV4CommonPacket.class)) {
                    IcmpV4CommonPacket icmpV4CommonPacket = packet.get(IcmpV4CommonPacket.class);
                    // if type is echo
                    if (icmpV4CommonPacket.getHeader().getType() == IcmpV4Type.ECHO) {
                        return true;
                    }
                }
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
}
