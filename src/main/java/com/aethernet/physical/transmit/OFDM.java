package com.aethernet.physical.transmit;

import java.util.ArrayList;

import com.aethernet.ASIO.ASIOHost;
import com.aethernet.config.L2Config;
import com.aethernet.config.L2Config.ConfigTerm;

/* Use OFDM to modulate a bit string
 * Always padded with 0s, the length of the modulated signal will be a multiple of:
 * keyingCapacity * numSubCarriers
 * The maximum amplitude of the modulated signal is 0.2
 */
public class OFDM {

    public static class Configs {
        /**
         * The distance of subcarriers in (times of spectrum resolution)
         */
        public static ConfigTerm<Integer> subCarrierDist = 
            new ConfigTerm<Integer>("subCarrierDist", 1, false);
        public static ConfigTerm<Integer> symbolLength = 
            new ConfigTerm<Integer>("symbolLength", 128, false)
        {
            @Override
            public boolean newValCheck(Integer newVal) {
                // if not power of 2
                if ((newVal & (newVal - 1)) != 0) {
                    // report invalid change
                    System.out.println("Invalid change: symbolLength should be power of 2");
                    return false;
                }
                return true;
            }
        };
        public static ConfigTerm<Double> subCarrierWidth = 
            new ConfigTerm<Double>("subCarrierWidth", null, true) 
        {
            @Override
            public void PassiveParamUpdVal() {
                set(ASIOHost.Configs.sampleRate.v() / OFDM.Configs.symbolLength.v() * 
                        OFDM.Configs.subCarrierDist.v());
            }
        };
        /**
         * The length of cyclic prefix in seconds
         */
        public static ConfigTerm<Double> cyclicPrefixLenth = 
            new ConfigTerm<Double>("cyclicPrefixLenth", (Double) 0.0, false);
        public static ConfigTerm<Integer> cyclicPrefixNSamples = 
            new ConfigTerm<Integer>("cyclicPrefixNSamples", null, true) 
        {
            @Override
            public void PassiveParamUpdVal() {
                set((int) Math.round(ASIOHost.Configs.sampleRate.v() * 
                            OFDM.Configs.cyclicPrefixLenth.v()));
            }
        };
        public static ConfigTerm<Boolean> cyclicPrefixMute = 
            new ConfigTerm<Boolean>("cyclicPrefixMute", false, false);
        public static ConfigTerm<Double> bandWidthLowEdit = 
            new ConfigTerm<Double>("bandWidthLowEdit", 4000.0, false);
        public static ConfigTerm<Double> bandWidthLow =
            new ConfigTerm<Double>("bandWidthLow", null, true) 
        {
            @Override
            public void PassiveParamUpdVal() {
                set(Math.max(Math.ceil(OFDM.Configs.bandWidthLowEdit.v() / OFDM.Configs.subCarrierWidth.v()), 1) 
                    * OFDM.Configs.subCarrierWidth.v());
            }
        };
        public static ConfigTerm<Double> bandWidthHighEdit = 
            new ConfigTerm<Double>("bandWidthHighEdit", 14000.0, false);
        public static ConfigTerm<Double> bandWidthHigh =
            new ConfigTerm<Double>("bandWidthHigh", null, true) 
        {
            @Override
            public void PassiveParamUpdVal() {
                set(Math.floor(OFDM.Configs.bandWidthHighEdit.v() / OFDM.Configs.subCarrierWidth.v()) 
                    * OFDM.Configs.subCarrierWidth.v());
            }
        };
        public static ConfigTerm<Integer> numSubCarriers = 
            new ConfigTerm<Integer>("numSubCarriers", null, true) 
        {
            @Override
            public void PassiveParamUpdVal() {
                set((int) Math.round((OFDM.Configs.bandWidthHigh.v() - OFDM.Configs.bandWidthLow.v()) 
                    / OFDM.Configs.subCarrierWidth.v()) + 1);
            }
        };
        public static ConfigTerm<Integer> PSKeyingCapacity =
            new ConfigTerm<Integer>("PSKeyingCapacity", 1, false);
        public static ConfigTerm<Integer> ASKeyingCapacity =
            new ConfigTerm<Integer>("ASKeyingCapacity", 1, false);
        public static ConfigTerm<Integer> symbolCapacity =
            new ConfigTerm<Integer>("symbolCapacity", null, true) 
        {
            @Override
            public void PassiveParamUpdVal() {
                set(OFDM.Configs.numSubCarriers.v() * OFDM.Configs.PSKeyingCapacity.v() + 
                    (OFDM.Configs.numSubCarriers.v() - 1) * OFDM.Configs.ASKeyingCapacity.v());
            }
        };
    }

    public OFDM() {
    }

    /**
     * Applies PSK modulation to a binary string of length 'keyingCapacity'. 
     * Here, the maximum amplitude is 0.2.
     * The Cyclical Prefix is not added.
     * Bits are kept in their original order: the first bit is the most significant bit,
     * to create a 'key' index.
     * Phase for each 'key' is calculated as: 2 * PI * index / numKeys.
    */
    public static float[] phaseShiftKeying(int[] data, double carrierFreq) {
        // sanity: data length should be equal to keyingCapacity
        assert data.length == OFDM.Configs.PSKeyingCapacity.v();

        // number of keys
        int numKeys = (int) Math.pow(2, OFDM.Configs.PSKeyingCapacity.v());

        // Index
        int index = 0;
        for (int i = 0; i < OFDM.Configs.PSKeyingCapacity.v(); i++) {
            index += data[i] * Math.pow(2, OFDM.Configs.PSKeyingCapacity.v() - i - 1);
        }

        // Phase
        float phase = (float) (2 * Math.PI * index / numKeys);

        // Modulated signal
        float[] modulatedSignal = new float[OFDM.Configs.symbolLength.v()];
        for (int i = 0; i < OFDM.Configs.symbolLength.v(); i++) {
            double t = (double) i / ASIOHost.Configs.sampleRate.v();
            // use cos because we use complex representation
            modulatedSignal[i] = 0.8F * (float) Math.cos(2 * Math.PI * carrierFreq * t + phase);
        }

        return modulatedSignal;
    }

