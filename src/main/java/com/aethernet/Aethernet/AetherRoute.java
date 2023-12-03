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

public class AetherRoute {

    public static PcapNetworkInterface AethernetAdapter;
    public static PcapHandle AethernetHandle;

    static MacManager macManager;

    public static void send(Packet packet) {
        System.out.println("a packet delivered into Aethernet");

        // feed into Aethernet adapter
        try {
            AethernetHandle.sendPacket(packet);
        }
        catch (PcapNativeException | NotOpenException e) {
            e.printStackTrace();
        }
    }
    
    static FrameReceivedListener frameReceivedListener = new FrameReceivedListener() {
        @Override
        public void frameReceived(MacFrame packet) {
            System.out.println("Aethernet got a packet");
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

        // macManager = new MacManager((byte) 0xff, "AetherRoute", frameReceivedListener);
    }
}
