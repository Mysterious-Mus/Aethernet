package com.aethernet.Aethernet;

import java.util.Map;

import org.pcap4j.packet.Packet;

import java.util.HashMap;
import java.net.Inet4Address;
import com.aethernet.Aethernet.utils.IPAddr;
import com.aethernet.Aethernet.utils.PacketResolve;

public class ARPTable {

    Map<Inet4Address, Byte> table;

    public ARPTable() {
        table = new HashMap<Inet4Address, Byte>();
        addInitEntries();
    }
 
    public void addInitEntries() {
        // 172.0.0.1 -> 0x01
        table.put(IPAddr.buildV4FromStr("172.18.5.1"), (byte) 0x01);
        table.put(IPAddr.buildV4FromStr("172.18.5.2"), (byte) 0x02);
    }

    /* null for not present */
    public Byte query(Inet4Address ipAddr) {
        return table.get(ipAddr);
    }

    public Byte query(Packet packet) {
        return query(PacketResolve.getDstIP(packet));
    }
}
