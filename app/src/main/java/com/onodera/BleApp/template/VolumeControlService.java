package com.onodera.BleApp.template;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.onodera.BleApp.template.network.UdpServerService;

public class VolumeControlService extends Service {

    private  Network mNetwork;
    private Adpcm encodeAdpcm = new Adpcm();
    private Adpcm decodeAdpcm = new Adpcm();

    public enum Network{
        local,
        UDP
    };

    @Override
    public void onCreate(){
        super.onCreate();
        mNetwork = Network.local;
        final IntentFilter filter = new IntentFilter();
        filter.addAction(AccelerometerService.BROADCAST_TEMPLATE_MEASUREMENT);
        filter.addAction(UdpServerService.BROADCAST_NETWORK_MEASUREMENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(intentBroadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        // when user has disconnected from the sensor, we have to cancel the notification that we've created some milliseconds before using unbindService
        //stopForegroundService();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(intentBroadcastReceiver);

        super.onDestroy();
    }

    class LocalBinder extends Binder {
        public void setNetwork(Network network){
            mNetwork = network;
        }

        public Network getNetwork(){
            return mNetwork;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    private void volumeControl(byte[] value) {
        int sample;
        byte code;
        for (int i = 0; i < value.length / 2; i++) {
            sample = decodeAdpcm.ADPCMDecoder((byte) ((value[2 * i] >> 4) & 0x0f));
            code = encodeAdpcm.ADPCMEncoder((short) sample);
            code = (byte) ((code << 4) & 0xf0);

            sample = decodeAdpcm.ADPCMDecoder((byte) ((value[2 * i + 1]) & 0x0f));
            code |= encodeAdpcm.ADPCMEncoder((short) sample);

            value[i] = code;
        }
    }

    private BroadcastReceiver intentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (mNetwork){
                case local:
                    if (AccelerometerService.BROADCAST_TEMPLATE_MEASUREMENT.equals(action)) {
                        byte[] value = intent.getByteArrayExtra(AccelerometerService.EXTRA_DATA);
                        //volumeControl(value);
                        final Intent broadcast = new Intent(HapbeatService.BROADCAST_OUTPUT_MEASUREMENT);
                        broadcast.putExtra(HapbeatService.EXTRA_OUTPUT_DATA, value);
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(broadcast);
                    }
                    break;
                case UDP:
                    if (UdpServerService.BROADCAST_NETWORK_MEASUREMENT.equals(action)) {
                        byte[] value = intent.getByteArrayExtra(UdpServerService.NETWORK_DATA);
                        //volumeControl(value);
                        final Intent broadcast = new Intent(HapbeatService.BROADCAST_OUTPUT_MEASUREMENT);
                        broadcast.putExtra(HapbeatService.EXTRA_OUTPUT_DATA, value);
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(broadcast);
                    }
                    break;
            }
        }
    };


}
