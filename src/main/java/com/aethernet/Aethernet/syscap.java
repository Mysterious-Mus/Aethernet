package com.aethernet.Aethernet;

import java.util.List;

import com.aethernet.utils.CyclicBuffer;

import com.sun.jna.Platform;
import java.io.IOException;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.PcapStat;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.NifSelector;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Packet.IpV4Header;
import java.net.Inet4Address;
import org.pcap4j.packet.EthernetPacket;

/**
 * this class is used to capture all system packages.
 * the system TCP/IP stack will accept packets from all adapters。
 * for athernet, it will do the same.
 */
public class syscap {

    public static class Configs {
        public static int SNAPLEN = 65536;
        public static int READ_TIMEOUT = 10;
    }
    
    public static CyclicBuffer<Packet> buffer = new CyclicBuffer<Packet>(1000);
    
    /**
     * start capturing all packets from all adapters
     */
    public static void start() {
        List<PcapNetworkInterface> allDevs = null;
        try {
            allDevs = Pcaps.findAllDevs();
        }
        catch (PcapNativeException e) {
            e.printStackTrace();
        }

        for (PcapNetworkInterface device : allDevs) {
            new Thread(() -> {
                try {
                    final PcapHandle handle = device.openLive(
                        Configs.SNAPLEN, PromiscuousMode.PROMISCUOUS, Configs.READ_TIMEOUT);
                    handle.loop(-1, new PacketListener() {
                        @Override
                        public void gotPacket(Packet packet) {
                            if (packet instanceof EthernetPacket) {
                                EthernetPacket ethPacket = (EthernetPacket) packet;
                                Packet payload = ethPacket.getPayload();

                                if (payload instanceof IpV4Packet) {
                                    IpV4Packet ipPacket = (IpV4Packet) payload;
                                    IpV4Packet.IpV4Header header = ipPacket.getHeader();
                                    Inet4Address srcAddr = header.getSrcAddr();
                                    Inet4Address dstAddr = header.getDstAddr();
                                    System.out.println("Source IP: " + srcAddr);
                                    System.out.println("Destination IP: " + dstAddr);
                                }
                            }
                        }
                    });
                } catch (PcapNativeException | NotOpenException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public static void findLoopBack() {
        List<PcapNetworkInterface> allDevs = null;
        try {
            allDevs = Pcaps.findAllDevs();
        }
        catch (PcapNativeException e) {
            e.printStackTrace();
        }

        for (PcapNetworkInterface device : allDevs) {
            if (device.isLoopBack()) {
                System.out.println("Found loopback adapter: " + device.getName());
                // You can start capturing packets from the loopback adapter here
            }
        }
    }

    public static void main(String args[]) {
        findLoopBack();
        start();
    }
}
