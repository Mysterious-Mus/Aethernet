package com.aethernet.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

import javax.swing.*;
import java.awt.*;

import com.aethernet.config.utils.*;

public class EthConfig {

    public static final String CONF_PATH = "configs\\EthConfig.txt";
    
    public static Map<String, ConfigTermTemplate> configTermsMap = new HashMap<String, ConfigTermTemplate>();


    public static class ConfigTerm<T> extends ConfigTermTemplate<T>{

        public ConfigTerm(String name, T value, boolean passive) {
            super(name, value, passive, configTermsMap, () -> {});
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

            JPanel loadAndDump = new JPanel();
            loadAndDump.setLayout(new GridLayout(0, 2));
            loadAndDump.add(new LoadButton()); loadAndDump.add(new DumpButton());
            this.add(loadAndDump);
        }
    }
    public static ConfigPanel panel;


    public static void DumpConfig() {
        // print info
        System.out.println("Dumping config to " + CONF_PATH);
        try (PrintWriter writer = new PrintWriter(new File(CONF_PATH))) {
            for (Map.Entry<String, ConfigTermTemplate> entry : configTermsMap.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue().value2Str());
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
            // create file
            try {
                File file = new File(CONF_PATH);
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        // ConfigChange();
    }

    public EthConfig() {
        LoadConfig();
        panel = new ConfigPanel();
    }
}
