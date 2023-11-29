package com.AcousticNFC.physical.receive;

import java.util.ArrayList;

import com.AcousticNFC.utils.FFT;
import com.AcousticNFC.utils.Complex;
import com.AcousticNFC.utils.ECC;

import java.io.File;

import com.AcousticNFC.Config;
import com.AcousticNFC.mac.MacFrame;
import com.AcousticNFC.physical.transmit.OFDM;
import com.AcousticNFC.utils.FileOp;
import com.AcousticNFC.ASIO.ASIOHost;

public class Demodulator {
    
    public ArrayList<Boolean> frameBuffer;
    // double timeCompensation = 0; // compensate the sampling offset

    ECC Ecc;

    public static Complex[] subCarrCoeffs(float[] samples) {
        Complex[] result = new Complex[OFDM.Configs.numSubCarriers.v()];
        Complex[] fftResult = FFT.fft(samples);
        for (int i = 0; i < OFDM.Configs.numSubCarriers.v(); i++) {
            result[i] = fftResult[
                    (int) Math.round((OFDM.Configs.bandWidthLow.v() + i * OFDM.Configs.subCarrierWidth.v()) / 
                    ASIOHost.Configs.sampleRate.v() * OFDM.Configs.symbolLength.v())];
            // result[i] = fftResult[
            //         (int) Math.round((Config.bandWidthLow + i * OFDM.Configs.subCarrierWidth.v()) / 
            //         ASIOHost.Configs.sampleRate.v() * Config.symbolLength)].phase() + 
            //         timeCompensation * 2 * Math.PI * (Config.bandWidthLow + i * OFDM.Configs.subCarrierWidth.v());
        }
        return result;
    }

    /* Demodulate the next symbol 
     * Push result bits into the receiver's buffer
    */
    public static ArrayList<Boolean> demodulateSymbol(float[] samples) {
        ArrayList<Boolean> resultBuffer = new ArrayList<Boolean>();
        // get Phases
        Complex coeffs[] = subCarrCoeffs(samples);

        // // log the first symbol phases
        // // if the frameBuffer is empty
        // if (frameBuffer.size() == 0) {
        //     String panelInfo = "";
        //     for (int i = 0; i < OFDM.Configs.numSubCarriers.v(); i++) {
        //         panelInfo += String.format("%.2f ", phases[i]);
        //     }
        //     Config.UpdFirstSymbolPhases(panelInfo);
        // }

        // print phases
        // System.out.print("Phases: ");
        // for (int i = 0; i < 8; i++) {
        //     System.out.print(String.format("%.2f ", coeffs[i].phase()));
        // }
        // System.out.println();
        // // print amplitudes
        // System.out.print("Amplitudes: ");
        // for (int i = 0; i < 8; i++) {
        //     System.out.print(String.format("%.2f ", coeffs[i].abs()));
        // }
        // System.out.println();

        // calculate the keys of the subcarriers
        double unitAmp = 0;
        for (int i = 0; i < OFDM.Configs.numSubCarriers.v(); i++) {
            // see proj1.ipynb for the derivation
            double thisCarrierPhase = coeffs[i].phase();

            int numPhaseKeys = 1 << OFDM.Configs.PSKeyingCapacity.v();
            double lastPhaseSegment = 2 * Math.PI / numPhaseKeys / 2;
            int thisCarrierPhaseIndex = (int)Math.floor((thisCarrierPhase + 2 * Math.PI + lastPhaseSegment) 
                % (2 * Math.PI) /  (2 * Math.PI) * numPhaseKeys);
            
            // push the bits into the receiver's buffer
            for (int j = 0; j < OFDM.Configs.PSKeyingCapacity.v(); j++) {
                resultBuffer.add((thisCarrierPhaseIndex & (1 << (OFDM.Configs.PSKeyingCapacity.v() - j - 1))) != 0);
            }

            if (i == 0) {
                unitAmp = coeffs[i].abs();
            }
            else {
                int thisCarrierAmpIdx = (int) Math.round(coeffs[i].abs() / unitAmp) - 1;
                // control with max
                thisCarrierAmpIdx = thisCarrierAmpIdx > (1 << OFDM.Configs.ASKeyingCapacity.v()) - 1 ? 
                    (1 << OFDM.Configs.ASKeyingCapacity.v()) - 1 : thisCarrierAmpIdx;
                // push bits
                for (int j = 0; j < OFDM.Configs.ASKeyingCapacity.v(); j++) {
                    resultBuffer.add((thisCarrierAmpIdx & (1 << (OFDM.Configs.ASKeyingCapacity.v() - j - 1))) != 0);
                }
            }
        }
        return resultBuffer;
    }

