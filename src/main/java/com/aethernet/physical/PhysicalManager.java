package com.aethernet.physical;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
// action
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.GridLayout;
import java.sql.Array;
import java.util.ArrayList;

import com.aethernet.ASIO.ASIOHost;
import com.aethernet.ASIO.ASIOHost.NewBufferListener;
import com.aethernet.UI.panels.ChannelSelectPanel;
import com.aethernet.config.L2Config;
import com.aethernet.config.L2Config.ConfigTerm;
import com.aethernet.mac.MacFrame;
import com.aethernet.mac.MacFrame.Header;
import com.aethernet.mac.MacManager.physicalCallback;
import com.aethernet.physical.receive.Demodulator;
import com.aethernet.physical.transmit.EthernetPacket;
import com.aethernet.physical.transmit.OFDM;
import com.aethernet.physical.transmit.SoF;
import com.aethernet.utils.CyclicBuffer;
import com.aethernet.utils.Player;
import com.aethernet.utils.TypeConvertion;
import com.aethernet.utils.sync.Notifier;
import com.aethernet.utils.sync.Permission;
import com.synthbot.jasiohost.AsioChannel;

public class PhysicalManager {

    public static class Configs {
        public static ConfigTerm<Double> channelEnergy = 
            new ConfigTerm<Double>("channelEnergy", 0.0, true)
        {
            @Override
            public void PassiveParamUpdVal() {
                return;
            }
        };

        public static ConfigTerm<Double> channelClearThresh =
            new ConfigTerm<Double>("channelClearThresh", 0.001, false);
    }

    public static class ChannelEnergyPanel extends JPanel {

