package com.onodera.BleApp.template.network;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import com.onodera.BleApp.template.BleMainActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import static com.onodera.BleApp.template.network.NetworkConfiguration.MAXIMUM_PACKET_SIZE;
import static com.onodera.BleApp.template.network.NetworkConfiguration.UDP_PORT;

public class UdpServerService extends Service {
    public static final String BROADCAST_NETWORK_MEASUREMENT = "com.onodera.BleApp.template.BROADCAST_NETWORK_MEASUREMENT";
    public static final String NETWORK_DATA = "com.onodera.bleApp.template.NETWORK_DATA";
    private volatile UdpServerThread mServerThread;
    private boolean activityIsChangingConfiguration;
    private final static int NOTIFICATION_ID = 643;
    private final static int OPEN_ACTIVITY_REQ = 0;
    private final static int DISCONNECT_REQ = 1;

    public class UdpServerThread extends ServerThread {
        private DatagramSocket mSocket;
        private byte[] mReceiveBuffer = new byte[MAXIMUM_PACKET_SIZE];
        private DatagramPacket mPacket = new DatagramPacket(mReceiveBuffer, mReceiveBuffer.length);

        @Override
        protected void createSocket() {
            boolean isSocketCreated = false;
            while (/*mKeepAlive.get()*/mKeepAlive && !isSocketCreated) {
                try {
                    mSocket = new DatagramSocket(null);
                } catch (SocketException e) {
                    e.printStackTrace();
                    break;
                }
                try {
                    mSocket.bind(new InetSocketAddress(UDP_PORT));
                } catch (SocketException e) {
                    e.printStackTrace();
                    break;
                }
                isSocketCreated = true;
            }
            Log.d("MyMonitor", "Create Server Socket!");
        }

        @Override
        protected void waitConnection() {

        }

        @Override
        protected void closeSocket() {
            if(mSocket != null && !mSocket.isClosed()) {
                mSocket.close();
                //Log.d("MyMonitor", "Socket close!");
            }
            mSocket = null;
        }

        @Override
        protected int waitForData(byte[] ReadBuffer) {
            int nData = 0;
            try {
                mSocket.receive(mPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            nData = mPacket.getLength();
            for(int i=0; i<nData; i++){
                ReadBuffer[i] = (byte) (mReceiveBuffer[i] & 0xff);
            }
            return nData;
        }

        @Override
        protected void networkDataReceived(byte[] ReadBuffer) {
            final Intent broadcast = new Intent(BROADCAST_NETWORK_MEASUREMENT);
            broadcast.putExtra(NETWORK_DATA, ReadBuffer);
            LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(broadcast);
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
            mServerThread = new UdpServerThread();
            mServerThread.start();
            //Log.d("MyMonitor", "Server Start!");
        return START_REDELIVER_INTENT;
    }

    protected void stopService() {
        stopSelf();
    }

    public class LocalBinder extends Binder {
        public final void disconnect() {
            //mServerThread.mKeepAlive.set(false);
            mServerThread.mKeepAlive = false;
            /*
            if(mServerThread.mSocket!=null)
                mServerThread.mSocket.close();
            mServerThread.mSocket = null;

             */
            stopService();
        }
        public void setActivityIsChangingConfiguration(final boolean changing) {
            activityIsChangingConfiguration = changing;
        }
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
        super.onDestroy();
        /*
        mServerThread.mKeepAlive.set(false);
        if(mServerThread.mSocket!=null)
            mServerThread.mSocket.close();
        mServerThread = null;

         */
        mServerThread.mKeepAlive = false;
        mServerThread.closeSocket();
        mServerThread = null;
        //Log.d("MyMonitor", "Server finish!");
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
        builder.setContentTitle(getString(R.string.app_name)).setContentText("UDPService");
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
