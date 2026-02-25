package com.us3rn1me.noVPN;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Represents a single IPv4 CIDR block (e.g. 192.168.0.0/16) and provides
 * a fast membership check for individual addresses.
 *
 * Only IPv4 is supported because the IP lists we consume are IPv4-only.
 */
public class InetAddressRange {

    private final byte[] networkBytes;
    private final int prefixLength;
    private final int mask;

    private InetAddressRange(byte[] networkBytes, int prefixLength) {
        this.networkBytes = networkBytes;
        this.prefixLength = prefixLength;
        this.mask = prefixLength == 0 ? 0 : (0xFFFFFFFF << (32 - prefixLength));
    }

    /**
     * Parses a CIDR string such as {@code "10.0.0.0/8"}.
     *
     * @param cidr CIDR notation string
     * @return the parsed range, or {@code null} if the input cannot be parsed
     */
    public static InetAddressRange parse(String cidr) {
        int slash = cidr.indexOf('/');
        if (slash < 0)
            return null;

        String host = cidr.substring(0, slash);
        int prefix;
        try {
            prefix = Integer.parseInt(cidr.substring(slash + 1));
        } catch (NumberFormatException e) {
            return null;
        }

        if (prefix < 0 || prefix > 32)
            return null;

        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return null;
        }

        byte[] bytes = addr.getAddress();
        if (bytes.length != 4)
            return null; // IPv6 not supported

        return new InetAddressRange(bytes, prefix);
    }

    /**
     * Checks whether the given address falls within this CIDR block.
     *
     * @param address the address to test
     * @return {@code true} if the address is inside this range
     */
    public boolean contains(InetAddress address) {
        byte[] candidate = address.getAddress();
        if (candidate.length != 4)
            return false;

        int network = ByteBuffer.wrap(networkBytes).order(ByteOrder.BIG_ENDIAN).getInt();
        int addr = ByteBuffer.wrap(candidate).order(ByteOrder.BIG_ENDIAN).getInt();

        return (network & mask) == (addr & mask);
    }

    @Override
    public String toString() {
        try {
            return InetAddress.getByAddress(networkBytes).getHostAddress() + "/" + prefixLength;
        } catch (UnknownHostException e) {
            return Arrays.toString(networkBytes) + "/" + prefixLength;
        }
    }
}
