package com.onodera.BleApp.template;

public class HighPassFilter {
    private float in1 = 0, out1 = 0;
    private float a0 = 1, a1 = -0.9969f, b0 = 0.9984f, b1 = -0.9984f;

    private float out2 = 0;
    private float[] in2 = new float[60];
    private float[] h = {
            -0.000849535f,
            -0.000807708f,
            -0.000602236f,
            -0.000189219f,
            0.000453489f,
            0.001276167f,
            0.002119018f,
            0.002710854f,
            0.002720255f,
            0.001856907f,
            -5.13707E-18f,
            -0.002686326f,
            -0.005696649f,
            -0.008224682f,
            -0.009315167f,
            -0.008109611f,
            -0.004135286f,
            0.002436758f,
            0.010644579f,
            0.018762651f,
            0.024530323f,
            0.02555879f,
            0.01984425f,
            0.00627827f,
            -0.014966108f,
            -0.042276832f,
            -0.072733513f,
            -0.102523108f,
            -0.127571168f,
            -0.14426585f,
            0.85070962f,
            -0.14426585f,
            -0.127571168f,
            -0.102523108f,
            -0.072733513f,
            -0.042276832f,
            -0.014966108f,
            0.00627827f,
            0.01984425f,
            0.02555879f,
            0.024530323f,
            0.018762651f,
            0.010644579f,
            0.002436758f,
            -0.004135286f,
            -0.008109611f,
            -0.009315167f,
            -0.008224682f,
            -0.005696649f,
            -0.002686326f,
            -5.13707E-18f,
            0.001856907f,
            0.002720255f,
            0.002710854f,
            0.002119018f,
            0.001276167f,
            0.000453489f,
            -0.000189219f,
            -0.000602236f,
            -0.000807708f,
            -0.000849535f,
    };

    public float butterworthFilter(float input) {
        float output;
        output = b0/a0 * (float)input + b1/a0 * in1 - a1/a0 * out1;
        in1 = input;
        out1 = output;
        return output;
    }

    public float firFilter(float input) {
        float output = 0;
        for (int i=0; i<h.length; i++){
            if(i==0) output += h[i] * input;
            else output += h[i] * in2[h.length-1-i];
        }
        float tmp1 = 0, tmp2 = 0;
        for (int i=0; i<h.length-1; i++){
            if(i==0){
                tmp1 = in2[h.length-2];
                in2[h.length-2] = input;
            } else {
                tmp2 = in2[h.length-2-i];
                in2[h.length-2-i] = tmp1;
                tmp1 = tmp2;
            }
        }
        return output;
    }
}
