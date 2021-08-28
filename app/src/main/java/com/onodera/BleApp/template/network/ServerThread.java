package com.onodera.BleApp.template.network;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.onodera.BleApp.template.network.NetworkConfiguration.MAXIMUM_PACKET_SIZE;

public abstract class ServerThread extends Thread {
    //protected final AtomicBoolean mKeepAlive = new AtomicBoolean(false);
    protected volatile boolean mKeepAlive = false;
    private byte[] mReadBuffer = new byte[MAXIMUM_PACKET_SIZE];

    protected abstract void createSocket();
    protected abstract void waitConnection();
    protected abstract void closeSocket();
    protected abstract int waitForData(byte[] ReadBuffer);
    protected abstract void networkDataReceived(byte[] ReadBuffer);

    private int count = 0;

    @Override
    public void run() {
        //super.run();
        //mKeepAlive.set(true);
        mKeepAlive = true;
        createSocket();
            //waitConnection();
            //mKeepAlive.set(true);
        while(/*mKeepAlive.get()*/mKeepAlive){
            int nData = waitForData(mReadBuffer);


            //count++;
            //if(count>=2000) {

            if(nData>0) {
                int val = mReadBuffer[0] & 0xff;
                Log.d("MyMonitor", "Receive: " + val);
            }

            if(nData>0) {
                networkDataReceived(mReadBuffer);
            }




            //    count = 0;
            //}

        }
        //closeSocket();
    }
}