    // private void scanTest() {
    //     int alignNSample = Config.alignNSymbol * (OFDM.Configs.cyclicPrefixNSamples.v() + Config.symbolLength);
    //     int alignBitLen = Config.alignNSymbol * OFDM.Configs.PSkeyingCapacity.v() * OFDM.Configs.numSubCarriers.v();
    //     int lastSampleIdx = receiver.tickDone + Config.scanWindow + alignNSample;
    //     while (lastSampleIdx >= receiver.getLength()) {
    //         // busy waiting
    //         Thread.yield();
    //     }
    //     if (lastSampleIdx < receiver.getLength()) {
    //         int bestDoneIdx = receiver.tickDone;
    //         double bestDistortion = 1000;
    //         double bestBER = 1;
    //         for (int doneIdx = receiver.tickDone - Config.scanWindow; 
    //         doneIdx <= receiver.tickDone + Config.scanWindow; doneIdx++) {
    //             int testReceiverPtr = doneIdx;
    //             double avgAbsDistortion = 0;
    //             double avgDistortion = 0;
    //             double BER = 0;
    //             for (int testSymId = 0; testSymId < Config.alignNSymbol; testSymId++) {
    //                 testReceiverPtr += OFDM.Configs.cyclicPrefixNSamples.v();
    //                 float nxtSymbolSamples[] = new float[Config.symbolLength];
    //                 for (int i = 0; i < Config.symbolLength; i++) {
    //                     nxtSymbolSamples[i] = receiver.samples.get(testReceiverPtr + i + 1);
    //                 }
    //                 testReceiverPtr += Config.symbolLength;
    //                 // calculate average time distortion
    //                 double[] phases = subCarrCoeffs(nxtSymbolSamples);
    //                 for (int subCId = 0; subCId < OFDM.Configs.numSubCarriers.v(); subCId ++) {
    //                     double thisCarrierPhase = phases[subCId];
    //                     int numKeys = (int) Math.round(Math.pow(2, OFDM.Configs.PSkeyingCapacity.v()));
    //                     int thisCarrierIdx = 0;
    //                     for (int bitId = 0; bitId < OFDM.Configs.PSkeyingCapacity.v(); bitId ++) {
    //                         thisCarrierIdx += (Config.alignBitFunc(testSymId * OFDM.Configs.PSkeyingCapacity.v() * OFDM.Configs.numSubCarriers.v() + subCId * OFDM.Configs.PSkeyingCapacity.v() + bitId) ? 1 : 0)
    //                          << (OFDM.Configs.PSkeyingCapacity.v() - bitId - 1);
    //                     }
    //                     double requiredPhase = 2 * Math.PI / numKeys * thisCarrierIdx;
    //                     double distortion = ((thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) < 
    //                         (2 * Math.PI) - (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) ? 
    //                         (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) : 
    //                         (thisCarrierPhase - requiredPhase + 4 * Math.PI) % (2 * Math.PI) - 2 * Math.PI);
    //                     avgAbsDistortion += Math.abs(distortion)
    //                                         / (2 * Math.PI * (Config.bandWidthLow + subCId * OFDM.Configs.subCarrierWidth.v()));
    //                     avgDistortion += distortion
    //                                         / (2 * Math.PI * (Config.bandWidthLow + subCId * OFDM.Configs.subCarrierWidth.v()));
    //                 }
    //                 // add to BER
    //                 ArrayList<Boolean> demodulated = demodulateSymbol(nxtSymbolSamples);
    //                 for (int symBitId = 0; symBitId < OFDM.Configs.PSkeyingCapacity.v() * OFDM.Configs.numSubCarriers.v(); symBitId ++) {
    //                     BER += (demodulated.get(symBitId) != Config.alignBitFunc(testSymId * OFDM.Configs.PSkeyingCapacity.v() * OFDM.Configs.numSubCarriers.v() + symBitId) ? 1 : 0);
    //                 }
    //             }
    //             avgAbsDistortion /= alignBitLen;
    //             avgDistortion /= alignBitLen;
    //             BER /= alignBitLen;
    //             if (Math.abs(avgAbsDistortion) < Math.abs(bestDistortion)) {
    //                 bestDistortion = avgAbsDistortion;
    //                 bestDoneIdx = doneIdx;
    //                 // timeCompensation = -avgDistortion;
    //                 bestBER = BER;
    //             }
    //         }
    //         // print compensation: bestdone - tickdone
    //         System.out.println("Compensation: " + (bestDoneIdx - receiver.tickDone));
    //         // timeCompensation = -bestDistortion;
    //         // print avg distort samples
    //         System.out.println("Avg Distort: " + bestDistortion * ASIOHost.Configs.sampleRate.v());
    //         // print BER
    //         System.out.println("BER: " + bestBER);

    //         receiver.scanAligning = false;
    //         receiver.tickDone = bestDoneIdx + alignNSample;
    //         receiver.unpacking = true;
    //     }
    // }
}
