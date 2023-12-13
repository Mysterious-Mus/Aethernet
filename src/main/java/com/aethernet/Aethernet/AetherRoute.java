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
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.EthernetPacket.EthernetHeader;

import com.aethernet.Aethernet.SysRoute.adapterListenerThread;
import com.aethernet.Aethernet.utils.PacketResolve;
import com.aethernet.config.EthConfig.ConfigTerm;
import com.aethernet.mac.MacFrame;
import com.aethernet.mac.MacManager;
import com.aethernet.Aethernet.SysRoute.Configs;
import com.aethernet.mac.MacManager;
import com.aethernet.mac.MacManager.FrameReceivedListener;
import com.aethernet.utils.TypeConvertion;
import com.aethernet.Aethernet.utils.IPAddr;
import com.aethernet.Aethernet.utils.PacketCreate;

/**
 * Since each Aethernet host may start a ping from its cmd,
 * they each should have a static Aethernet router to route
 * packets from the adapter the system uses to the Aethernet
 */
public class AetherRoute {

    public static String internetAgentMagic = "I'm delivering for Athernet!";

    public static ConfigTerm<Boolean> asGateway;
    
    // hardcode
    public static Inet4Address gatewayIP = IPAddr.buildV4FromStr("172.0.0.2");

    public static PcapNetworkInterface AethernetAdapter;
    public static PcapHandle AethernetHandle;

    static Byte Mac = (byte) 0xff;
    static AethHost me;

    /**
     * deliver a packet into Aethernet
     * also remembering the handle to reply to the adapter
     * @param srcHandle
     * @param packet
     */
    public static void deliver(Packet packet) {
        // if src is my internet IP, then translate into my aethernet ip
        if (PacketResolve.getSrcIP(packet).equals(SysRoute.internetIP)) {
            packet = PacketCreate.changeSrcIp((EthernetPacket) packet, me.ipAddr.v());
        }
        // feed into Aethernet adapter, then wireshark can monitor the traffic
        try {
            AethernetHandle.sendPacket(packet);
        }
        catch (PcapNativeException | NotOpenException e) {
            e.printStackTrace();
        }

        // send it into Aethernet
        Inet4Address dstIp = SysRoute.aetherSubnet.matches(packet) ? PacketResolve.getDstIP(packet) : gatewayIP;
        
        // The packet forward to internet 
        if(asGateway.v() && dstIp == gatewayIP) 
            return;

        Byte dstMac = me.arpTable.query(dstIp);

        // arp resolve
        while (dstMac == null) {
            Packet arpRequest = PacketCreate.createArpRequest(me.macAddr.v(), IPAddr.buildV4FromStr(me.ipAddr.v()), dstIp);
            try {
                AethernetHandle.sendPacket(arpRequest);
            }
            catch (PcapNativeException | NotOpenException e) {
                e.printStackTrace();
            }
            me.macManager.sendNoWait(MacFrame.Configs.broadcastAddr, arpRequest.getRawData());
            me.arpReply.waitTillPermitted();
            me.arpReply.unpermit();
            dstMac = me.arpTable.query(dstIp);
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

        if (PacketResolve.getDstIP(packet).equals(
            IPAddr.buildV4FromStr(me.ipAddr.v())))
        {
            // I get an Aethernet packet to me. Maybe I should feed it into
            // internet handle to respond system ack or so
            if (PacketResolve.isReplyingMe(packet, IPAddr.buildV4FromStr(me.ipAddr.v())))
                SysRoute.forward2Internet(
                    PacketCreate.changeDstMac(
                        PacketCreate.correctIpV4Checksum(
                            PacketCreate.changeDstIp((EthernetPacket) packet, SysRoute.internetIP)
                        ),
                        SysRoute.internetMAC
                    )   
                );
        }
        else if (!SysRoute.aetherSubnet.matches(packet)) {
            // the packet is neither a reply to outer nor a request to aether subnet
            // might be a packet from a non-gateway host to ping outside
            if (asGateway.v()) {
                // if the Aethernet host want to ping outside
                if (PacketResolve.isIcmpPing(packet)) {
                    // change the src to internet ip
                    Packet agentPacket = 
                        PacketCreate.changeIcmpPingId(
                            (EthernetPacket) packet,
                            TypeConvertion.unsignedByteToShort(
                                SysRoute.aetherSubnet.getHostId(PacketResolve.getSrcIP(packet))
                            )
                        );
                    // System.out.println("Step1:");
                    // System.out.println(agentPacket);
                    agentPacket = 
                    PacketCreate.changeSrcIp(
                        (EthernetPacket) agentPacket, SysRoute.internetIP
                        );
                    // System.out.println("Step2:");
                    // System.out.println(agentPacket);
                    // agentPacket = 
                    // PacketCreate.changeIcmpPingPayload(
                    //     (EthernetPacket) agentPacket,
                    //     internetAgentMagic
                    //     );
                    agentPacket = 
                    PacketCreate.changeSrcMac(
                        (EthernetPacket) agentPacket,
                        SysRoute.internetMAC
                        );
                    agentPacket =
                    PacketCreate.correctIpV4Checksum((EthernetPacket)agentPacket);
                    // System.out.println("Step3:");
                    // System.out.println(agentPacket);
                    SysRoute.forward2Internet(agentPacket);
                }
            }
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
        asGateway = new ConfigTerm<Boolean>("asGateway", true, false) {
            @Override
            public void newvalOp(Boolean newv) {
                if (newv) {
                    System.out.println("The current host is the gateway, check address!");
                }
            }
        };

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
    }
}
