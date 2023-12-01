package com.aethernet.physical.transmit;

import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.aethernet.ASIO.ASIOHost;
import com.aethernet.config.L2Config;
import com.aethernet.config.L2Config.ConfigTerm;
import com.aethernet.utils.CyclicBuffer;

import java.awt.GridLayout;

/* Dataframe SoF generator
 * The SoF is a 1ms long sound that is used to indicate the start of a dataframe
 * The SoF is a chirp signal in Dhwani's implementation, the formulation
 * can be found in notes.ipynb
 */
public class SoF {

    public static class Configs {
        public static ConfigTerm<Float> SoF_amplitude = new ConfigTerm<Float>("SoF_amplitude", 1f, false);
        public static ConfigTerm<Double> SoF_T = new ConfigTerm<Double>("SoF_T", 0.0004, false);
        public static ConfigTerm<Integer> sofNSamples = new ConfigTerm<Integer>("sofNSamples", null, true)
        {
            @Override
            public void PassiveParamUpdVal() {
                set((int)(2 * SoF.Configs.SoF_T.v() * ASIOHost.Configs.sampleRate.v()));
            }
        };
        public static ConfigTerm<Double> SoF_fmin = new ConfigTerm<Double>("SoF_fmin", 6000.0, false);
        public static ConfigTerm<Double> SoF_fmax = new ConfigTerm<Double>("SoF_fmax", 16000.0, false);
        public static ConfigTerm<Double> SofEndMuteT = new ConfigTerm<Double>("SofEndMuteT", 0.0, false);
        public static ConfigTerm<Integer> sofEndMuteNSamples = new ConfigTerm<Integer>("sofEndMuteNSamples", null, true)
        {
            @Override
            public void PassiveParamUpdVal() {
                set((int)(SoF.Configs.SofEndMuteT.v() * ASIOHost.Configs.sampleRate.v()));
            }
        };
        public static ConfigTerm<Double> maxSofCorrDetect = 
            new ConfigTerm<Double>("maxSofCorrDetect", 0.0, true)
        {
            @Override
            public void PassiveParamUpdVal() {
                return;
            }
        };
        public static ConfigTerm<Double> SofDetectThreshold = 
            new ConfigTerm<Double>("SofDetectThreshold", 0.3, false);
        public static ConfigTerm<Integer> SofDetectWindow = 
            new ConfigTerm<Integer>("SofDetectWindow", 100, false);
    }

    public static boolean alignBitFunc(int idx) {return (idx % 5 <= 2);}

    public static class DetectorPanel extends JPanel{

        
        public DetectorPanel() {
            JButton resetMaxCorrButton = new JButton("Reset Max Corr");
            resetMaxCorrButton.addActionListener(e -> {
                Configs.maxSofCorrDetect.set(0.0);
            });

            JButton setSofDetectThresholdButton = new JButton("Set 70%");
            setSofDetectThresholdButton.addActionListener(e -> {
                Configs.SofDetectThreshold.set(0.7 * Configs.maxSofCorrDetect.v());
            });
            // set layout along Y axis
            this.setLayout(new GridLayout(0, 1));
            JPanel statGrid = new JPanel();
            statGrid.setLayout(new GridLayout(0, 4));

            statGrid.add(new JLabel("Max SoF Corr Dectected:"));
            statGrid.add(Configs.maxSofCorrDetect.displayer());
            statGrid.add(new JLabel("SoF Threshold:"));
            statGrid.add(Configs.SofDetectThreshold.displayer());
            add(statGrid);

            JPanel ctrlGrid = new JPanel();
            ctrlGrid.setLayout(new GridLayout(1, 0));
            ctrlGrid.add(resetMaxCorrButton);
            ctrlGrid.add(setSofDetectThresholdButton);
            add(ctrlGrid);

            JPanel paramGrid = new JPanel();
            paramGrid.setLayout(new GridLayout(0, 4));
            paramGrid.add(new JLabel("SofDetectWindow"));
            paramGrid.add(Configs.SofDetectWindow.displayer());
            add(paramGrid);
        }
    }
    public static DetectorPanel detectorPanel = new DetectorPanel();

    public static float[] SofNoSilence = generateSoFNoSilence();

    public static float[] generateSoF() {
        float[] samples = new float[SoF.Configs.sofNSamples.v() + SoF.Configs.sofEndMuteNSamples.v()];
        
        float[] samplesNoSilence = generateSoFNoSilence();
        System.arraycopy(samplesNoSilence, 0, samples, 0, SoF.Configs.sofNSamples.v());
        return samples;
    }

    public static float[] generateSoFNoSilence() {
        float[] samples = new float[SoF.Configs.sofNSamples.v()];
        float a = (float)((Configs.SoF_fmax.v() - Configs.SoF_fmin.v()) / Configs.SoF_T.v());
        float phi0 = (float)(Math.PI * a * Configs.SoF_T.v() * Configs.SoF_T.v());
        // stage 1
        for (int i = 0; i < SoF.Configs.sofNSamples.v() / 2; i++) {
            float time = (float) i / (float) SoF.Configs.sofNSamples.v();
            float phase = (float) (Math.PI * a * time * time);
            samples[i] = Configs.SoF_amplitude.v() * (float) Math.cos(phase);
        }
        // stage 2
        for (int i = SoF.Configs.sofNSamples.v() / 2; i < SoF.Configs.sofNSamples.v(); i++) {
            float t = (float) i / ASIOHost.Configs.sampleRate.v().floatValue();
            float phase = (float) (phi0 + Configs.SoF_fmax.v()*(t-Configs.SoF_T.v()) - 
                Math.PI * a * (t-Configs.SoF_T.v()) * (t-Configs.SoF_T.v()));
            samples[i] = Configs.SoF_amplitude.v() * (float) Math.cos(phase);
        }
        return samples;
    }

    public static int NSample() {
        return (int) (2 * Configs.SoF_T.v() * Configs.SoF_amplitude.v());
    }

    // public static ArrayList<Boolean> alignBits() {
    //     ArrayList<Boolean> bits = new ArrayList<Boolean>();
    //     for (int i = 0; i < Config.alignBitLen; i++) {
    //         bits.add(alignBitFunc(i));
    //     }
    //     return bits;
    // }

    public static double calcCorr(ArrayList<Float> buffer, int startIdx) {
        double corr = 0;
        for (int i = 0; i < SoF.Configs.sofNSamples.v(); i++) {
            corr += buffer.get(startIdx+i) * SoF.SofNoSilence[i];
        }
        corr /= SoF.Configs.sofNSamples.v();
        return corr;
    }

    public static double calcCorr(CyclicBuffer<Float> buffer, int startIdx) {
        double corr = 0;
        for (int i = 0; i < SoF.Configs.sofNSamples.v(); i++) {
            corr += buffer.get(startIdx+i) * SoF.SofNoSilence[i];
        }
        corr /= SoF.Configs.sofNSamples.v();
        if (corr > Configs.maxSofCorrDetect.v()) {
            Configs.maxSofCorrDetect.set(corr);
        }
        return corr;
    }
}