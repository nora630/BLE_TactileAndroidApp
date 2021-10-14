package com.onodera.BleApp.template;

import android.util.Log;

public class Adpcm {
    /*
    class ADPCMstate {
        short prevsample;
        int previndex;
    }*/


    //ADPCMstate mstate;

    short prevsample = 0;
    int previndex = 0;


    byte[] IndexTable = {
        -1, -1, -1, -1, 2, 4, 6, 8,
        -1, -1, -1, -1, 2, 4, 6, 8,
    };

    int[] StepSizeTable = {
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
        19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
        50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
        130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
        337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
        876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
        2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
        5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
        15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };
    /*
    public static void initAdpcm(ADPCMstate state){
        state.prevsample = 0;
        state.previndex = 0;
    }*/

    /*
    public Adpcm(){
        mstate.previndex = 0;
        mstate.prevsample = 0;
    }*/

    public byte ADPCMEncoder(short sample){
        int code, diff, step, predsample, diffq, index;

        predsample = prevsample;
        index = previndex;
        step = StepSizeTable[index];

        diff = sample - predsample;
        if (diff>=0)
            code = 0;
        else
        {
            code = 8;
            diff = -diff;
        }

        diffq = step >> 3;
        if (diff >= step)
        {
            code |= 4;
            diff -= step;
            diffq += step;
        }
        step >>= 1;
        if (diff >= step)
        {
            code |= 2;
            diff -= step;
            diffq += step;
        }
        step >>= 1;
        if (diff >= step)
        {
            code |= 1;
            diffq += step;
        }

        if ((code & 8) == 8) {
            predsample -= diffq;
        } else {
            predsample += diffq;
        }

        if (predsample > 32767)
            predsample = 32767;
        else if (predsample < -32767)
            predsample = -32767;

        index += IndexTable[code];

        if (index < 0)
            index = 0;
        if (index > 88)
            index = 88;

        prevsample = (short)predsample;
        previndex = index;

        //Log.d("adpcm", ""+prevsample);

        return (byte) (code & 0x0f);
    }

    public int ADPCMDecoder(byte code){
        int step, predsample, diffq, index;
        predsample = prevsample;
        index = previndex;
        step = StepSizeTable[index];

        diffq = step >> 3;
        if ((code & 4)==4)
            diffq += step;
        if ((code & 2)==2)
            diffq += step >> 1;
        if ((code & 1)==1)
            diffq += step >> 2;

        if ((code & 8)==8)
            predsample -= diffq;
        else
            predsample += diffq;

        if (predsample > 32767)
            predsample = 32767;
        else if (predsample < -32767)
            predsample = -32767;

        index += IndexTable[code];

        if (index < 0)
            index = 0;
        if (index > 88)
            index = 88;

        prevsample = (short)predsample;
        previndex = index;

        Log.d("adpcm", "prevsample="+prevsample+"   previndex="+previndex);

        return(predsample);
    }
}
