/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.onodera.BleApp.template;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import no.nordicsemi.android.log.Logger;
import com.onodera.BleApp.FeaturesActivity;
import com.onodera.BleApp.R;
import com.onodera.BleApp.ToolboxApplication;
import com.onodera.BleApp.profile.BleProfileService;
import com.onodera.BleApp.profile.LoggableBleManager;

public class AccelerometerService extends BleProfileService implements AccelerometerManagerCallbacks {
    public static final String BROADCAST_TEMPLATE_MEASUREMENT = "com.onodera.BleApp.template.BROADCAST_MEASUREMENT";
    public static final String EXTRA_DATA = "com.onodera.BleApp.template.EXTRA_DATA";

    public static final String BROADCAST_BATTERY_LEVEL = "com.onodera.BleApp.BROADCAST_BATTERY_LEVEL";
    public static final String EXTRA_BATTERY_LEVEL = "com.onodera.BleApp.EXTRA_BATTERY_LEVEL";

    private final static String ACTION_DISCONNECT = "com.onodera.BleApp.template.ACTION_DISCONNECT";

    private final static int NOTIFICATION_ID = 864;
    private final static int OPEN_ACTIVITY_REQ = 0;
    private final static int DISCONNECT_REQ = 1;

    public static final int ACCELEROMETER = 1;

    private AccelerometerManager manager;

    private final LocalBinder binder = new TemplateBinder();

    private AccelerometerListener mListener;

    interface AccelerometerListener{
        void onAccelerometerSend(byte[] value);
    }

    /**
     * This local binder is an interface for the bound activity to operate with the sensor.
     */
    class TemplateBinder extends LocalBinder {
        // TODO Define service API that may be used by a bound Activity

        public void setListener(BleConnectActivity listener) {
            mListener = listener;
        }

        /**
         * Sends some important data to the device.
         *
         * @param parameter some parameter.
         */
        void performAction(final String parameter) {
            manager.performAction(parameter);
        }
    }

    @Override
    protected LocalBinder getBinder() {
        return binder;
    }


    @Override
    protected LoggableBleManager<AccelerometerManagerCallbacks> initializeManager() {
        return manager = new AccelerometerManager(this);
    }

    /*
    @Override
    public void onCreate() {
        super.onCreate();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DISCONNECT);
        registerReceiver(disconnectActionBroadcastReceiver, filter);
    } */

    @Override
    public void onDestroy() {
        // when user has disconnected from the sensor, we have to cancel the notification that we've created some milliseconds before using unbindService
        stopForegroundService();
        //unregisterReceiver(disconnectActionBroadcastReceiver);

        super.onDestroy();
    }

    @Override
    protected void onRebind() {
        stopForegroundService();
    }

    @Override
    protected void onUnbind() {
        startForegroundService();
    }

    @Override
    public void onSampleValueReceived(@NonNull final BluetoothDevice device, final byte[] value) {
        /*
        final Intent broadcast = new Intent(BROADCAST_TEMPLATE_MEASUREMENT);
        broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
        broadcast.putExtra(EXTRA_DATA, value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);



        if (!bound) {
            // Here we may update the notification to display the current value.
            // TODO modify the notification here
        }
         */
        if(mListener!=null) {
            mListener.onAccelerometerSend(value);
        }
    }

    /*
    @Override
    public void onBatteryLevelChanged(@NonNull final BluetoothDevice device, final int batteryLevel) {

    } */

