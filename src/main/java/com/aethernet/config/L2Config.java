package com.aethernet.config;

import javax.swing.*;

import com.aethernet.ASIO.ASIOHost;
import com.aethernet.mac.MacFrame;
import com.aethernet.mac.MacManager;
import com.aethernet.physical.PhysicalManager;
import com.aethernet.physical.transmit.OFDM;
import com.aethernet.physical.transmit.SoF;
import com.aethernet.utils.FileOp;
import com.aethernet.config.utils.ConfigTermTemplate;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

/*
 * Steps to add one parameter to panel:
 *  1. declare it with ConfigTerm<T> class
 *  2. initialize it at the ctor Config() since some of them won't init properly before used
 *      also for proper update order
 *  3. add it to panel
 */


public class L2Config {

    public static final String CONF_PATH = "configs\\L2Config.txt";

    public static ArrayList<ConfigTermTemplate> ConfigTermList = new ArrayList<>();
    public static Map<String, ConfigTermTemplate> configTermsMap = new HashMap<String, ConfigTermTemplate>();

    /**
     * VIEW 'STEPS TO ADD CONFIG' <hr>
     * A class to hold the name and value of a config term <hr>
     * The Config class holds a reference of each and is in charge of <hr>
     * modifying them during runtime parameter tuning <hr>
     * 
     * NAME is supposed to be the variable name for the sake of maintenance
     */
    public static class ConfigTerm<T> extends ConfigTermTemplate<T>{

        public ConfigTerm(String name, T value, boolean passive) {
            super(name, value, passive, configTermsMap, () -> {ConfigChange();});
        }
    }


    public class ConfigPanel extends JPanel {

        public class DumpButton extends JButton {
            public DumpButton() {
                super("Dump");
                this.addActionListener(e -> {
                    DumpConfig();
                });
            }
        }

        public class LoadButton extends JButton {
            public LoadButton() {
                super("Load");
                this.addActionListener(e -> {
                    LoadConfig();
                });
            }
        }

        public ConfigPanel() {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            constructRow("BUFFER_SIZE", null);
            constructRow("sampleRate", "subCarrierDist");
            constructRow("symbolLength", "subCarrierWidth");
            constructRow("cyclicPrefixLenth", "cyclicPrefixNSamples");
            constructRow("cyclicPrefixMute", null);
            constructRow("bandWidthLowEdit", "bandWidthLow");
            constructRow("bandWidthHighEdit", "bandWidthHigh");
            constructRow("numSubCarriers", null);
            constructRow("payloadMaxNumBytes", null);
            constructRow("PSKeyingCapacity", "ASKeyingCapacity");
            constructRow("symbolCapacity", "SoF_amplitude");
            constructRow("SoF_T", "sofNSamples");
            constructRow("SoF_fmin", "SoF_fmax");
            constructRow("SofEndMuteT", "sofEndMuteNSamples");
            constructRow("ACK_EXPIRE_TIME", "BACKOFF_UNIT");
            constructRow("BACKOFF_MAX_TIMES", "BACKOFF_AFTER_ACKED");

            JPanel loadAndDump = new JPanel();
            loadAndDump.setLayout(new GridLayout(0, 2));
            loadAndDump.add(new LoadButton()); loadAndDump.add(new DumpButton());
            this.add(loadAndDump);
        }

        private void constructRow(String paramName1, String paramName2) {
            JPanel row = new JPanel();
            row.setLayout(new GridLayout(0, 4));
            if (paramName1 != null) {
                row.add(new JLabel(paramName1)); row.add(configTermsMap.get(paramName1).displayer());
            }
            else {
                row.add(new JLabel()); row.add(new JLabel());
            }
            if (paramName2 != null) {
                row.add(new JLabel(paramName2)); row.add(configTermsMap.get(paramName2).displayer());
            }
            else {
                row.add(new JLabel()); row.add(new JLabel());
            }
            this.add(row);
        }
    }
    public static ConfigPanel panel;

