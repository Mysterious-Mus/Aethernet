package com.aethernet.Aethernet.utils;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

import com.aethernet.Aethernet.utils.IPAddr;
import com.aethernet.Aethernet.utils.PacketResolve;

import org.pcap4j.packet.Packet;

public class RoutingTableEntry {
    private Inet4Address networkAddress;
    private Inet4Address subnetMask;

    public RoutingTableEntry(Inet4Address networkAddress, Inet4Address subnetMask) {
        this.networkAddress = networkAddress;
        this.subnetMask = subnetMask;
    }

    public RoutingTableEntry(String networkAddr, String subnetMask) {
        this.networkAddress = IPAddr.buildV4FromStr(networkAddr);
        this.subnetMask = IPAddr.buildV4FromStr(subnetMask);
    }

    public boolean matches(Inet4Address ipAddress) {
        byte[] ipAddressBytes = ipAddress.getAddress();
        byte[] subnetMaskBytes = subnetMask.getAddress();
        byte[] networkAddressBytes = networkAddress.getAddress();

        if (ipAddressBytes.length != subnetMaskBytes.length || ipAddressBytes.length != networkAddressBytes.length) {
            throw new IllegalArgumentException("IP address, subnet mask, and network address must be of the same version");
        }

        for (int i = 0; i < ipAddressBytes.length; i++) {
            if ((ipAddressBytes[i] & subnetMaskBytes[i]) != (networkAddressBytes[i] & subnetMaskBytes[i])) {
                return false;
            }
        }

        return true;
    }

    public boolean matches(String addr) {
        return matches(IPAddr.buildV4FromStr(addr));
    }

    public boolean dstMatches(Packet packet) {
        Inet4Address dstIP = PacketResolve.getDstIP(packet);
        if (dstIP == null) return false;
        return matches(dstIP);
    }

    public byte getHostId(Inet4Address ip) {
        byte[] addressBytes = ip.getAddress();
        return addressBytes[addressBytes.length - 1];
    }

    public Inet4Address hostId2Address(byte id) {
        byte[] addressBytes = this.networkAddress.getAddress();
        addressBytes[addressBytes.length - 1] = id;
        try {
            return (Inet4Address) Inet4Address.getByAddress(addressBytes);
        }
        catch (Exception e) {
            return null;
        }
    }
}