    @Override
    public void onDeviceConnecting(@NonNull final BluetoothDevice device) {
        final Intent broadcast = new Intent(BROADCAST_CONNECTION_STATE);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_CONNECTION_STATE, STATE_CONNECTING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onDeviceConnected(@NonNull final BluetoothDevice device) {
        final Intent broadcast = new Intent(BROADCAST_CONNECTION_STATE);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_CONNECTION_STATE, STATE_CONNECTED);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_DEVICE_NAME, deviceName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onDeviceDisconnecting(@NonNull final BluetoothDevice device) {
        // Notify user about changing the state to DISCONNECTING
        final Intent broadcast = new Intent(BROADCAST_CONNECTION_STATE);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_CONNECTION_STATE, STATE_DISCONNECTING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
        // Note 1: Do not use the device argument here unless you change calling onDeviceDisconnected from the binder above

        // Note 2: if BleManager#shouldAutoConnect() for this device returned true, this callback will be
        // invoked ONLY when user requested disconnection (using Disconnect button). If the device
        // disconnects due to a link loss, the onLinkLossOccurred(BluetoothDevice) method will be called instead.

        final Intent broadcast = new Intent(BROADCAST_CONNECTION_STATE);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_CONNECTION_STATE, STATE_DISCONNECTED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

        if (stopWhenDisconnected())
            stopService();
    }

    @Override
    public void onLinkLossOccurred(@NonNull final BluetoothDevice device) {
        final Intent broadcast = new Intent(BROADCAST_CONNECTION_STATE);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_CONNECTION_STATE, STATE_LINK_LOSS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onServicesDiscovered(@NonNull final BluetoothDevice device, final boolean optionalServicesFound) {
        final Intent broadcast = new Intent(BROADCAST_SERVICES_DISCOVERED);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_SERVICE_PRIMARY, true);
        broadcast.putExtra(EXTRA_SERVICE_SECONDARY, optionalServicesFound);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onDeviceReady(@NonNull final BluetoothDevice device) {
        final Intent broadcast = new Intent(BROADCAST_DEVICE_READY);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onDeviceNotSupported(@NonNull final BluetoothDevice device) {
        final Intent broadcast = new Intent(BROADCAST_SERVICES_DISCOVERED);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_SERVICE_PRIMARY, false);
        broadcast.putExtra(EXTRA_SERVICE_SECONDARY, false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

        // no need for disconnecting, it will be disconnected by the manager automatically
    }

    @Override
    public void onBondingRequired(@NonNull final BluetoothDevice device) {
        showToast(com.onodera.BleApp.common.R.string.bonding);

        final Intent broadcast = new Intent(BROADCAST_BOND_STATE);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onBonded(@NonNull final BluetoothDevice device) {
        showToast(com.onodera.BleApp.common.R.string.bonded);

        final Intent broadcast = new Intent(BROADCAST_BOND_STATE);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onBondingFailed(@NonNull final BluetoothDevice device) {
        showToast(com.onodera.BleApp.common.R.string.bonding_failed);

        final Intent broadcast = new Intent(BROADCAST_BOND_STATE);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onError(@NonNull final BluetoothDevice device, @NonNull final String message, final int errorCode) {
        final Intent broadcast = new Intent(BROADCAST_ERROR);
        broadcast.putExtra(EXTRA_DISTINGUISH, ACCELEROMETER);
        broadcast.putExtra(EXTRA_DEVICE, bluetoothDevice);
        broadcast.putExtra(EXTRA_ERROR_MESSAGE, message);
        broadcast.putExtra(EXTRA_ERROR_CODE, errorCode);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    /**
     * Sets the service as a foreground service
     */
    private void startForegroundService(){
        // when the activity closes we need to show the notification that user is connected to the peripheral sensor
        // We start the service as a foreground service as Android 8.0 (Oreo) onwards kills any running background services
        final Notification notification = createNotification(R.string.uart_notification_connected_message, 0);
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

    /**
     * Creates the notification.
     *
     * @param messageResId message resource id. The message must have one String parameter,<br />
     *                     f.e. <code>&lt;string name="name"&gt;%s is connected&lt;/string&gt;</code>
     * @param defaults     signals that will be used to notify the user
     */
    @SuppressWarnings("SameParameterValue")
    private Notification createNotification(final int messageResId, final int defaults) {
        final Intent parentIntent = new Intent(this, FeaturesActivity.class);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Intent targetIntent = new Intent(this, BleMainActivity.class);

        final Intent disconnect = new Intent(ACTION_DISCONNECT);
        final PendingIntent disconnectAction = PendingIntent.getBroadcast(this, DISCONNECT_REQ, disconnect, PendingIntent.FLAG_UPDATE_CURRENT);

        // both activities above have launchMode="singleTask" in the AndroidManifest.xml file, so if the task is already running, it will be resumed
        final PendingIntent pendingIntent = PendingIntent.getActivities(this, OPEN_ACTIVITY_REQ, new Intent[]{parentIntent, targetIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ToolboxApplication.CONNECTED_DEVICE_CHANNEL);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(getString(R.string.app_name)).setContentText(getString(messageResId, getDeviceName()));
        builder.setSmallIcon(R.drawable.ic_stat_notify_template);
        builder.setShowWhen(defaults != 0).setDefaults(defaults).setAutoCancel(true).setOngoing(true);
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_action_bluetooth, getString(R.string.template_notification_action_disconnect), disconnectAction));

        return builder.build();
    }

    /**
     * Cancels the existing notification. If there is no active notification this method does nothing
     */
    private void cancelNotification() {
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    /**
     * This broadcast receiver listens for {@link #ACTION_DISCONNECT} that may be fired by pressing Disconnect action button on the notification.
     */
    /*
    private final BroadcastReceiver disconnectActionBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Logger.i(getLogSession(), "[Notification] Disconnect action pressed");
            if (isConnected())
                getBinder().disconnect();
            else
                stopSelf();
        }
    };

     */
}