    public L2Config() {
        ConfigTermList.add(ASIOHost.Configs.sampleRate);
        ConfigTermList.add(ASIOHost.Configs.BUFFER_SIZE);
        ConfigTermList.add(MacFrame.Configs.payloadMaxNumBytes);
        ConfigTermList.add(OFDM.Configs.subCarrierDist);
        ConfigTermList.add(OFDM.Configs.symbolLength);
        ConfigTermList.add(OFDM.Configs.subCarrierWidth);
        ConfigTermList.add(OFDM.Configs.cyclicPrefixLenth);
        ConfigTermList.add(OFDM.Configs.cyclicPrefixNSamples);
        ConfigTermList.add(OFDM.Configs.cyclicPrefixMute);
        ConfigTermList.add(OFDM.Configs.bandWidthLowEdit);
        ConfigTermList.add(OFDM.Configs.bandWidthLow);
        ConfigTermList.add(OFDM.Configs.bandWidthHighEdit);
        ConfigTermList.add(OFDM.Configs.bandWidthHigh);
        ConfigTermList.add(OFDM.Configs.numSubCarriers);
        ConfigTermList.add(OFDM.Configs.PSKeyingCapacity);
        ConfigTermList.add(OFDM.Configs.ASKeyingCapacity);
        ConfigTermList.add(OFDM.Configs.symbolCapacity);
        ConfigTermList.add(SoF.Configs.SoF_amplitude);
        ConfigTermList.add(SoF.Configs.SoF_T);
        ConfigTermList.add(SoF.Configs.sofNSamples);
        ConfigTermList.add(SoF.Configs.SoF_fmin);
        ConfigTermList.add(SoF.Configs.SoF_fmax);
        ConfigTermList.add(SoF.Configs.SofEndMuteT);
        ConfigTermList.add(SoF.Configs.sofEndMuteNSamples);
        ConfigTermList.add(SoF.Configs.maxSofCorrDetect);
        ConfigTermList.add(SoF.Configs.SofDetectThreshold);
        ConfigTermList.add(SoF.Configs.SofDetectWindow);
        ConfigTermList.add(MacManager.Configs.ACK_EXPIRE_TIME);
        ConfigTermList.add(MacManager.Configs.BACKOFF_UNIT);
        ConfigTermList.add(MacManager.Configs.BACKOFF_MAX_TIMES);
        ConfigTermList.add(FileOp.Configs.INPUT_DIR);
        ConfigTermList.add(FileOp.Configs.OUTPUT_DIR);
        ConfigTermList.add(PhysicalManager.Configs.channelEnergy);
        ConfigTermList.add(PhysicalManager.Configs.channelClearThresh);
        ConfigTermList.add(MacManager.Configs.BACKOFF_AFTER_ACKED);
        ConfigTermList.add(ASIOHost.Configs.allowChannelMultiAssign);

        LoadConfig();

        // Create a panel with text fields for each field in the Config class
        panel = new ConfigPanel();
        ConfigChange();
    }
    
    public static void ConfigChange() {
        // check all passive configs
        for (ConfigTermTemplate term : ConfigTermList) {
            if (term.isPassive()) {
                term.PassiveParamUpdVal();
            }
        }
    }

    public static void DumpConfig() {
        // print info
        System.out.println("Dumping config to " + CONF_PATH);
        try (PrintWriter writer = new PrintWriter(new File(CONF_PATH))) {
            for (ConfigTermTemplate term : ConfigTermList) {
                if (!term.isPassive()) {
                    writer.println(term.name + " " + term.v());
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void LoadConfig() {
        // print info
        System.out.println("Loading config from " + CONF_PATH);
        try (Scanner scanner = new Scanner(new File(CONF_PATH))) {
            while (scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split(" ");
                ConfigTermTemplate term = configTermsMap.get(line[0]);
                if (term != null) term.set(line[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ConfigChange();
    }
}