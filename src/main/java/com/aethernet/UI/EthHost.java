package com.aethernet.UI;

import com.aethernet.UI.L2Host;
import com.aethernet.config.EthConfig;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

public class EthHost extends JFrame{
    
    L2Host l2Host;
    public static ArrayList<JPanel> controlPanels = new ArrayList<JPanel>();

    private class ShowL2Button extends JPanel {
        public ShowL2Button() {
            super(new BorderLayout());

            JButton button = new JButton("Show L2");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    l2Host.setVisible(true);
                }
            });

            this.add(button, BorderLayout.CENTER);
        }
    }

    private void layoutPanel() {
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.gridy = 0;

        gbc.gridy = 0; this.add(new ShowL2Button(), gbc);

        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc_ctp = new GridBagConstraints();
        gbc_ctp.fill = GridBagConstraints.HORIZONTAL; gbc_ctp.gridx = 0; gbc_ctp.gridy = 0;
        for (JPanel panel : controlPanels) {
            gbc_ctp.gridx++; controlPanel.add(panel, gbc_ctp);
        }

        gbc.gridy = 1; this.add(controlPanel, gbc);

        gbc.gridy = 2; this.add(EthConfig.panel, gbc);

        this.setSize(600, 200);
        this.setResizable(false);
        this.setVisible(true);
    }

    public EthHost() {
        l2Host = new L2Host();
        layoutPanel();
    }

}
