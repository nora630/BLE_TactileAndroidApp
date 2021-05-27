package com.onodera.BleApp.template.network;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.onodera.BleApp.FeaturesActivity;
import com.onodera.BleApp.R;
import com.onodera.BleApp.ToolboxApplication;
import com.onodera.BleApp.template.AccelerometerService;
import com.onodera.BleApp.template.BleMainActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static com.onodera.BleApp.template.network.NetworkConfiguration.MAXIMUM_PACKET_SIZE;
import static com.onodera.BleApp.template.network.NetworkConfiguration.UDP_PORT;

public class UdpClientService extends Service {
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final  String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(AccelerometerService.EXTRA_DEVICE);
            if (AccelerometerService.BROADCAST_TEMPLATE_MEASUREMENT.equals(action)) {
                byte[] value = intent.getByteArrayExtra(AccelerometerService.EXTRA_DATA);
                mClientThread.addDataToSendQueue(value, value.length);

                //int intValue = value[0] & 0xff;
                //Log.d("MyMonitor", String.valueOf(intValue));
            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AccelerometerService.BROADCAST_TEMPLATE_MEASUREMENT);
        return intentFilter;
    }

    public class UdpClientThread extends ClientThread{
        private DatagramSocket mSocket;
        private DatagramPacket mPacket;
        private byte[] mSendBuffer = new byte[MAXIMUM_PACKET_SIZE];

        public UdpClientThread(){
            mPacket = new DatagramPacket(mSendBuffer, MAXIMUM_PACKET_SIZE);
            mPacket.setPort(UDP_PORT);
        }

        @Override
        protected void onResolveIP(InetAddress ipAddress) {
            mPacket.setAddress(ipAddress);
        }

        @Override
        protected boolean createConnection() {
            mIsNetworkConnected.set(false);
            try {
                mSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            Log.d("MyMonitor", "Client connected to server at " + mPacket.getAddress());
            /* notify server the start of the connection */
            /*
            mPacket.setLength(0);
            try {
                mSocket.send(mPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }

             */
            mIsNetworkConnected.set(true);
            return mIsNetworkConnected.get();
        }

        @Override
        protected void sendData(byte[] sendDataBuffer, int nData) {
            for (int i=0; i<nData; i++){
                mSendBuffer[i] = sendDataBuffer[i];
            }
            mPacket.setLength(nData);
            try {
                mSocket.send(mPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void closeConection() {
            mIsNetworkConnected.set(false);
            Log.d("MyMonitor", "Socket Close");

            if(mSocket != null && !mSocket.isClosed())
                mSocket.close();

            mSocket = null;
        }

        /*
        @Override
        protected void closeConection() {
            mIsNetworkConnected.set(false);
            if(mSocket!=null)
                mSocket.close();
            mSocket = null;
        }

         */
    }

    protected void stopService() {
        stopSelf();
    }

    private volatile UdpClientThread mClientThread;
    private boolean activityIsChangingConfiguration;
    protected boolean bound;
    private final static int NOTIFICATION_ID = 754;
    private final static int OPEN_ACTIVITY_REQ = 0;
    private final static int DISCONNECT_REQ = 1;

    public class LocalBinder extends Binder{
            public final void disconnect() {
                mClientThread.mIsNetworkConnected.set(false);
                mClientThread.mKeepAlive = false;
                /*
                if(mClientThread.mSocket!=null)
                    mClientThread.mSocket.close();
                mClientThread.mSocket = null;
                */
                stopService();
        }
            public void setActivityIsChangingConfiguration(final boolean changing) {
                activityIsChangingConfiguration = changing;
            }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId){
        mClientThread = new UdpClientThread();
        mClientThread.start();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeIntentFilter());
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        //registerReceiver(broadcastReceiver, makeIntentFilter());
        //LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeIntentFilter());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public final void onRebind(final Intent intent) {
        //bound = true;

        if (!activityIsChangingConfiguration)
            stopForegroundService();
    }

    @Override
    public final boolean onUnbind(final Intent intent) {
        //bound = false;

        if (!activityIsChangingConfiguration)
            startForegroundService();

        // We want the onRebind method be called if anything else binds to it again
        return true;
    }

    @Override
    public void onDestroy() {
        stopForegroundService();
        //unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
        /*
        mClientThread.mIsNetworkConnected.set(false);
        mClientThread.mKeepAlive = false;
        if(mClientThread.mSocket!=null)
            mClientThread.mSocket.close();
        mClientThread = null;

         */
        mClientThread.mKeepAlive = false;
        mClientThread.closeConection();
        mClientThread = null;

    }

    /**
     * Sets the service as a foreground service
     */
    private void startForegroundService(){
        // when the activity closes we need to show the notification that user is connected to the peripheral sensor
        // We start the service as a foreground service as Android 8.0 (Oreo) onwards kills any running background services
        final Notification notification = createNotification(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Stops the service as a foreground service
     */
    private void stopForegroundService(){
        // when the activity rebinds to the service, remove the notification and stop the foreground service
        // on devices running Android 8.0 (Oreo) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            cancelNotification();
        }
    }

    private Notification createNotification(final int defaults) {
        final Intent parentIntent = new Intent(this, FeaturesActivity.class);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Intent targetIntent = new Intent(this, BleMainActivity.class);


        // both activities above have launchMode="singleTask" in the AndroidManifest.xml file, so if the task is already running, it will be resumed
        final PendingIntent pendingIntent = PendingIntent.getActivities(this, OPEN_ACTIVITY_REQ, new Intent[]{parentIntent, targetIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ToolboxApplication.CONNECTED_DEVICE_CHANNEL);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(getString(R.string.app_name)).setContentText("UDPClient");
        builder.setSmallIcon(R.drawable.ic_stat_notify_template);
        builder.setShowWhen(defaults != 0).setDefaults(defaults).setAutoCancel(true).setOngoing(true);

        return builder.build();
    }
    /**
     * Cancels the existing notification. If there is no active notification this method does nothing
     */
    private void cancelNotification() {
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }
}
