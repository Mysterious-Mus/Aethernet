package com.aethernet.Aethernet;

import java.util.Map;
import java.util.HashMap;
import java.net.Inet4Address;
import com.aethernet.Aethernet.utils.IPAddr;

public class ARPTable {

    static Map<Inet4Address, Byte> table;

    public static void init() {
        table = new HashMap<Inet4Address, Byte>();
    }
 
    static void addInitEntries() {
        // 172.0.0.1 -> 0x01
        table.put(IPAddr.buildV4FromStr("172.0.0.1"), (byte) 0x01);
        table.put(IPAddr.buildV4FromStr("172.0.0.1"), (byte) 0x01);
    }

    /* null for not present */
    static byte query(Inet4Address ipAddr) {
        return table.get(ipAddr);
    }
}
