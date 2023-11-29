package com.AcousticNFC.utils;

import java.util.ArrayList;

public class Player {
    
    private ArrayList<Float> playerBuffer;
    private int playerBufferIndex;

    /**
     * construct the player object
     * @param playerBuffer
     * the buffer to play, won't be modified the buffer
     */
    public Player(float[] playerBuffer) {
        this.playerBuffer = TypeConvertion.floatArr2FloatList(playerBuffer);
        this.playerBufferIndex = 0;
    }

    /**
     * construct the player object<p>
     * note that the playerBuffer won't be copied and may be deleted
     * @param playerBuffer
     */
    public Player(ArrayList<Float> playerBuffer) {
        this.playerBuffer = playerBuffer;
        this.playerBufferIndex = 0;
    }

    /**
     * construct an empty player object
     */
    public Player() {
        this.playerBuffer = new ArrayList<>();
        this.playerBufferIndex = 0;
    }

    public void clear() {
        playerBuffer = new ArrayList<>();
        playerBufferIndex = 0;
    }

    public boolean empty() {
        return playerBufferIndex == playerBuffer.size();
    }

    public void addContent(ArrayList<Float> content) {
        playerBuffer.addAll(content);
    }

    /**
     * feed BUFFERSIZE samples into OUTBUFFER, padded with 0 if not enough<p>
     * will report if this is the last play, and clear the player if it is<p>
     * @param bufferSize
     * @param outBuffer
     * @return true if this is not the last play, false otherwise
     */
    public boolean playContent(int bufferSize, float[] outBuffer) {
        // get the content to play
        // check if it's the last buffer to play
        if (playerBufferIndex + bufferSize >= playerBuffer.size()) {
            // copy the remaining content
            for (int i = 0; i < playerBuffer.size() - playerBufferIndex; i++) {
                outBuffer[i] = playerBuffer.get(playerBufferIndex + i);
            }
            // fill the rest of the buffer with 0
            for (int i = playerBuffer.size() - playerBufferIndex; i < bufferSize; i++) {
                outBuffer[i] = 0;
            }
            // update the index
            playerBufferIndex = playerBuffer.size();
            // now we can safely clear the player
            clear();
            // return false to indicate that this call handles the last buffer
            return false;
        }
        else {
            // copy the content
            for (int i = 0; i < bufferSize; i++) {
                outBuffer[i] = playerBuffer.get(playerBufferIndex + i);
            }
            // update the index
            playerBufferIndex += bufferSize;
            // return true to indicate that this call doesn't handle the last buffer
            return true;
        }
    }

    public int getBufferIndex() {
        return playerBufferIndex;
    }

    public int getBufferLength() {
        return playerBuffer.size();
    }
}
