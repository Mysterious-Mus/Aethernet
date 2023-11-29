package com.AcousticNFC.UI.panels;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JComboBox;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;

import java.util.Set;

import com.AcousticNFC.ASIO.ASIOHost;
import com.AcousticNFC.physical.PhysicalManager.ChannelChangedListener;
import com.synthbot.jasiohost.AsioChannel;
import com.AcousticNFC.UI.UIHost;

public class ChannelSelectPanel extends JPanel{
    
    String physMgrName;
    AsioChannel inChannel, outChannel;
    ChannelChangedListener inChangedListener, outChangedListener;

    private class chanSelWrapper {
        AsioChannel channel;
        String name;
        public chanSelWrapper(AsioChannel channel) {
            this.channel = channel;
            this.name = channel == null? "None": channel.toString();
        }
        @Override
        public String toString() {
            return name;
        }
        public AsioChannel getChannel() {
            return channel;
        }
    }

    private chanSelWrapper emptyChoice = new chanSelWrapper(null);

    class channelSelectCombo extends JComboBox<chanSelWrapper> {
        chanSelWrapper selectRes;
        Set<AsioChannel> selectPool;
        ChannelChangedListener channelChangedListener;

        public channelSelectCombo(Set<AsioChannel> selPool, AsioChannel selRes, ChannelChangedListener listener) {
            super();

            this.selectPool = selPool;
            this.selectRes = new chanSelWrapper(selRes);
            this.channelChangedListener = listener;
            // fill in the items
            for (AsioChannel item : selectPool) {
                addItem(new chanSelWrapper(item));
            }
            if (selectRes != emptyChoice) addItem(emptyChoice);
            // sanity check: selectResult should not be in the pool
            if (selectPool.contains(selectRes.getChannel())) {
                throw new IllegalArgumentException("selectResult should not be in the pool");
            }
            addItem(selectRes);
            // set the initial selected item
            setSelectedItem(selectRes);

            // set popup action
            this.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    // see all items of the combobox
                    int ptr = 0;
                    while (ptr < getItemCount()) {
                        if (getItemAt(ptr) != selectRes) {
                            removeItemAt(ptr);
                        }
                        else {
                            ptr++;
                        }
                    }
                    for (AsioChannel item : selectPool) {
                        addItem(new chanSelWrapper(item));
                    }
                    if (selectRes != emptyChoice) addItem(emptyChoice);
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    // not used
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    // not used
                }
            });

            // set select action
            this.addActionListener(e -> {
                selectRes = (chanSelWrapper) getSelectedItem();
                // notify the listener
                channelChangedListener.ChannelChanged(selectRes.getChannel());
            });
        }
    }

    channelSelectCombo inChannelCombo, outChannelCombo;

    public ChannelSelectPanel(
        String physMgrName,
        AsioChannel inChannel, AsioChannel outChannel,
        ChannelChangedListener inChangedListener, ChannelChangedListener outChangedListener
    ) {
        this.physMgrName = physMgrName;
        this.inChannel = inChannel;
        this.outChannel = outChannel;
        this.inChangedListener = inChangedListener;
        this.outChangedListener = outChangedListener;

        // initialize comboBoxes
        inChannelCombo = new channelSelectCombo(ASIOHost.availableInChannels, inChannel, inChangedListener);
        outChannelCombo = new channelSelectCombo(ASIOHost.availableOutChannels, outChannel, outChangedListener);
        layoutComponents();

        // add to UIHost
        UIHost.channelSelectPanels.add(this);
    }


    private void layoutComponents() {
        // set box layout along Y axis
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // on the top should be the the label of whose channel is being selected
        this.add(new JLabel("Select the channel for " + physMgrName), gbc);

        // then there should be a subpannel, 2 by 2, on the left are labels
        // notifying input or output, on the right are the channel names comboBoxes
        JPanel subPanel = new JPanel();
        subPanel.setLayout(new GridBagLayout());

        // row 1
        // "input" label
        subPanel.add(new JLabel("Input"), gbc);
        // input channel comboBox
        gbc.gridx = 1;
        subPanel.add(inChannelCombo, gbc);

        // row 2
        // "output" label
        gbc.gridx = 0;
        gbc.gridy = 1;
        subPanel.add(new JLabel("Output"), gbc);
        // output channel comboBox
        gbc.gridx = 1;
        subPanel.add(outChannelCombo, gbc);

        // add the subpanel to the main panel
        gbc.gridx = 0;
        gbc.gridy = 1;
        this.add(subPanel, gbc);
    }
}
