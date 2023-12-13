package com.aethernet;

import java.util.ArrayList;

import javax.swing.JPanel;

import com.aethernet.APP.TxRx;
import com.aethernet.ASIO.ASIOHost;
import com.aethernet.Aethernet.ARPTable;
import com.aethernet.Aethernet.AetherRoute;
import com.aethernet.Aethernet.AethHost;
import com.aethernet.Aethernet.SysRoute;
import com.aethernet.UI.EthUIHost;
import com.aethernet.config.EthConfig;
import com.aethernet.config.L2Config;
import com.aethernet.mac.MacManager;
import com.aethernet.physical.PhysicalManager;
import com.aethernet.utils.AddrAlloc;
import com.aethernet.utils.BitString;

import javax.swing.BoxLayout;
import javax.swing.JButton;

public class Main {
    
    L2Config l2config;
    EthConfig ethConfig;

    public static AddrAlloc addrAlloc = new AddrAlloc();

    ASIOHost asioHost;
    EthUIHost ethHost;
    public static ArrayList<TxRx> txrxApps = new ArrayList<TxRx>();
    AethHost me, far;

    public static void AllStop() {
        for (TxRx txrx : txrxApps) {
            txrx.stop();
        }
    }

    public static void AllReceive() {
        for (TxRx txrx : txrxApps) {
            txrx.receive();
        }
    }

    public static void AllTransmit() {
        for (TxRx txrx : txrxApps) {
            txrx.transmit();
        }
    }

    public static class AllCtrlPanel extends JPanel {
        public AllCtrlPanel() {
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            JButton allStop = new JButton("All Stop");
            allStop.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    AllStop();
                }
            });
            JButton allReceive = new JButton("All Receive");
            allReceive.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    AllReceive();
                }
            });
            JButton allTransmit = new JButton("All Transmit");
            allTransmit.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    AllTransmit();
                }
            });
            this.add(allStop);
            this.add(allReceive);
            this.add(allTransmit);
        }
    }
    public static AllCtrlPanel allCtrlPanel = new AllCtrlPanel();

    public Main() {
        l2config = new L2Config();
        asioHost = new ASIOHost();
        
        // txrxApps.add(new TxRx("TxRx 1", (byte) 0x00, (byte) 0x01));
        // txrxApps.add(new TxRx("TxRx 2", (byte) 0x01, (byte) 0x00));
        
        me = new AethHost("AethHost_me"); // assigned to the aetherRouter
        // far = new AethHost("AethHost_far"); // for experiment
        AetherRoute.init(me);
        SysRoute.start();
        // because of java lazy initialization, now the parameters of ipHost are
        // initialized in the ctors
        ethConfig = new EthConfig();
        // ui should be launched last because it has to collect all the panels,
        // also, it should wait for other threads to be ready
        ethHost = new EthUIHost();
    }

    public static void main(String[] args) {
        @SuppressWarnings("unused")
        Main main = new Main();
    }
}
