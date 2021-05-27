package com.onodera.BleApp.template.network;

import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.onodera.BleApp.template.network.NetworkConfiguration.MAXIMUM_PACKET_SIZE;

public abstract class ClientThread extends Thread {

    protected volatile boolean mKeepAlive;
    protected InetAddress mInetAddress;
    private String mIpAddress;
    private final int DATA_SEND_INTERVAL = 5;
    private Object mQueueMutex = new Object();
    private ArrayDeque<Byte> mDataQueue = new ArrayDeque<>();

    protected abstract void onResolveIP(InetAddress ipAddress);
    protected abstract boolean createConnection();
    protected abstract void sendData(byte[] sendDataBuffer, int nData);
    protected abstract void closeConection();

    protected AtomicBoolean mIsNetworkConnected = new AtomicBoolean(false);
    private byte[] sendDataBuffer;

    public void setIpAddress(String IpAddress) {this.mIpAddress = IpAddress;}

    @Override
    public void run() {
        super.run();
        mKeepAlive = true;

        //mDataQueue.add((byte)12);

        try {

            //mIpAddress = "192.168.11.9";

            mInetAddress = InetAddress.getByName(mIpAddress);
            onResolveIP(mInetAddress);

        } catch (UnknownHostException e) {
            e.printStackTrace();
            mKeepAlive = false;
        }

        while(mKeepAlive) {
            boolean isConnected = createConnection();
            if(!isConnected) {
                Log.d("MyMonitor", "Cannot Connect To : " + mIpAddress);
                continue;
            }
            //mIsNetworkConnected.set(isConnected);

            while (mIsNetworkConnected.get() && mKeepAlive) {
                try {
                    Thread.sleep(DATA_SEND_INTERVAL);
                    //mDataQueue.add((byte)12);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int nData = 0;

                synchronized (mQueueMutex) {
                    nData = Math.min(mDataQueue.size(), MAXIMUM_PACKET_SIZE);
                    if (nData==0)
                        continue;
                    sendDataBuffer = new byte[nData];
                    for (int i=0; i<nData; i++){
                        sendDataBuffer[i] = mDataQueue.pop();
                    }
                }
                if (nData>0){
                    sendData(sendDataBuffer, nData);
                    //Log.d("MyMonitor", "send!");
                }
            }
        }
        //closeConection();
    }

    public void addDataToSendQueue(byte[] data, int size){
        synchronized (mQueueMutex){
            for(int i=0; i<size; i++)
                mDataQueue.add(data[i]);
        }
    }
}
