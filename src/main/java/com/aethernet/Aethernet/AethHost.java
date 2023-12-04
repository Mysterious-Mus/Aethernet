package com.aethernet.Aethernet;

import javax.swing.JPanel;
import java.awt.*;
import javax.swing.*;

import com.aethernet.UI.EthHost;
import com.aethernet.config.EthConfig.ConfigTerm;
import com.aethernet.config.utils.ConfigTermTemplate;
import com.aethernet.mac.MacFrame;
import com.aethernet.mac.MacManager;
import com.aethernet.mac.MacManager.FrameReceivedListener;

import org.pcap4j.packet.Packet;

public class AethHost {
    
    String name;

    public ConfigTerm<String> ipAddr;
    public ConfigTerm<Byte> macAddr;

    MacManager macManager;

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
        public void frameReceived(MacFrame packet) {
            System.out.println("IPHost: " + name + " got a packet");
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
        EthHost.controlPanels.add(controlPanel);

        macManager = new MacManager(macAddr.v(), name, frameReceivedListener);
    }

    public void pingMeHandler(Packet packet) {
        // print message
        System.out.println("IPHost: " + name + " got a ping request");
    }
}
