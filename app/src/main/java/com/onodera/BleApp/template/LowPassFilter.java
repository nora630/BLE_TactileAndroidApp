package com.onodera.BleApp.template;

public class LowPassFilter {
    private int[] in1 = new int[48];
    private float[] h = {
            0.001041157f,
            -0.001078934f,
            0.001167567f,
            -0.001269535f,
            0.001323542f,
            -0.001247414f,
            0.000943364f,
            -0.000305328f,
            -0.000772159f,
            0.002384243f,
            -0.004605954f,
            0.007483199f,
            -0.011025332f,
            0.01520002f,
            -0.019930958f,
            0.025098761f,
            -0.030545128f,
            0.036080105f,
            -0.041492019f,
            0.046559433f,
            -0.051064288f,
            0.054805303f,
            -0.057610644f,
            0.059348923f,
            0.939024151f,
            0.059348923f,
            -0.057610644f,
            0.054805303f,
            -0.051064288f,
            0.046559433f,
            -0.041492019f,
            0.036080105f,
            -0.030545128f,
            0.025098761f,
            -0.019930958f,
            0.01520002f,
            -0.011025332f,
            0.007483199f,
            -0.004605954f,
            0.002384243f,
            -0.000772159f,
            -0.000305328f,
            0.000943364f,
            -0.001247414f,
            0.001323542f,
            -0.001269535f,
            0.001167567f,
            -0.001078934f,
            0.001041157f,

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
