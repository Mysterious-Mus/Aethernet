package com.aethernet.UI;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;

import com.aethernet.Main;
import com.aethernet.ASIO.ASIOHost;
import com.aethernet.config.L2Config;
import com.aethernet.physical.PhysicalManager;
import com.aethernet.physical.transmit.SoF;

import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

public class L2Host extends JFrame{

    public static class Configs {
        public static int UIHeight = 600;
        public static int UIWidth = 1300;
    }

    // panel collectors
    public static ArrayList<JPanel> channelSelectPanels = new ArrayList<JPanel>();
    public static ArrayList<JPanel> appCtrls = new ArrayList<JPanel>();
    
    public L2Host() {
        super("Acoustic NFC");
        setCloseOp();
        layoutPanel();
    }

    private void setCloseOp() {
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    private void layoutPanel() {
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.gridy = 0;

        gbc.gridy = 1;
        for (JPanel panel : appCtrls) {
            gbc.gridx++; this.add(panel, gbc);
        }
        gbc.gridy = 0; gbc.gridwidth = gbc.gridx + 1; gbc.gridx = 0; this.add(new ChannelPanel(), gbc);
        gbc.gridy = 2; this.add(SoF.detectorPanel, gbc);
        gbc.gridy = 3; this.add(Main.allCtrlPanel, gbc);
        gbc.gridy = 4; this.add(PhysicalManager.channelEnergyPanel, gbc);
        
        gbc.gridheight = gbc.gridy + 1; 
        gbc.gridy = 0; gbc.gridx = gbc.gridwidth; gbc.gridwidth = 1; this.add(L2Config.panel, gbc);

        this.setSize(Configs.UIWidth, Configs.UIHeight);
        this.setResizable(false);
        // this.setVisible(true);
    }

    private class ChannelPanel extends JPanel{

        public ChannelPanel() {
            super();
            
            setLayout(new GridBagLayout());
            // add open control panel button
            JButton openControlPanelButton = new JButton("Open Control Panel");
            openControlPanelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ASIOHost.openControlPanel();
                }
            });
            // give it an independent panel to adapt size
            JPanel openControlPanelJPanel = new JPanel(new BorderLayout());
            openControlPanelJPanel.add(openControlPanelButton, BorderLayout.CENTER);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 0;
            add(openControlPanelJPanel, gbc);
            // add channel selection panels
            // chanSelCol.add(Main.physicalManager.getChannelSelectPanel());
            for (JPanel panel : channelSelectPanels) {
                gbc.gridy += 1;
                add(panel, gbc);
            }
            // display the config: channel multi assign
            JPanel allowMultiAssignPanel = new JPanel(new GridLayout(0, 2));
            allowMultiAssignPanel.add(new JLabel("Allow Channel Multiple Assign(need reboot)"));
            allowMultiAssignPanel.add(ASIOHost.Configs.allowChannelMultiAssign.displayer());

            gbc.gridy += 1;
            add(allowMultiAssignPanel, gbc);
        }
    }
}
