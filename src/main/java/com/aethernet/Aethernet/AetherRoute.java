package com.aethernet.Aethernet;

import java.util.List;
import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;

import com.aethernet.Aethernet.SysRoute.adapterListenerThread;
import com.aethernet.Aethernet.utils.PacketResolve;
import com.aethernet.mac.MacFrame;
import com.aethernet.mac.MacManager;
import com.aethernet.Aethernet.SysRoute.Configs;
import com.aethernet.mac.MacManager;
import com.aethernet.mac.MacManager.FrameReceivedListener;

/**
 * Since each Aethernet host may start a ping from its cmd,
 * they each should have a static Aethernet router to route
 * packets from the adapter the system uses to the Aethernet
 */
public class AetherRoute {

    public static PcapNetworkInterface AethernetAdapter;
    public static PcapHandle AethernetHandle;

    static Byte Mac = (byte) 0xff;
    static AethHost me;
    static ARPTable arpTable;

    /**
     * reply packets should be delivered to the same adapter
     */
    static Map<Inet4Address, PcapHandle> replyMap = new HashMap<Inet4Address, PcapHandle>();

    /**
     * deliver a packet into Aethernet
     * also remembering the handle to reply to the adapter
     * @param srcHandle
     * @param packet
     */
    public static void deliver(PcapHandle srcHandle, Packet packet) {
        System.out.println("a packet delivered into Aethernet");

        replyMap.put(PacketResolve.getSrcIP(packet), srcHandle);

        // feed into Aethernet adapter, then wireshark can monitor the traffic
        try {
            AethernetHandle.sendPacket(packet);
        }
        catch (PcapNativeException | NotOpenException e) {
            e.printStackTrace();
        }

        // send it into Aethernet
        // arp resolve
        Byte dstMac = arpTable.query(packet);
        if (dstMac == null) {
            System.out.println("Aethernet router: arp not found");
            return;
        }
        System.out.println("sending to " + dstMac);
        me.macManager.sendNoWait(dstMac, packet.getRawData());
    }

    /**
     * if an aether host receives a packet from aethernet,
     * it should call this function to feed the packet into the adapter
     */
    public static void receive(Packet packet) {
        try {
            AethernetHandle.sendPacket(packet);
        }
        catch (PcapNativeException | NotOpenException e) {
            e.printStackTrace();
        }

        PcapHandle replyHandle = replyMap.get(PacketResolve.getDstIP(packet));
        if (replyHandle != null)
            try {
            replyHandle.sendPacket(packet);
            }
            catch (PcapNativeException | NotOpenException e) {
                e.printStackTrace();
            }
        else if (!SysRoute.aetherSubnet.matches(packet)) {
            // the packet is neither a reply to outer nor a request to aether subnet
            // TODO: 
        }
    }

    /**
     * if an aether host sends a packet to aethernet, (raised by itself, can only be replying),
     * it should call this function to feed the packet into the adapter
     */
    public static void replyReport(Packet packet) {
        try {
            AethernetHandle.sendPacket(packet);
        }
        catch (PcapNativeException | NotOpenException e) {
            e.printStackTrace();
        }
    }

    /**
     * find the Aethernet adapter and open a handle for it
     * open a MacManager to forward packets 
     */
    public static void init(AethHost hostAssigned) {
        me = hostAssigned;

        List<PcapNetworkInterface> allDevs = null;
        try {
            allDevs = Pcaps.findAllDevs();
        }
        catch (PcapNativeException e) {
            e.printStackTrace();
        }
        for (PcapNetworkInterface device : allDevs) {
            if (device.getDescription().startsWith("Microsoft KM-TEST")) {
                AethernetAdapter = device;
                try {
                    AethernetHandle = AethernetAdapter.openLive(
                        Configs.SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, Configs.READ_TIMEOUT);
                }
                catch (PcapNativeException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        arpTable = new ARPTable();
    }
}