        private class set10ptButton extends JButton {
            public set10ptButton() {
                this.setText("Set 10% of Channel Energy");
                this.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Configs.channelClearThresh.set(Configs.channelEnergy.v() * 0.1);
                    }
                });
            }
        }

        private class resetMaxObserveBtn extends JButton {
            public resetMaxObserveBtn() {
                this.setText("Reset Max Observe");
                this.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Configs.channelEnergy.set(0.0);
                    }
                });
            }
        }
    
        public ChannelEnergyPanel() {
            this.setLayout(new GridLayout(0, 1));

            JPanel channelEnergyDisp = new JPanel();
            channelEnergyDisp.setLayout(new GridLayout(0, 4));
            channelEnergyDisp.add(new JLabel("Channel Energy:"));
            channelEnergyDisp.add(Configs.channelEnergy.displayer());
            channelEnergyDisp.add(new JLabel("Channel Clear Threshold:"));
            channelEnergyDisp.add(Configs.channelClearThresh.displayer());

            add(channelEnergyDisp);

            JPanel set10ptButtonPanel = new JPanel();
            set10ptButtonPanel.setLayout(new GridLayout(0, 2));
            set10ptButtonPanel.add(new resetMaxObserveBtn());
            set10ptButtonPanel.add(new set10ptButton());

            add(set10ptButtonPanel);
        }
    }
    public static ChannelEnergyPanel channelEnergyPanel = new ChannelEnergyPanel();

    public static int PHYSICAL_BUFFER_SIZE = 200000;

    private String physMgrName;
    private CyclicBuffer<Float> sampleBuffer = new CyclicBuffer<Float>(PHYSICAL_BUFFER_SIZE);
    private AsioChannel receiveChannel;
    private AsioChannel sendChannel;

    private Notifier newSampleNotify = new Notifier();

    private physicalCallback macInterface;

    private double calcEnergy(float[] x) {
        double energy = 0;
        for (float sample : x) {
            energy += sample * sample;
        }
        return energy / x.length;
    }

    /**
     * Interface for ASIOHost for sample receiving
     */
    private NewBufferListener newBufferListener = new NewBufferListener() {
        @Override
        public void handleNewBuffer(float[] buffer) {
            // calc channel energy
            double energy = calcEnergy(buffer);
            if (energy > Configs.channelEnergy.v()) {
                Configs.channelEnergy.set(energy);
            }
            macInterface.channelClear(energy < Configs.channelClearThresh.v());
            // if both detect and decode are not permitted, discard the samples
            if (!permissions.detect.isPermitted() && !permissions.decode.isPermitted()) {
                sampleBuffer.setFIW(sampleBuffer.tailIdx() + buffer.length);
            }
            else {
                sampleBuffer.pusharr(TypeConvertion.floatArr2FloatList(buffer));
                newSampleNotify.mNotify();
            }
        }
    };

    /**
     * Interface for channelSelUI to listen to channel change events
     */
    public interface ChannelChangedListener {
        public void ChannelChanged(AsioChannel channel);
    }
    private ChannelChangedListener inChangedListener = new ChannelChangedListener() {
        @Override
        public void ChannelChanged(AsioChannel channel) {
            ASIOHost.unregisterReceiver(receiveChannel, newBufferListener);
            receiveChannel = channel;
            ASIOHost.registerReceiver(receiveChannel, newBufferListener);
        }
    };
    private ChannelChangedListener outChangedListener = new ChannelChangedListener() {
        @Override
        public void ChannelChanged(AsioChannel channel) {
            ASIOHost.unregisterPlayer(sendChannel);
            sendChannel = channel;
            ASIOHost.registerPlayer(sendChannel);
        }
    };
    private ChannelSelectPanel channelSelectPanel;

    public class Permissions {
        public Permission detect = new Permission(false);
        public Permission decode = new Permission(false);
    }

    public Permissions permissions = new Permissions();

    private Thread detectThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                newSampleNotify.mWait();
                permissions.detect.waitTillPermitted();
                if (permissions.decode.isPermitted()) {
                    System.out.println("Error: detectThread: both detect and decode are permitted");
                }
                detectFrame();
            }
        }
    });

    private Thread decodeThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                permissions.decode.waitTillPermitted();
                newSampleNotify.mWait();
                decode();
            }
        }
    });

    public PhysicalManager(String name, physicalCallback macInterface) {
        this.physMgrName = name;
        this.macInterface = macInterface;

        // at construction, defaultly register the first available channel
        // for both input and output
        receiveChannel = ASIOHost.availableInChannels.iterator().next();
        ASIOHost.registerReceiver(receiveChannel, newBufferListener);
        sendChannel = ASIOHost.availableOutChannels.iterator().next();
        ASIOHost.registerPlayer(sendChannel);

        channelSelectPanel = new ChannelSelectPanel(
            physMgrName, receiveChannel, sendChannel, inChangedListener, outChangedListener);

        // launch receiving thread
        detectThread.start();
        decodeThread.start();
    }

    public ChannelSelectPanel getChannelSelectPanel() {
        return channelSelectPanel;
    }

    /**
     * The send function in Physical Layer<p>
     * send the MACFRAME<p>
     * will block the thread until the frame is sent<p>
     *
     * @param frame: the frame to be sent
     * @return void
     */
    public void send(MacFrame macframe) {
        float [] samples = EthernetPacket.getPacket(macframe.getWhole());

        // wait till the samples are played
        ASIOHost.waitTransmit(sendChannel);

        // play the samples
        ASIOHost.play(sendChannel, TypeConvertion.floatArr2FloatList(samples));
        
        // wait till the samples are played
        ASIOHost.waitTransmit(sendChannel);
    }

    /**
     * perform SoF detection<p>
     * pipeline:<p>
     * 1. find a point with correlation > threshold<p>
     * 2. find the greatest point in the window<p>
     */
    private void detectFrame() {
        // we say the candidate is the index of the first sample of the SoF
        // where should our candidates be?
        int earlyCandidate = sampleBuffer.FIW;
        int lateCandidate = sampleBuffer.tailIdx() - SoF.Configs.sofNSamples.v();
        int lateCandidateWindow = lateCandidate - SoF.Configs.SofDetectWindow.v();
        for (int threshCheckIdx = earlyCandidate; threshCheckIdx <= lateCandidateWindow; threshCheckIdx ++) {
            double corrCheck = SoF.calcCorr(sampleBuffer, threshCheckIdx);
            if (corrCheck > SoF.Configs.SofDetectThreshold.v()) {
                double maxCorr = corrCheck;
                int maxCorrIdx = threshCheckIdx;
                for (int windowCheckIdx = threshCheckIdx + 1; 
                    windowCheckIdx <= threshCheckIdx + SoF.Configs.SofDetectWindow.v();
                    windowCheckIdx ++) {
                    double corrWindowCheck = SoF.calcCorr(sampleBuffer, windowCheckIdx);
                    if (corrWindowCheck > maxCorr) {
                        maxCorr = corrWindowCheck;
                        maxCorrIdx = windowCheckIdx;
                    }
                }
                // update the states
                frameDetAct(maxCorrIdx);
                return;
            }
        }
        // discard samples before and including lateCandidateWindow
        sampleBuffer.setFIW(lateCandidateWindow + 1);
    }

    private synchronized void frameDetAct(int maxCorrIdx) {
        // callback
        // contension prevension
        if (!permissions.detect.isPermitted() || permissions.decode.isPermitted()) {
            return;
        }
        macInterface.frameDetected();
        // if decoding is permitted
        if (permissions.decode.isPermitted()) {
            // discard SoF samples
            sampleBuffer.setFIW(maxCorrIdx + SoF.Configs.sofNSamples.v() + SoF.Configs.sofEndMuteNSamples.v());
            // clear frameBuffer
            frameBuffer.clear();
            headerReported = false;
        }
        else {
            // discard all samples
            sampleBuffer.setFIW(sampleBuffer.tailIdx());
        }
    }

    ArrayList<Boolean> frameBuffer = new ArrayList<Boolean>();
    boolean headerReported = false;
    int expectedFrameBitLen = 0;
    private void decode() {
        // decode till samples are used up or we have a frame
        while (frameBuffer.size() < MacFrame.getMaxFrameBitLen()) {
            // get the next sample
            float[] symbolSamples = popNxtSample();
            if (symbolSamples == null) {
                // if not enough samples, terminate
                break;
            }
            // decode the sample
            ArrayList<Boolean> bits = Demodulator.demodulateSymbol(symbolSamples);
            // add the bits to the frameBuffer
            frameBuffer.addAll(bits);
        }

        if (!permissions.decode.isPermitted()) return;
        // if we get enough for a header, report to MAC
        // if the header is already wrong or is ack, don't do full frame got report
        // otherwise this function can go on
        if (!headerReported && frameBuffer.size() >= MacFrame.Header.getNbit()) {
            headerReported = true;
            Header headerGot = new MacFrame.Header(
                TypeConvertion.booleanList2ByteArray(
                    new ArrayList<>(frameBuffer.subList(0, MacFrame.Header.getNbit()))
                )
            );
            expectedFrameBitLen = MacFrame.getFrameBitLen(headerGot);
            macInterface.headerReceived(
                headerGot
            );
            // if now we don't have the permission to decode
            if (!permissions.decode.isPermitted()) {
                // // print message
                // System.out.println("Error: decodeThread: header received but decode is not permitted");
                // clear frameBuffer
                frameBuffer.clear();
                headerReported = false;
                return;
            }
        }

        // if we gets a frame
        if (frameBuffer.size() >= expectedFrameBitLen) {
            // pop padding
            while (frameBuffer.size() > expectedFrameBitLen) {
                frameBuffer.remove(frameBuffer.size() - 1);
            }
            // convert to MacFrame
            MacFrame frame = new MacFrame(frameBuffer);
            // invoke callback
            macInterface.frameReceived(frame);
        }
    }

    private float[] popNxtSample() {
        // check buffer size
        if (sampleBuffer.size() < OFDM.Configs.cyclicPrefixNSamples.v() + OFDM.Configs.symbolLength.v()) {
            return null;
        }

        // skip the cyclic prefix
        sampleBuffer.setFIW(sampleBuffer.FIW + OFDM.Configs.cyclicPrefixNSamples.v());

        // get the samples of the symbol
        float[] samples = new float[OFDM.Configs.symbolLength.v()];
        for (int i = 0; i < OFDM.Configs.symbolLength.v(); i++) {
            samples[i] = sampleBuffer.popFront();
        }

        return samples;
    }

}
