package com.AcousticNFC.physical.transmit;

import com.AcousticNFC.Config;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.stream.DoubleStream;

import com.AcousticNFC.utils.ECC;
import com.AcousticNFC.utils.TypeConvertion;

/* Frame Protocol:
 * 1. SoF
 * 2. Length of bit string: 32 bits
 * 3. Bit string
 */
public class EthernetPacket {
    /**
     * Get the packet to be sent to 
     * Recieve a mac frame
     * @param MacFrame
     * @return {@code float[]} packet samples to physical layer
     */
    public static float[] getPacket(byte[] MacFrame) {
        /* 
        * Input: frame from mac layer
        * Modulate the packet using OFDM (byte[] -> float[])
        * Add preamble and SOF filed 
        * Add interpacket gap field
        * Output: packet to physical layer
        */

        ArrayList<Boolean> MacFrameBits = TypeConvertion.byteArray2BooleanList(MacFrame);

        // modulate the MacFrame 
        float[] MacFrameSamples = OFDM.modulate(MacFrameBits);

        // add preamble and SoF
        float[] SoFSamples = SoF.generateSoF();

        // concatenate all the samples
        float[] packetSamples = new float[SoFSamples.length + MacFrameSamples.length];
        System.arraycopy(SoFSamples, 0, packetSamples, 0, SoFSamples.length);
        System.arraycopy(MacFrameSamples, 0, packetSamples, SoFSamples.length, MacFrameSamples.length);

        return packetSamples;

    } 
}
