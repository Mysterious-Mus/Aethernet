package com.aethernet.ASIO;

// AsioDriverListener
import com.synthbot.jasiohost.AsioDriverListener;
import com.synthbot.jasiohost.AsioDriverState;
import com.synthbot.jasiohost.AsioSampleType;
import com.aethernet.config.L2Config;
import com.aethernet.config.L2Config.ConfigTerm;
import com.aethernet.utils.CyclicBuffer;
import com.aethernet.utils.Player;
import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;

// ArrayList
import java.util.HashSet;
import java.util.Set;
// Map
import java.util.Map;
import java.nio.channels.AsynchronousFileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class ASIOHost implements AsioDriverListener{

    public static class Configs {
        public static ConfigTerm<Double> sampleRate = new ConfigTerm<Double>("sampleRate", (double)44100, false);
        public static ConfigTerm<Integer> BUFFER_SIZE = new ConfigTerm<Integer>("BUFFER_SIZE", 256, false)
        {
            @Override
            public boolean newValCheck(Integer newVal) {
                // report if the newval is not what we get from asiodriver
                if (newVal != asioDriver.getBufferPreferredSize()) {
                    System.out.println("Warning: buffer size is not " + newVal + 
                        ". System will probably fail. Please reset and reboot");
                }
                return true;
            }
        };
        public static ConfigTerm<Boolean> allowChannelMultiAssign =
            new ConfigTerm<Boolean>("allowChannelMultiAssign", true, false);
    }

    private static AsioDriver asioDriver;
    private Set<AsioChannel> activeChannels = new HashSet<AsioChannel>();
    
    private int bufferSize;

    private static Map<AsioChannel, Player> playChannels = new HashMap<>();

    public interface NewBufferListener {
        public void handleNewBuffer(float[] newbuffer);
    }

	private static Map<AsioChannel, Set<NewBufferListener>> receiveChannels = new HashMap<>();

	public static Set<AsioChannel> availableInChannels = new HashSet<>();
	public static Set<AsioChannel> availableOutChannels = new HashSet<>();

    /**
     * a map from channel to the semaphore that is used to wait for the channel to complete transmitting
     */
    private static Map<AsioChannel, Semaphore> channelWaiters = new HashMap<>();

	/**
	 * construct the ASIO host<p>
	 * only one ASIO host should be allowed<p>
	 * 
	 * statically: <p>
	 * the physical layer can register contents to play by channel name<p>
	 * the physical layer can register receive task by registering the name along with the cyclicbuffer to feed<p>
     * to properly select channel names, each physicalManager would maintain the unselected channel names,
     * which is a statical Set
	 */
    public ASIOHost() {
        driverInit();
    }

    /**
     * register a player to play content to the channel <p>
     * @param channel
     * @param content
     */
	public static void registerPlayer(AsioChannel channel) {
        if (channel == null) {
            return;
        }
        // sanity check
        if (!availableOutChannels.contains(channel)) {
            System.out.println("Channel " + channel.toString() + " is not available.");
            return;
        }
        // assign an empty player to the channel
        playChannels.put(channel, new Player());
        // maintain the available list
        if (!Configs.allowChannelMultiAssign.v())
            availableOutChannels.remove(channel);
	}

    /**
     * play content to the channel
     * @param channel
     * @param content
     */
    public static void play(AsioChannel channel, ArrayList<Float> content) {
        if (channel == null) return;
        // sanity check
        if (!playChannels.containsKey(channel)) {
            System.out.println("Channel " + channel + " is not registered.");
            return;
        }
        // get the player
        Player player = playChannels.get(channel);
        // print warning message if the player is not empty
        if (!player.empty()) {
            System.out.println("Warning: channel " + channel + " is not empty.");
        }
        // add the content to the player
        player.addContent(content);
    }

    public static void unregisterPlayer(AsioChannel channel) {
        if (channel == null) return;
        // playChannels.remove(channel);
        // maintain the available list
        if (!Configs.allowChannelMultiAssign.v())
            availableOutChannels.add(channel);
    }

    /**
     * register a cyclic buffer to receive data from the channel
     * @param channel
     * @param listener
     */
	public static void registerReceiver(AsioChannel channel, NewBufferListener listener) {
        if (channel == null) return;
        if (receiveChannels.get(channel) == null)
            receiveChannels.put(channel, new HashSet<>(Collections.singleton(listener)));
        else {
            receiveChannels.get(channel).add(listener);
        }
        // maintain the available list
        if (!Configs.allowChannelMultiAssign.v())
            availableInChannels.remove(channel);
	}

    /**
     * unregister a player
     * @param channel
     */
    public static void unregisterReceiver(AsioChannel channel, NewBufferListener listener) {
        if (channel == null) return;
        receiveChannels.get(channel).remove(listener);
        // maintain the available list
        if (!Configs.allowChannelMultiAssign.v())
            availableInChannels.add(channel);
    }

    /**
     * wait for the channel to complete transmitting
     * @param channel
     */
    public synchronized static void waitTransmit(AsioChannel channel) {
        // if the channel is not registered, print log
        if (!playChannels.containsKey(channel)) {
            System.out.println("Warning: channel " + channel + " is not registered.");
            return;
        }

        // if the channel is already empty, do nothing
        if (playChannels.get(channel).empty()) return;

        // now the channel has something to play, give it a semaphore and down
        channelWaiters.put(channel, new Semaphore(0));
        try {
            channelWaiters.get(channel).acquire();
        }
        catch (InterruptedException e) {
            System.out.println("Warning: channel " + channel + " is interrupted.");
        }
    }

    public synchronized void bufferSwitch(long systemTime, long samplePosition, Set<AsioChannel> channels) {
		// Create a copy of the keySet because we are removing during the loop
		Set<AsioChannel> keys = new HashSet<>(playChannels.keySet());
		// look up all the play channels
		for (AsioChannel channel : keys) {
			// get the player
            if (channel == null) continue;
			Player player = playChannels.get(channel);
            if (player.empty()) {
                // up the waiter if exists
                if (channelWaiters.containsKey(channel)) {
                    channelWaiters.get(channel).release();
                    channelWaiters.remove(channel);
                }
            }
			// check if the channel exists
			if (channel == null || !channels.contains(channel)) {
				// report channel not found
				System.out.println("Channel " + channel + " not found.");
				// remove the registration
                unregisterPlayer(channel);
			}
			else {
				// get the float[] from the player
				float[] content = new float[bufferSize];
				boolean notLastBuffer = player.playContent(bufferSize, content);
                // if last buffer, up the waiter
                if (!notLastBuffer && channelWaiters.containsKey(channel)) {
                    channelWaiters.get(channel).release();
                    channelWaiters.remove(channel);
                }
				// copy the content to the channel
				channel.write(content);
			}
		}
		
		// look up all receiving channels
		for (AsioChannel channel : receiveChannels.keySet()) {
			// check if the channel exists
            if (channel == null) continue;
            // read the content from the channel
            float[] content = new float[bufferSize];
            channel.read(content);
            for (NewBufferListener listener : receiveChannels.get(channel)) {
                // invoke the listener
                listener.handleNewBuffer(content);
            }
		}
    }

    public void driverInit() {
        try {
            asioDriver = AsioDriver.getDriver("ASIO4ALL v2");
        }
        catch (Exception e) {
            System.out.println("ASIO driver not found.");
            // shutdown program
            System.exit(0);
            return;
        }
        asioDriver.addAsioDriverListener(this);
    
        // clear channels
        activeChannels.clear();
        availableInChannels.clear();
        availableOutChannels.clear();
        // activate output channels
        for (int i = 0; i < asioDriver.getNumChannelsOutput(); i++)
        {
            AsioChannel asioChannel = asioDriver.getChannelOutput(i);
            activeChannels.add(asioChannel);
            availableOutChannels.add(asioChannel);
        }
		
        // activate input channels
        for (int i = 0; i < asioDriver.getNumChannelsInput(); i++)
        {
			AsioChannel asioChannel = asioDriver.getChannelInput(i);
            activeChannels.add(asioChannel);
            availableInChannels.add(asioChannel);
        }

        bufferSize = asioDriver.getBufferPreferredSize();
        // report to the user if the buffer size is not as set
        if (bufferSize != Configs.BUFFER_SIZE.v()) {
            System.out.println("Warning: buffer size is not " + Configs.BUFFER_SIZE.value2Str() + 
                ". System will probably fail. Please reset and reboot");
        }
        double sampleRate = asioDriver.getSampleRate();
        // if sample rate is not 44100, throw warning
        if (Math.abs(sampleRate - Configs.sampleRate.v()) > 1e-6) {
          System.out.println("Warning: sample rate is not 44100. System will probably fail.");
        }
        asioDriver.createBuffers(activeChannels);
        asioDriver.start();
    }
    
    public void driverShutdown() {
        if (asioDriver != null) {
            asioDriver.shutdownAndUnloadDriver();
            activeChannels.clear();
            asioDriver = null;
        }
    }

    public static void openControlPanel() {
        if (asioDriver != null && 
            asioDriver.getCurrentState().ordinal() >= AsioDriverState.INITIALIZED.ordinal()) {
            asioDriver.openControlPanel();          
        }
    }

    public void bufferSizeChanged(int bufferSize) {
        System.out.println("bufferSizeChanged() callback received.");
    }

    public void latenciesChanged(int inputLatency, int outputLatency) {
        System.out.println("latenciesChanged() callback received.");
    }

    public void resetRequest() {
        /*
        * This thread will attempt to shut down the ASIO driver. However, it will
        * block on the AsioDriver object at least until the current method has returned.
        */
        new Thread() {
        @Override
        public void run() {
            System.out.println("resetRequest() callback received. Returning driver to INITIALIZED state.");
            asioDriver.returnToState(AsioDriverState.INITIALIZED);

            // reboot
            driverShutdown();
            driverInit();
        }
        }.start();
    }

    public void resyncRequest() {
        System.out.println("resyncRequest() callback received.");
    }

    public void sampleRateDidChange(double sampleRate) {
        System.out.println("sampleRateDidChange() callback received.");
    }
}
