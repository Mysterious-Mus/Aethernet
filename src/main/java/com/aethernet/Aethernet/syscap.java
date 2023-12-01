package com.aethernet.Aethernet;

import java.util.List;

import com.aethernet.utils.CyclicBuffer;
import com.aethernet.Aethernet.utils.IPAddr;
import com.aethernet.Aethernet.utils.PacketResolve;
import com.aethernet.Aethernet.utils.PacketCreate;

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

    public static PcapHandle loopBackAdapter = null;

    public static Inet4Address ipAddr = IPAddr.buildV4FromStr("192.168.111.10");
    // public static Inet4Address ipAddr = IPAddr.buildV4FromStr("1.1.1.1");

    PacketListener gotPackAction = new PacketListener() {
        @Override
        public void gotPacket(Packet packet) {
        
        }
    };
    
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
                            if (PacketResolve.isPingingMe(packet, ipAddr)) {
                                System.out.println(packet);
                                // Create ICMPv4 echo reply
                                EthernetPacket replyPacket = 
                                    PacketCreate.createReplyPacket((EthernetPacket)packet);
                                // print reply packet
                                System.out.println(replyPacket);
                                try {
                                    loopBackAdapter.sendPacket(replyPacket);
                                }
                                catch (PcapNativeException | NotOpenException e) {
                                    e.printStackTrace();
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
        } catch (PcapNativeException e) {
            e.printStackTrace();
        }

        for (PcapNetworkInterface nif : allDevs) {
            System.out.println(nif.getName() + " (" + nif.getDescription() + ")");
        }

        String adapterDescriptionPreffix = "Intel(R) Wi-Fi";
        // String adapterDescriptionPreffix = "Microsoft KM-TEST";
        PcapNetworkInterface nif = allDevs.stream()
            .filter(dev -> dev.getDescription() != null && dev.getDescription().startsWith(adapterDescriptionPreffix))
            .findFirst()
            .orElse(null);

        try {
            loopBackAdapter = nif.openLive(
                Configs.SNAPLEN, PromiscuousMode.PROMISCUOUS, Configs.READ_TIMEOUT);
        }
        catch (PcapNativeException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        findLoopBack();
        start();
    }
}
