package com.aethernet.config.utils;

import javax.swing.*;

import com.aethernet.utils.TypeConvertion;

import java.util.*;

public class ConfigTermTemplate<T> {

    T value = null;
    public String name;
    boolean passive;

    TermDisp displayer;

    public interface ConfChangeCallback {
        public void callback();
    }

    ConfChangeCallback changeCallback;

    private class TermDisp {

        public JComponent displayer;

        public TermDisp() {
            this.displayer = passive? new JLabel(value2Str()) : new JTextField(value2Str());

            // set concentration lost callback
            if (!passive) {
                ((JTextField)displayer).addActionListener(e -> {
                    // print message
                    System.out.println("ConfigTerm: " + name + " changed to " + ((JTextField)displayer).getText());
                    // update the value
                    T newVal = fromString(((JTextField)displayer).getText());
                    if (newValCheck(newVal)) {
                        set(newVal);
                    }
                    updDisp();
                    changeCallback.callback();
                });
            }
        }

        public void updDisp() {
            if (passive) {
                ((JLabel)displayer).setText(value2Str());
            }
            else {
                ((JTextField)displayer).setText(value2Str());
            }
        }

    }

    public JComponent displayer() {
        return displayer.displayer;
    }

    public ConfigTermTemplate(String name, T value, boolean passive,
        Map<String, ConfigTermTemplate> configTermsMap,
        ConfChangeCallback changeCallback) 
    {
        this.name = name;
        this.value = value;
        this.passive = passive;
        this.displayer = new TermDisp();
        this. changeCallback = changeCallback;
        configTermsMap.put(name, this);
    }

    public boolean isPassive() {
        return passive;
    }

    public String value2Str() {
        if (value instanceof Integer) {
            return Integer.toString((Integer) value);
        }
        else if (value instanceof Float) {
            return Float.toString((Float) value);
        }
        else if (value instanceof Double) {
            return Double.toString((Double) value);
        }
        else if (value instanceof Boolean) {
            return Boolean.toString((Boolean) value);
        }
        else if (value instanceof String) {
            return (String)value;
        }
        else if (value instanceof Byte) {
            return Integer.toString(TypeConvertion.unsignedByteToInt((Byte) value));
        }
        else {
            return "Unsupported Type";
        }
    }

    public T fromString(String x) {
        if (value instanceof Integer) {
            return (T) Integer.valueOf(x);
        }
        else if (value instanceof Float) {
            return (T) Float.valueOf(x);
        }
        else if (value instanceof Double) {
            return (T) Double.valueOf(x);
        }
        else if (value instanceof Boolean) {
            return (T) Boolean.valueOf(x);
        }
        else if (value instanceof String) {
            return (T)x;
        }
        else if (value instanceof Byte) {
            int intValue = Integer.parseInt(x);
            return (T) Byte.valueOf((byte) (intValue & 0xFF));
        }
        else {
            return null;
        }
    }

    public T v() {
        return value;
    }

    public void set(T x) {
        value = x;
        displayer.updDisp();
        newvalOp(x);
    }

    /**
     * for some config instances, this function can be overridden
     * it will be called whenever the config changes
     */
    public void newvalOp(T x) {
    }

    public void set(String x) {
        set(fromString(x));
    }

    /**
     * for passive parameters, this function should be overridden <hr>
     * specifying how to update the value as an anonymous class for each instance
    */
    public void PassiveParamUpdVal() {
        // print not implemented
        System.out.println("PassiveParamUpdVal not implemented: " + name);
    }

    public boolean newValCheck(T newVal) {return true;};
}