    /* Generate an OFDM symbol 
     * The input length should be numSubCarriers * keyingCapacity
     * The subcarriers with lower frequencies transmit the former bits
     * Maximum amplitude is 0.2
     * Cyclical Prefix is added before the data
    */
    public static float[] symbolGen(int[] data) {
        // Sanity: data length should be equal to numSubCarriers * keyingCapacity
        assert data.length == OFDM.Configs.numSubCarriers.v() * OFDM.Configs.PSKeyingCapacity.v();

        // Determine the number of samples per symbol
        int numSamplesPerWholeSymbol = OFDM.Configs.cyclicPrefixNSamples.v() + OFDM.Configs.symbolLength.v();

        // Generate the symbol
        float[] symbol = new float[numSamplesPerWholeSymbol];
        int usedDataPtr = 0;
        for (int i = 0; i < OFDM.Configs.numSubCarriers.v(); i++) {
            // Get the subcarrier frequency
            double carrierFreq = OFDM.Configs.bandWidthLow.v() + i * OFDM.Configs.subCarrierWidth.v();

            int[] subCarrierPhaseData = new int[OFDM.Configs.PSKeyingCapacity.v()];
            System.arraycopy(data, usedDataPtr, subCarrierPhaseData, 0, OFDM.Configs.PSKeyingCapacity.v());
            usedDataPtr += OFDM.Configs.PSKeyingCapacity.v();
            // apply PSK modulation to the subcarrier
            float[] modulatedSubCarrier = phaseShiftKeying(subCarrierPhaseData, carrierFreq);

            int ampIdx;
            // the first carrier is special, it should determine the unit amplitude
            if (i == 0) {
                ampIdx = 0;
            }
            else {
                int[] subCarrierAmpData = new int[Configs.ASKeyingCapacity.v()];
                System.arraycopy(data, usedDataPtr, subCarrierAmpData, 0, Configs.ASKeyingCapacity.v());
                usedDataPtr += Configs.ASKeyingCapacity.v();
                // calc ampIdx
                ampIdx = 0;
                for (int j = 0; j < Configs.ASKeyingCapacity.v(); j++) {
                    ampIdx += subCarrierAmpData[j] * (1 << Configs.ASKeyingCapacity.v() - j - 1);
                }
            }

            // apply amplitude modulation
            int Nlevels = 1 << Configs.ASKeyingCapacity.v();
            for (int j = 0; j < OFDM.Configs.symbolLength.v(); j++) {
                modulatedSubCarrier[j] *= (float) (ampIdx + 1) / Nlevels;
            }

            // Add the subcarrier to the symbol
            for (int j = 0; j < OFDM.Configs.symbolLength.v(); j++) {
                symbol[OFDM.Configs.cyclicPrefixNSamples.v() + j] += modulatedSubCarrier[j] / OFDM.Configs.numSubCarriers.v();
            }
        }

        // Add the cyclic prefix
        for (int i = OFDM.Configs.cyclicPrefixNSamples.v(); i < OFDM.Configs.cyclicPrefixNSamples.v(); i++) {
            symbol[i] = OFDM.Configs.cyclicPrefixMute.v() ? 0 :
                symbol[numSamplesPerWholeSymbol - OFDM.Configs.cyclicPrefixNSamples.v() + i];
        }

        return symbol;
    }

    /* Modulate the input data using OFDM
     * The actual output is padded with 0s to make the length a multiple of
     * keyingCapacity * numSubCarriers = symbolCapacity
     * So make sure the packet length is known by the receiver.
     * The maximum amplitude is 0.2.
     * Cyclical Prefix is added before each symbol.
     */
    public static float[] modulate(ArrayList<Boolean> data) {
        // pad the input with 0s to make the length a multiple of symbolCapacity
        int numSymbols = (int) Math.ceil((double) data.size() / OFDM.Configs.symbolCapacity.v());
        boolean[] paddedData = new boolean[numSymbols * OFDM.Configs.symbolCapacity.v()];
        for (int i = 0; i < data.size(); i++) {
            paddedData[i] = data.get(i);
        }
        // Sanity check
        assert paddedData.length % OFDM.Configs.symbolCapacity.v() == 0;

        // modulate
        int resultNSamples = numSymbols * (OFDM.Configs.cyclicPrefixNSamples.v() + OFDM.Configs.symbolLength.v());
        float[] result = new float[resultNSamples];
        for (int i = 0; i < numSymbols; i++) {
            // Get the data for this symbol
            int[] symbolData = new int[OFDM.Configs.symbolCapacity.v()];
            for (int j = 0; j < OFDM.Configs.symbolCapacity.v(); j++) {
                symbolData[j] = paddedData[i * OFDM.Configs.symbolCapacity.v() + j] ? 1 : 0;
            }

            // Generate the symbol
            float[] symbol = symbolGen(symbolData);

            // Add the symbol to the result
            for (int j = 0; j < OFDM.Configs.cyclicPrefixNSamples.v() + OFDM.Configs.symbolLength.v(); j++) {
                result[i * (OFDM.Configs.cyclicPrefixNSamples.v() + OFDM.Configs.symbolLength.v()) + j] = symbol[j];
            }
        }

        return result;
    }
}
