//
// Created by sho_o on 2021/10/16.
//

#ifndef ANDROID_NRF_TOOLBOX_ADPCM_H
#define ANDROID_NRF_TOOLBOX_ADPCM_H

struct ADPCMstate {
    short prevsample;   /* Predicted sample */
    int previndex;    /* Index into step size table */
};

/* Function prototype for the ADPCM Encoder routine */
char ADPCMEncoder(short, struct ADPCMstate *);

/* Function prototype for the ADPCM Decoder routine */
int ADPCMDecoder(char, struct ADPCMstate *);


#endif //ANDROID_NRF_TOOLBOX_ADPCM_H
