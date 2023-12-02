package com.aethernet.Aethernet;

import javax.swing.JPanel;
import java.awt.*;
import javax.swing.*;

import com.aethernet.UI.EthHost;
import com.aethernet.config.EthConfig.ConfigTerm;
import com.aethernet.config.utils.ConfigTermTemplate;

public class IPHost {
    
    String name;

    public ConfigTermTemplate<String> ipAddr;

    public class ControlPanel extends JPanel {
        public ControlPanel() {
            this.setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.gridy = 0;

            // title
            gbc.gridwidth = 2; this.add(new JLabel(name), gbc);

            // address
            gbc.gridwidth = 1;
            gbc.gridy = 1; this.add(new JLabel("IP Address: "), gbc);
            gbc.gridx = 1; this.add(ipAddr.displayer(), gbc);
        }
    }
    public ControlPanel controlPanel;

    public IPHost(String hostName) {
        this.name = hostName;
        ipAddr = new ConfigTerm<String>(hostName + ".ipAddr", "172.0.0.1", false);

        controlPanel = new ControlPanel();
        EthHost.controlPanels.add(controlPanel);
    }
}
