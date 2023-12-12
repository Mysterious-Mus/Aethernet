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
import org.pcap4j.util.MacAddress;

import com.aethernet.Aethernet.AetherRoute;
import com.aethernet.physical.transmit.AetherPacker;

import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import java.net.Inet4Address;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV4EchoPacket;
import org.pcap4j.packet.IcmpV4EchoReplyPacket;

import org.pcap4j.packet.ArpPacket;
import org.pcap4j.packet.namednumber.ArpOperation;

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

    public static boolean isArpRequestme(Packet packet, Inet4Address myIp) {
        if (packet.contains(ArpPacket.class)) {
            ArpPacket arpPacket = packet.get(ArpPacket.class);
            ArpPacket.ArpHeader arpHeader = arpPacket.getHeader();
            if (arpHeader.getOperation().equals(ArpOperation.REQUEST) && arpHeader.getDstProtocolAddr().equals(myIp)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isArpReplytme(Packet packet, Inet4Address myIp) {
        if (packet.contains(ArpPacket.class)) {
            ArpPacket arpPacket = packet.get(ArpPacket.class);
            ArpPacket.ArpHeader arpHeader = arpPacket.getHeader();
            if (arpHeader.getOperation().equals(ArpOperation.REPLY) && arpHeader.getDstProtocolAddr().equals(myIp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The get the mac address from the arp request 
     * @return 
     */
    public static Byte getMacSrcAddr(Packet packet){
        if (packet.contains(ArpPacket.class)) {
            ArpPacket arpPacket = packet.get(ArpPacket.class);
            ArpPacket.ArpHeader arpHeader = arpPacket.getHeader();
            // Get the last byte
            Byte macaddr = arpHeader.getSrcHardwareAddr().getAddress()[MacAddress.SIZE_IN_BYTES - 1];
            return macaddr;
        }
        System.err.println("The resolving packet is not an Arp Packet!");
        return null;
    }

    public static Inet4Address getSrcIP(Packet packet) {
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();
            Inet4Address srcAddr = ipV4Header.getSrcAddr();
            return srcAddr;
        }
        else if(packet.contains(ArpPacket.class)) {
            ArpPacket arpPacket = packet.get(ArpPacket.class);
            ArpPacket.ArpHeader arpHeader = arpPacket.getHeader();
            Inet4Address dstAddr = (Inet4Address)arpHeader.getSrcProtocolAddr();
            return dstAddr;
        }
        System.err.println("The resolving packet is not an valid Packet!");
        return null;
    }

    public static Inet4Address getDstIP(Packet packet) {
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();
            Inet4Address dstAddr = ipV4Header.getDstAddr();
            return dstAddr;
        }
        else if(packet.contains(ArpPacket.class)) {
            ArpPacket arpPacket = packet.get(ArpPacket.class);
            ArpPacket.ArpHeader arpHeader = arpPacket.getHeader();
            Inet4Address dstAddr = (Inet4Address)arpHeader.getDstProtocolAddr();
            return dstAddr;
        }
        System.err.println("The resolving packet is not an valid Packet!");
        return null;
    }

    public static boolean isAethernetAgent(Packet packet) {
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            if (ipV4Packet.contains(IcmpV4CommonPacket.class)) {
                IcmpV4CommonPacket icmpPacket = ipV4Packet.get(IcmpV4CommonPacket.class);
                byte[] payload = icmpPacket.getPayload().getRawData();
                String payloadString = new String(payload);
                return payloadString.equals(AetherRoute.internetAgentMagic);
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
}
