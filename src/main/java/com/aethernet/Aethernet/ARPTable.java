package com.aethernet.Aethernet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;

import org.pcap4j.packet.Packet;

import java.util.HashMap;
import java.net.Inet4Address;
import com.aethernet.Aethernet.utils.IPAddr;
import com.aethernet.Aethernet.utils.PacketResolve;
import com.aethernet.utils.ANSI;
import com.aethernet.utils.sync.Notifier;

public class ARPTable {

    public static class Configs {
        public static int EXPIRE_TIME = 15;  // seconds
    }

    Map<Inet4Address, Byte> table;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public ARPTable() {
        table = new HashMap<Inet4Address, Byte>();
       // addInitEntries();
    }
 
    public void addInitEntries() {
        // 172.0.0.1 -> 0x01
        table.put(IPAddr.buildV4FromStr("172.0.0.1"), (byte) 0x01);
        table.put(IPAddr.buildV4FromStr("172.0.0.2"), (byte) 0x02);
    }

    /* null for not present */
    public Byte query(Inet4Address ipAddr) {
        return table.get(ipAddr);
    }

    public Byte query(Packet packet) {
        return query(PacketResolve.getDstIP(packet));
    }
    
    public void addEntry(Inet4Address ipaddr, Byte macaddr) {
        // if already present, overwrite
        table.put(ipaddr, macaddr);
        // cancel the previous delete task if it exists
        // schedule a new delete task
        executor.schedule(() -> {
            table.remove(ipaddr);
            System.out.println(ANSI.ANSI_CYAN +"ARPTable: entry " + ipaddr + " deleted"+ANSI.ANSI_RESET);
        }, Configs.EXPIRE_TIME, TimeUnit.SECONDS);
    }

}
