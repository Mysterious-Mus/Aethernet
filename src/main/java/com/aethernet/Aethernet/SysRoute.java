package com.aethernet.Aethernet;

import java.util.List;

import com.aethernet.utils.CyclicBuffer;
import com.aethernet.utils.TypeConvertion;
import com.aethernet.Main;
import com.aethernet.Aethernet.utils.IPAddr;
import com.aethernet.Aethernet.utils.PacketResolve;
import com.aethernet.Aethernet.utils.PacketCreate;
import com.aethernet.Aethernet.utils.RoutingTableEntry;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.MacAddress;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import java.net.Inet4Address;
import java.net.InetAddress;

import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;

/**
 * this class is used to capture all system packages,
 * see if it is pinging into the aethernet subnet.
 * if it is, direct it into the aethernet adapter.
 * 
 * it also should direct icmp packets from aethernet adapter
 * since the system will only accept ping reply from the adapter sending the ping request,
 * SysNetInterface should also direct icmp packets from aethernet adapter to the correct adapter.
 */
public class SysRoute {

    public static RoutingTableEntry aetherSubnet;

    public static class Configs {
        public static int SNAPLEN = 65536;
        public static int READ_TIMEOUT = 10;
    }

    public static PcapHandle internetHandle;
    public static Inet4Address internetIP;
    
    public static CyclicBuffer<Packet> buffer = new CyclicBuffer<Packet>(1000);

    public static void forward2Internet(Packet packet) {
        try {
            internetHandle.sendPacket(packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void adapterReceiveHandler(PcapNetworkInterface nif, Packet packet) {
        if (!PacketResolve.isIcmpPing(packet) && !PacketResolve.isIcmpReply(packet)) return;
        
        if (AetherRoute.asGateway.v()) {
            // if the packet in the internet device is toward athernet
            if (aetherSubnet.matches(packet)) AetherRoute.deliver(packet);
            // if the packet is an icmp reply to internet IP and has payload agent magic
            // if (PacketResolve.isAethernetAgent(packet) && PacketResolve.isReplyingMe(packet, internetIP)) {
            if (PacketResolve.isReplyingMe(packet, internetIP)) {
                Packet packet4Aeth = 
                    PacketCreate.changeIcmpPingId(
                        PacketCreate.changeDstIp(
                            (EthernetPacket) packet,
                            aetherSubnet.hostId2Address(
                                TypeConvertion.short2byte(
                                    PacketResolve.getIcmpId(packet)
                                )
                            )  
                        ),
                        (short) 0x0001 // echo from cmd ping always have 0x0001 as id
                    );
                AetherRoute.deliver(packet);
            }
        }
        else {
            // if I'm a host, not the gateway
            if (PacketResolve.isIcmpPing(packet) 
                && PacketResolve.getSrcIP(packet).equals(internetIP)) 
            {
                // get a ping request to someone in the cmd
                AetherRoute.deliver(
                    PacketCreate.changeSrcIp((EthernetPacket) packet, AetherRoute.gatewayIP)
                );
            }
        }
    }

    static class adapterListenerThread extends Thread {
        PcapNetworkInterface device;
        PcapHandle handle;
        public adapterListenerThread(PcapNetworkInterface device) {
            this.device = device;
        }

        @Override
        public void run() {
            try {
                handle = device.openLive(
                    Configs.SNAPLEN, PromiscuousMode.PROMISCUOUS, Configs.READ_TIMEOUT);
                
                handle.loop(-1, new PacketListener() {
                    @Override
                    public void gotPacket(Packet packet) {
                        adapterReceiveHandler(device, packet);
                    }
                });
            } catch (PcapNativeException | NotOpenException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * start capturing all packets from all adapters except Aethernet
     * ping request to aethernet may come from any adapter
     */
    public static void start() {
        aetherSubnet = new RoutingTableEntry("172.0.0.0", "255.255.255.0");

        List<PcapNetworkInterface> allDevs = null;
        try {
            allDevs = Pcaps.findAllDevs();
        }
        catch (PcapNativeException e) {
            e.printStackTrace();
        }

        for (PcapNetworkInterface device : allDevs) {
            if (!device.getDescription().startsWith("Microsoft KM-TEST")) {
                new adapterListenerThread(device).start();
            }

            if (device.getDescription().startsWith("Intel(R) Wi-Fi")) {
                try {
                    internetHandle = device.openLive(
                        Configs.SNAPLEN, PromiscuousMode.PROMISCUOUS, Configs.READ_TIMEOUT);
                    for (PcapAddress addr : device.getAddresses()) {
                        if (addr.getAddress() instanceof Inet4Address) {
                            internetIP = (Inet4Address) addr.getAddress();
                        }
                    }
                    System.out.println("Internet IP: " + internetIP);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
