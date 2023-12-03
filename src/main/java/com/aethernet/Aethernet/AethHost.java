package com.aethernet.Aethernet;

import javax.swing.JPanel;
import java.awt.*;
import javax.swing.*;

import com.aethernet.UI.EthHost;
import com.aethernet.config.EthConfig.ConfigTerm;
import com.aethernet.config.utils.ConfigTermTemplate;

import org.pcap4j.packet.Packet;

public class AethHost {
    
    String name;

    public ConfigTermTemplate<String> ipAddr;
    public ConfigTermTemplate<Byte> macAddr;

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

    public AethHost(String hostName) {
        this.name = hostName;
        ipAddr = new ConfigTerm<String>(hostName + ".ipAddr", "172.0.0.1", false);
        macAddr = new ConfigTerm<Byte>(hostName + ".macAddr", (byte) 1, false);

        controlPanel = new ControlPanel();
        EthHost.controlPanels.add(controlPanel);
    }

    public void pingMeHandler(Packet packet) {
        // print message
        System.out.println("IPHost: " + name + " got a ping request");
    }
}
