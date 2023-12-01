package com.aethernet.Aethernet.utils;

import java.net.Inet4Address;
import java.net.InetAddress;

public class IPAddr {
    
    public static Inet4Address buildV4FromStr(String x) {
        Inet4Address result = null;
        try {
            result = (Inet4Address) InetAddress.getByName(x);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
