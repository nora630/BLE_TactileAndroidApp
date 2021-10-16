package com.onodera.BleApp.template;

public class HighPassFilter {
    private float in1 = 0, out1 = 0;
    private float a0 = 1, a1 = -0.9969f, b0 = 0.9984f, b1 = -0.9984f;

    private float out2 = 0;
    private float[] in2 = new float[60];
    private float[] h = {
            -9.46041E-18f,
            -0.000279857f,
            -0.000601655f,
            -0.000977619f,
            -0.001394346f,
            -0.001803029f,
            -0.00211697f,
            -0.002218319f,
            -0.001974471f,
            -0.001262717f,
            -1.08745E-17f,
            0.001826731f,
            0.004134858f,
            0.00673034f,
            0.009306166f,
            0.011457641f,
            0.012714805f,
            0.01258977f,
            0.010634294f,
            0.006500936f,
            -3.30132E-17f,
            -0.008855681f,
            -0.019825076f,
            -0.032437351f,
            -0.04601644f,
            -0.059730701f,
            -0.072663236f,
            -0.083895691f,
            -0.092596324f,
            -0.098102328f,
            0.899881042f,
            -0.098102328f,
            -0.092596324f,
            -0.083895691f,
            -0.072663236f,
            -0.059730701f,
            -0.04601644f,
            -0.032437351f,
            -0.019825076f,
            -0.008855681f,
            -3.30132E-17f,
            0.006500936f,
            0.010634294f,
            0.01258977f,
            0.012714805f,
            0.011457641f,
            0.009306166f,
            0.00673034f,
            0.004134858f,
            0.001826731f,
            -1.08745E-17f,
            -0.001262717f,
            -0.001974471f,
            -0.002218319f,
            -0.00211697f,
            -0.001803029f,
            -0.001394346f,
            -0.000977619f,
            -0.000601655f,
            -0.000279857f,
            -9.46041E-18f,

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
