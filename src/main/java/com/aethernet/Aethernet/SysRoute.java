package com.aethernet.Aethernet;

import java.util.List;

import com.aethernet.utils.CyclicBuffer;
import com.aethernet.utils.TypeConvertion;
import com.aethernet.Main;
import com.aethernet.Aethernet.utils.IPAddr;
import com.aethernet.Aethernet.utils.PacketResolve;
import com.aethernet.Aethernet.utils.PacketCreate;
import com.aethernet.Aethernet.utils.RoutingTableEntry;
import com.aethernet.Aethernet.utils.WhiteList;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.LinkLayerAddress;
import org.pcap4j.util.MacAddress;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;

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
    public static LinkLayerAddress internetMAC;
    public static WhiteList<String> handleDns = new WhiteList<String>(new String[] {"example.com"});
    public static WhiteList<Inet4Address> handleTcp = new WhiteList<Inet4Address>(
        new Inet4Address[] {IPAddr.buildV4FromStr("93.184.216.34")});
    public static int initTcpSeqNum = 0x21345768;
    public static boolean seqnumShiftSet = false;
    public static int tcpSeqNumShift;
    
    public static CyclicBuffer<Packet> buffer = new CyclicBuffer<Packet>(1000);

    public static void forward2Internet(Packet packet) {
        if (PacketResolve.hasPort(packet)) {
            int port = PacketResolve.getSrcPort(packet);
            try {
                // Create a server socket and bind it to the specified port
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Listening on port " + port);
            } catch (IOException e) {
                System.out.println("Could not listen on port " + port);
            }
        }

        try {
            internetHandle.sendPacket(packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void adapterReceiveHandler(PcapNetworkInterface nif, Packet packet) {
        if (AetherRoute.asGateway.v()) {
            // if the packet in the internet device is toward athernet
            // scenario: using node 3 to ping node 1
            // otherwise the dst of the packet should be the internet ip of node2
            if (aetherSubnet.matches(packet)) {
                System.out.println(nif.getDescription());
                AetherRoute.deliver(packet);
            }
            // if the packet is an icmp reply to internet IP and has payload agent magic
            // if (PacketResolve.isAethernetAgent(packet) && PacketResolve.isReplyingMe(packet, internetIP)) {
            if (PacketResolve.isPingReplyingMe(packet, internetIP) && PacketResolve.isReplyingAethernetAgent(packet)) {
                Inet4Address newDst =
                    aetherSubnet.hostId2Address(
                        TypeConvertion.short2byte(
                            PacketResolve.getIcmpId(packet)
                        )
                    );
                Packet packet4Aeth = 
                    PacketCreate.changeDstIp(
                        (EthernetPacket) packet,
                        newDst 
                    );
                packet4Aeth =
                    PacketCreate.changeIcmpEchoReplyId(
                        (EthernetPacket) packet4Aeth,
                        (short) 0x0001 // echo from cmd ping always have 0x0001 as id
                    );
                AetherRoute.deliver(packet4Aeth);
            }
            // we have to deliver all the dns replys to node1
            if (PacketResolve.isDnsReply(packet)) {
                Packet packet4Aeth = PacketCreate.changeDstIp((EthernetPacket) packet, AetherRoute.node1IP);
                packet4Aeth = PacketCreate.changeSrcIp((EthernetPacket)packet4Aeth, AetherRoute.gatewayIP);
                packet4Aeth = PacketCreate.correctIpV4Checksum((EthernetPacket) packet4Aeth);
                packet4Aeth = PacketCreate.correctUDPCheckSum((EthernetPacket) packet4Aeth);
                System.out.println("DNS reply to node1 " + PacketResolve.getDstIP(packet4Aeth));
                AetherRoute.deliver(packet4Aeth);
            }
            // deliver TCP replys to node1
            if (PacketResolve.isTcpReplyPacket(packet)) {
                EthernetPacket packet4Aeth = PacketCreate.changeDstIp((EthernetPacket) packet, AetherRoute.node1IP);
                packet4Aeth = PacketCreate.correctIpV4Checksum((EthernetPacket) packet4Aeth);
                packet4Aeth = PacketCreate.correctTCPCheckSum((EthernetPacket) packet4Aeth);
                AetherRoute.deliver(packet4Aeth);
            }
        }
        else {
            // if I'm a host, not the gateway
            // I may raise icmp ping or dns query
            if ((PacketResolve.isIcmpPing(packet) || PacketResolve.isDnsQuery(packet))
                && PacketResolve.getSrcIP(packet).equals(internetIP)) 
            {
                // get a ping request or dns query to someone in the cmd 
                AetherRoute.deliver(
                    PacketCreate.changeSrcIp((EthernetPacket) packet, AetherRoute.me.ipAddr.v())
                );
            }
            // I may have tcp packets to transmit
            if (PacketResolve.isTcpRequestPacket(packet)) {
                // change src ip and seqnum, then forward to gateway
                EthernetPacket packet4Aeth = PacketCreate.changeSrcIp((EthernetPacket) packet, AetherRoute.me.ipAddr.v());
                if (!seqnumShiftSet) {
                    seqnumShiftSet = true;
                    tcpSeqNumShift = initTcpSeqNum - PacketResolve.getTcpSequenceNumber(packet);
                }
                int newSeqnum = PacketResolve.getTcpSequenceNumber(packet) + tcpSeqNumShift;
                packet4Aeth = PacketCreate.changeTcpSequenceNumber(packet4Aeth, newSeqnum);
                packet4Aeth = PacketCreate.correctIpV4Checksum(packet4Aeth);
                packet4Aeth = PacketCreate.correctTCPCheckSum(packet4Aeth);
                AetherRoute.deliver(packet4Aeth);
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
        aetherSubnet = new RoutingTableEntry("172.18.5.0", "255.255.255.0");

        List<PcapNetworkInterface> allDevs = null;
        try {
            allDevs = Pcaps.findAllDevs();
        }
        catch (PcapNativeException e) {
            e.printStackTrace();
        }

        for (PcapNetworkInterface device : allDevs) {
            // if (!device.getDescription().startsWith("Microsoft KM-TEST")) {
            //     new adapterListenerThread(device).start();
            // }
            if (device.getDescription().startsWith("Intel(R) Wi-Fi")) {
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

                    internetMAC = device.getLinkLayerAddresses().get(0);
                    System.out.println(internetMAC);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    // public static void main(String[] args) {
    //     System.out.println(IPAddr.buildV4FromStr("93.184.216.34").equals(IPAddr.buildV4FromStr("93.184.216.35")));
    // }
}
