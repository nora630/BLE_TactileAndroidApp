package com.onodera.BleApp.template;

public class LowPassFilter {
    private int[] in1 = new int[48];
    private float[] h = {
            -0.000132991f,
            0.000288904f,
            -0.000509619f,
            0.000839925f,
            -0.001325005f,
            0.002007747f,
            -0.002926044f,
            0.004110207f,
            -0.005580665f,
            0.007346054f,
            -0.009401836f,
            0.011729532f,
            -0.01429662f,
            0.017057139f,
            -0.019952983f,
            0.022915831f,
            -0.025869651f,
            0.028733638f,
            -0.031425484f,
            0.033864788f,
            -0.035976476f,
            0.037694031f,
            -0.038962401f,
            0.039740419f,
            0.960063114f,
            0.039740419f,
            -0.038962401f,
            0.037694031f,
            -0.035976476f,
            0.033864788f,
            -0.031425484f,
            0.028733638f,
            -0.025869651f,
            0.022915831f,
            -0.019952983f,
            0.017057139f,
            -0.01429662f,
            0.011729532f,
            -0.009401836f,
            0.007346054f,
            -0.005580665f,
            0.004110207f,
            -0.002926044f,
            0.002007747f,
            -0.001325005f,
            0.000839925f,
            -0.000509619f,
            0.000288904f,
            -0.000132991f,
    };

    public  int filter(int input) {
        float output = 0;
        for (int i=0; i<h.length; i++){
            if(i==0) output += h[i] * input;
            else output += h[i] * in1[h.length-1-i];
        }
        int tmp1 = 0, tmp2 = 0;
        for (int i=0; i<h.length-1; i++){
            if(i==0){
                tmp1 = in1[h.length-2];
                in1[h.length-2] = input;
            } else {
                tmp2 = in1[h.length-2-i];
                in1[h.length-2-i] = tmp1;
                tmp1 = tmp2;
            }
        }
        return (int)output;
    }
}