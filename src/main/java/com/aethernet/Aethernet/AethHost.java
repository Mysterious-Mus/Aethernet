package com.aethernet.Aethernet;

import java.awt.*;
import javax.swing.*;

import com.aethernet.Aethernet.utils.IPAddr;
import com.aethernet.Aethernet.utils.PacketCreate;
import com.aethernet.Aethernet.utils.PacketResolve;
import com.aethernet.UI.EthUIHost;
import com.aethernet.config.EthConfig.ConfigTerm;
import com.aethernet.mac.MacFrame;
import com.aethernet.mac.MacManager;
import com.aethernet.mac.MacFrame.Configs.HeaderFields;
import com.aethernet.mac.MacManager.FrameReceivedListener;
import com.aethernet.utils.sync.Permission;

import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.IcmpV4DestinationUnreachablePacket.IcmpV4DestinationUnreachableHeader;
import org.pcap4j.packet.factory.PacketFactories;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IcmpV4Code;
import org.pcap4j.packet.namednumber.NotApplicable;

public class AethHost {
    
    String name;

    public ConfigTerm<String> ipAddr;
    public ConfigTerm<Byte> macAddr;

    public MacManager macManager;
    
    public ARPTable arpTable;
    // sync
    public Permission arpReply = new Permission(false);

    public class ControlPanel extends JPanel {
        public ControlPanel() {
            this.setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.gridy = 0;

            // title
            gbc.gridwidth = 2; this.add(new JLabel(name), gbc);

            // IP
            gbc.gridwidth = 1;
            gbc.gridy = 1; this.add(new JLabel("IP Address: "), gbc);
            gbc.gridx = 1; this.add(ipAddr.displayer(), gbc);

            // MAC
            gbc.gridy = 2; gbc.gridx = 0; this.add(new JLabel("MAC Address: "), gbc);
            gbc.gridx = 1; this.add(macAddr.displayer(), gbc);
        }
    }
    public ControlPanel controlPanel;

    FrameReceivedListener frameReceivedListener = new FrameReceivedListener() {
        @Override
        public void frameReceived(MacFrame macPacket) {
            byte[] data = macPacket.getData(); // your byte array
            Packet packet = PacketResolve.byteArr2Packet(data);
            AetherRoute.receive(packet);

            // reply if ping me
            if (PacketResolve.isPingingMe(packet, IPAddr.buildV4FromStr(ipAddr.v()))) {
                Packet replyPacket = PacketCreate.createReplyPacket((EthernetPacket) packet);
                macManager.sendNoWait(
                    macPacket.getHeader().getField(HeaderFields.SRC_ADDR), 
                    replyPacket.getRawData());
                AetherRoute.replyReport(replyPacket);
            }

            // reply if received an arp request
            else if (PacketResolve.isArpRequestme(packet, IPAddr.buildV4FromStr(ipAddr.v()))) {
                System.out.println(ipAddr.v()+ " ARP request received");
                // Update the arp table by the way 
                arpTable.addEntry(PacketResolve.getSrcIP(packet), PacketResolve.getMacSrcAddr(packet));
                Packet replyPacket = PacketCreate.createArpReply((EthernetPacket) packet, macAddr.v());
                macManager.sendNoWait(
                    macPacket.getHeader().getField(HeaderFields.SRC_ADDR), 
                    replyPacket.getRawData());
                AetherRoute.replyReport(replyPacket);
            }

            // Update the arp table if the reply received 
            else if(PacketResolve.isArpReplytme(packet, IPAddr.buildV4FromStr(ipAddr.v()))) {
                System.out.println(ipAddr.v() + " ARP reply received");
                System.out.println("src ip: " + PacketResolve.getSrcIP(packet));
                System.out.println("src mac: " + PacketResolve.getMacSrcAddr(packet));
                arpTable.addEntry(PacketResolve.getSrcIP(packet), PacketResolve.getMacSrcAddr(packet));
                arpReply.permit();
            }

        }
    };

    public AethHost(String hostName) {
        this.name = hostName;
        ipAddr = new ConfigTerm<String>(hostName + ".ipAddr", "172.0.0.1", false);
        macAddr = new ConfigTerm<Byte>(hostName + ".macAddr", (byte) 1, false) {
            @Override
            public void newvalOp(Byte newVal) {
                macManager.syncAddr(newVal);
            }
        };

        controlPanel = new ControlPanel();
        EthUIHost.controlPanels.add(controlPanel);

        macManager = new MacManager(macAddr.v(), name, frameReceivedListener);

        arpTable = new ARPTable();
    }

    public void pingMeHandler(Packet packet) {
        // print message
        System.out.println("IPHost: " + name + " got a ping request");
    }
}
