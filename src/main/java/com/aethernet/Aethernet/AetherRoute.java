package com.aethernet.Aethernet;

import java.util.List;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;

import com.aethernet.Aethernet.SysRoute.adapterListenerThread;
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
    static MacManager macManager;
    static ARPTable arpTable;

    public static void send(Packet packet) {
        System.out.println("a packet delivered into Aethernet");

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
        macManager.sendParallel(dstMac, packet.getRawData());
    }
    
    static FrameReceivedListener frameReceivedListener = new FrameReceivedListener() {
        @Override
        public void frameReceived(MacFrame packet) {
            System.out.println("Aethernet router got a packet");
        }
    };

    /**
     * find the Aethernet adapter and open a handle for it
     * open a MacManager to forward packets 
     */
    public static void init() {
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
        macManager = new MacManager(Mac, "AetherRoute", frameReceivedListener);
    }
}
