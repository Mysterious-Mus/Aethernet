package com.AcousticNFC.utils;

public class Highpass {
    private float previousInput = 0.0f;
    private float previousOutput = 0.0f;
    private float cutoffFrequency = 4000f;
    private float alpha;

    public Highpass(float sampleRate) {
        alpha = (float) (1 / (1 + (sampleRate / (2 * Math.PI * cutoffFrequency))));
    }

    public float filter(float input) {
        float output = alpha * (previousOutput + input - previousInput);
        previousInput = input;
        previousOutput = output;
        return output;
    }
}
// END: highpassfilterclass