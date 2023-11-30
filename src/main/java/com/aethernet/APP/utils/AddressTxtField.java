package com.aethernet.APP.utils;

import javax.swing.JTextField;

import com.aethernet.mac.MacFrame;

public class AddressTxtField extends JTextField {
    public AddressTxtField(String initAddr) {
        super();
        this.setText(initAddr);
    }

    public int getAddress() {
        int result =  Integer.parseInt(this.getText(), 16);
        // print error message if address is out of range
        if (result < MacFrame.Configs.addrLb || result > MacFrame.Configs.addrUb) {
            System.out.println("Address out of range");
        }
        return result;
    }
}