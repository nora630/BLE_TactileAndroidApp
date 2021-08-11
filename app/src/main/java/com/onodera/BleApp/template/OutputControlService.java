package com.onodera.BleApp.template;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import com.onodera.BleApp.template.network.UdpServerService;

public class OutputControlService extends Service {

    private  Network mNetwork;
    private Adpcm encodeAdpcm = new Adpcm();
    private Adpcm decodeAdpcm = new Adpcm();
    private int mVolumeScale = 70;
    private boolean activityIsChangingConfiguration;
    private final static int NOTIFICATION_ID = 361;
    private final static int OPEN_ACTIVITY_REQ = 0;

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

        public void setVolumeScale(int volumeScale) { mVolumeScale = volumeScale; }

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

    private Notification createNotification(final int defaults) {
        final Intent parentIntent = new Intent(this, FeaturesActivity.class);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Intent targetIntent = new Intent(this, BleMainActivity.class);


        // both activities above have launchMode="singleTask" in the AndroidManifest.xml file, so if the task is already running, it will be resumed
        final PendingIntent pendingIntent = PendingIntent.getActivities(this, OPEN_ACTIVITY_REQ, new Intent[]{parentIntent, targetIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ToolboxApplication.CONNECTED_DEVICE_CHANNEL);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(getString(R.string.app_name)).setContentText("OutputControlService");
        builder.setSmallIcon(R.drawable.ic_stat_notify_template);
        builder.setShowWhen(defaults != 0).setDefaults(defaults).setAutoCancel(true).setOngoing(true);

        return builder.build();
    }

    private void stopForegroundService(){
        // when the activity rebinds to the service, remove the notification and stop the foreground service
        // on devices running Android 8.0 (Oreo) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            cancelNotification();
        }
    }

    private void cancelNotification() {
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    private void volumeControl(byte[] value) {
        int sample;
        byte code;
        for (int i = 0; i < value.length; i++) {
            sample = decodeAdpcm.ADPCMDecoder((byte) ((value[i] >> 4) & 0x0f));
            sample = (int)(sample * mVolumeScale / 10.0);
            Log.d("MyMonitor", "" + sample);
            code = encodeAdpcm.ADPCMEncoder((short) sample);
            code = (byte) ((code << 4) & 0xf0);

            sample = decodeAdpcm.ADPCMDecoder((byte) ((value[i]) & 0x0f));
            sample = (int)(sample * mVolumeScale / 10.0);
            Log.d("MyMonitor", "" + sample);
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
                        volumeControl(value);
                        final Intent broadcast = new Intent(HapbeatService.BROADCAST_OUTPUT_MEASUREMENT);
                        broadcast.putExtra(HapbeatService.EXTRA_OUTPUT_DATA, value);
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(broadcast);
                    }
                    break;
                case UDP:
                    if (UdpServerService.BROADCAST_NETWORK_MEASUREMENT.equals(action)) {
                        byte[] value = intent.getByteArrayExtra(UdpServerService.NETWORK_DATA);
                        volumeControl(value);
                        final Intent broadcast = new Intent(HapbeatService.BROADCAST_OUTPUT_MEASUREMENT);
                        broadcast.putExtra(HapbeatService.EXTRA_OUTPUT_DATA, value);
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(broadcast);
                    }
                    break;
            }
        }
    };


}
