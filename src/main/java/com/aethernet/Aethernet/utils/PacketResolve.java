package com.aethernet.Aethernet.utils;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import java.net.Inet4Address;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV4EchoPacket;

public class PacketResolve {
    public static Inet4Address getIPV4Addr(Packet packet) {
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();
            Inet4Address srcAddr = ipV4Header.getSrcAddr();
            return srcAddr;
        }
        return null;
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
                    if (icmpV4CommonPacket.contains(IcmpV4EchoPacket.class)) {
                        // IcmpV4EchoPacket icmpV4EchoReplyPacket = icmpV4CommonPacket.get(IcmpV4EchoPacket.class);
                        // if (icmpV4EchoReplyPacket.getHeader().getIdentifier() == 0x1234) {
                        //     return true;
                        // }
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
