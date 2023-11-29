package com.AcousticNFC.utils;

import java.io.FileWriter;
import java.io.IOException;

public class Recorder {

    private float[] recordings; // the recordings

    public Recorder() {
        super();

        recordings = new float[0];
    }

    public void record(float[] input) {
        // add the new samples to the recordings
        float[] newRecordings = new float[recordings.length + input.length];
        System.arraycopy(recordings, 0, newRecordings, 0, recordings.length);
        System.arraycopy(input, 0, newRecordings, recordings.length, input.length);
        recordings = newRecordings;
    }

    public float[] getRecordings() {
        return recordings;
    }

    public void outputRecordings(String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName + ".csv");
            for (int i = 0; i < recordings.length; i++) {
                writer.append(Float.toString(recordings[i]));
                writer.append('\n'); // new line
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
