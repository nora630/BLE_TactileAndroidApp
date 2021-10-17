package com.onodera.BleApp.template.network;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.onodera.BleApp.template.network.NetworkConfiguration.MAXIMUM_PACKET_SIZE;

public abstract class ServerThread extends Thread {
    //protected final AtomicBoolean mKeepAlive = new AtomicBoolean(false);
    protected volatile boolean mKeepAlive = false;
    private byte[] mReadBuffer = new byte[MAXIMUM_PACKET_SIZE];
    private final int DATA_SEND_INTERVAL = 2;
    protected Object mQueueMutex = new Object();
    protected ArrayDeque<Byte> mDataQueue = new ArrayDeque<>(4096);

    protected abstract void createSocket();
    protected abstract void waitConnection();
    protected abstract void closeSocket();
    protected abstract int waitForData(byte[] ReadBuffer);
    protected abstract void networkDataReceived(byte[] ReadBuffer);

    private int count = 0;

    protected ServerListener mListener;

    public interface ServerListener {
        void onServerToHapbeatSend(byte[] value);
    }

    @Override
    public void run() {
        //super.run();
        //mKeepAlive.set(true);
        mKeepAlive = true;
        createSocket();
            //waitConnection();
            //mKeepAlive.set(true);

        int nData = 0;
        while(/*mKeepAlive.get()*/mKeepAlive){
            nData = waitForData(mReadBuffer);


            //count++;
            //if(count>=2000) {
            /*

            if(nData>0) {
                int val = mReadBuffer[0] & 0xff;
                Log.d("MyMonitor", "Receive: " + val);
            } */

            if(nData>0&&mListener!=null) {
                //networkDataReceived(mReadBuffer);
                mListener.onServerToHapbeatSend(mReadBuffer);
                //nData = 0;
            }

            try {
                Thread.sleep(DATA_SEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }




            //    count = 0;
            //}

        }
        //closeSocket();
    }
}
