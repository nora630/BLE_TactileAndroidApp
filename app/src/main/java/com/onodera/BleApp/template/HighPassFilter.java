package com.onodera.BleApp.template;

public class HighPassFilter {
    private float in1 = 0, out1 = 0;
    private float a0 = 1, a1 = -0.9969f, b0 = 0.9984f, b1 = -0.9984f;

    public int filter(int input) {
        float output;
        output = b0/a0 * (float)input + b1/a0 * in1 - a1/a0 * out1;
        in1 = (float)input;
        out1 = output;
        return (int)output;
    }
}
