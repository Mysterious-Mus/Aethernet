package com.aethernet.Aethernet;

import java.util.List;

import com.aethernet.utils.CyclicBuffer;
import com.aethernet.Main;
import com.aethernet.Aethernet.utils.IPAddr;
import com.aethernet.Aethernet.utils.PacketResolve;
import com.aethernet.Aethernet.utils.PacketCreate;
import com.aethernet.Aethernet.utils.RoutingTableEntry;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.MacAddress;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import java.net.Inet4Address;
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
    
    public static CyclicBuffer<Packet> buffer = new CyclicBuffer<Packet>(1000);

    public static Inet4Address ipAddr = IPAddr.buildV4FromStr("192.168.111.10");
    // public static Inet4Address ipAddr = IPAddr.buildV4FromStr("1.1.1.1");

    private static void adapterReceiveHandler(PcapHandle handle, Packet packet) {
        if (!PacketResolve.isIcmp(packet)) return;

        if (AetherRoute.asGateway.v()) {
            if (aetherSubnet.matches(packet)) AetherRoute.deliver(handle, packet);
        }
        else {
            AetherRoute.deliver(handle, packet);
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
                        adapterReceiveHandler(handle, packet);
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
                    PcapHandle handle = device.openLive(
                        Configs.SNAPLEN, PromiscuousMode.PROMISCUOUS, Configs.READ_TIMEOUT);
                    handle.sendPacket(PacketCreate.createPingPacket(
                        IPAddr.buildV4FromStr("10.20.225.10"), IPAddr.buildV4FromStr("1.1.1.1"),
                        (MacAddress) device.getLinkLayerAddresses().get(0)
                    ));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
    }
}
