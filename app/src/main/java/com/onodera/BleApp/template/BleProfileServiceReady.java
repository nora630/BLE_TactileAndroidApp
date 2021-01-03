package com.onodera.BleApp.template;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.onodera.BleApp.R;
import com.onodera.BleApp.profile.BleProfileService;
import com.onodera.BleApp.profile.BleProfileServiceReadyActivity;
import com.onodera.BleApp.utility.DebugLogger;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.Logger;

public class BleProfileServiceReady<E extends BleProfileService.LocalBinder> extends AppCompatActivity
        implements BleManagerCallbacks {
    private static final String TAG = "BleProfileServiceReady";
    private E service;
    private TextView deviceNameView;
    private Button connectButton;

    private ILogSession logSession;
    private BluetoothDevice bluetoothDevice;
    private String deviceName;

    private BleBroadcastReceiver commonBroadcastReceiver;
    private BleServiceConnection serviceConnection;

    public void setService(E service){
        this.service = service;
    }

    public E getService() {
        return this.service;
    }

    public void setDeviceNameView(TextView view){
        this.deviceNameView = view;
    }

    public TextView getDeviceNameView() {
        return this.deviceNameView;
    }

    public void setConnectButton(Button view){
        this.connectButton = view;
    }

    public Button getConnectButton() {
        return this.connectButton;
    }

    public void setLogSession(ILogSession d){
        this.logSession = d;
    }

    public void setBluetoothDevice(BluetoothDevice bd){
        this.bluetoothDevice = bd;
    }

    public BluetoothDevice getBluetoothDevice() {
        return this.bluetoothDevice;
    }

    public void setDeviceName(String s){
        this.deviceName = s;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setCommonBroadcastReceiver(BleBroadcastReceiver commonBroadcastReceiver) {
        this.commonBroadcastReceiver = commonBroadcastReceiver;
    }

    public BleBroadcastReceiver getCommonBroadcastReceiver() {
        return commonBroadcastReceiver;
    }

    public BleServiceConnection getServiceConnection() {
        return serviceConnection;
    }

    public void setServiceConnection(BleServiceConnection serviceConnection) {
        this.serviceConnection = serviceConnection;
    }

    public class BleBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Check if the broadcast applies the connected device
            if (!isBroadcastForThisDevice(intent))
                return;

            final BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BleProfileService.EXTRA_DEVICE);
            if (bluetoothDevice == null)
                return;

            final String action = intent.getAction();
            switch (action) {
                case BleProfileService.BROADCAST_CONNECTION_STATE: {
                    final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);

                    switch (state) {
                        case BleProfileService.STATE_CONNECTED: {
                            deviceName = intent.getStringExtra(BleProfileService.EXTRA_DEVICE_NAME);
                            onDeviceConnected(bluetoothDevice);
                            break;
                        }
                        case BleProfileService.STATE_DISCONNECTED: {
                            onDeviceDisconnected(bluetoothDevice);
                            deviceName = null;
                            break;
                        }
                        case BleProfileService.STATE_LINK_LOSS: {
                            onLinkLossOccurred(bluetoothDevice);
                            break;
                        }
                        case BleProfileService.STATE_CONNECTING: {
                            onDeviceConnecting(bluetoothDevice);
                            break;
                        }
                        case BleProfileService.STATE_DISCONNECTING: {
                            onDeviceDisconnecting(bluetoothDevice);
                            break;
                        }
                        default:
                            // there should be no other actions
                            break;
                    }
                    break;
                }
                case BleProfileService.BROADCAST_SERVICES_DISCOVERED: {
                    final boolean primaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_PRIMARY, false);
                    final boolean secondaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_SECONDARY, false);

                    if (primaryService) {
                        onServicesDiscovered(bluetoothDevice, secondaryService);
                    } else {
                        onDeviceNotSupported(bluetoothDevice);
                    }
                    break;
                }
                case BleProfileService.BROADCAST_DEVICE_READY: {
                    onDeviceReady(bluetoothDevice);
                    break;
                }
                case BleProfileService.BROADCAST_BOND_STATE: {
                    final int state = intent.getIntExtra(BleProfileService.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    switch (state) {
                        case BluetoothDevice.BOND_BONDING:
                            onBondingRequired(bluetoothDevice);
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            onBonded(bluetoothDevice);
                            break;
                    }
                    break;
                }
                case BleProfileService.BROADCAST_ERROR: {
                    final String message = intent.getStringExtra(BleProfileService.EXTRA_ERROR_MESSAGE);
                    final int errorCode = intent.getIntExtra(BleProfileService.EXTRA_ERROR_CODE, 0);
                    onError(bluetoothDevice, message, errorCode);
                    break;
                }
            }
        }
    }

    public class BleServiceConnection implements ServiceConnection {
        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            final E bleService = BleProfileServiceReady.this.service = (E) service;
            bluetoothDevice = bleService.getBluetoothDevice();
            logSession = bleService.getLogSession();
            Logger.d(logSession, "Activity bound to the service");
            onServiceBound(bleService);

            // Update UI
            deviceName = bleService.getDeviceName();
            deviceNameView.setText(deviceName);
            connectButton.setText(R.string.action_disconnect);

            // And notify user if device is connected
            if (bleService.isConnected()) {
                onDeviceConnected(bluetoothDevice);
            } else {
                // If the device is not connected it means that either it is still connecting,
                // or the link was lost and service is trying to connect to it (autoConnect=true).
                onDeviceConnecting(bluetoothDevice);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            // Note: this method is called only when the service is killed by the system,
            // not when it stops itself or is stopped by the activity.
            // It will be called only when there is critically low memory, in practice never
            // when the activity is in foreground.
            Logger.d(logSession, "Activity disconnected from the service");
            deviceNameView.setText(getDefaultDeviceName());
            connectButton.setText(R.string.action_connect);

            service = null;
            deviceName = null;
            bluetoothDevice = null;
            logSession = null;
            onServiceUnbound();
        }
    }

    /**
     * Called when activity binds to the service. The parameter is the object returned in {@link Service#onBind(Intent)} method in your service. The method is
     * called when device gets connected or is created while sensor was connected before. You may use the binder as a sensor interface.
     */
    protected void onServiceBound(E binder) {

    }

    /**
     * Called when activity unbinds from the service. You may no longer use this binder because the sensor was disconnected. This method is also called when you
     * leave the activity being connected to the sensor in the background.
     */
    protected void onServiceUnbound() {

    }


    @Override
    public void onDeviceConnecting(@NonNull final BluetoothDevice device) {
        deviceNameView.setText(deviceName != null ? deviceName : "n/a");
        connectButton.setText(R.string.action_connecting);
    }

    @Override
    public void onDeviceConnected(@NonNull final BluetoothDevice device) {
        deviceNameView.setText(deviceName);
        connectButton.setText(R.string.action_disconnect);
    }

    @Override
    public void onDeviceDisconnecting(@NonNull final BluetoothDevice device) {
        connectButton.setText(R.string.action_disconnecting);
    }

    @Override
    public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
        connectButton.setText(R.string.action_connect);
        deviceNameView.setText(getDefaultDeviceName());

        try {
            Logger.d(logSession, "Unbinding from the service...");
            unbindService(serviceConnection);
            service = null;

            Logger.d(logSession, "Activity unbound from the service");
            onServiceUnbound();
            deviceName = null;
            bluetoothDevice = null;
            logSession = null;
        } catch (final IllegalArgumentException e) {
            // do nothing. This should never happen but does...
        }
    }

    @Override
    public void onLinkLossOccurred(@NonNull final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onServicesDiscovered(@NonNull final BluetoothDevice device, final boolean optionalServicesFound) {
        // empty default implementation
    }

    @Override
    public void onDeviceReady(@NonNull final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onBondingRequired(@NonNull final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onBonded(@NonNull final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onBondingFailed(@NonNull final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onError(@NonNull final BluetoothDevice device, @NonNull final String message, final int errorCode) {
        DebugLogger.e(TAG, "Error occurred: " + message + ",  error code: " + errorCode);
        showToast(message + " (" + errorCode + ")");
    }

    @Override
    public void onDeviceNotSupported(@NonNull final BluetoothDevice device) {
        showToast(R.string.not_supported);
    }

    protected void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(BleProfileServiceReady.this, message, Toast.LENGTH_LONG).show());
    }

    protected void showToast(final int messageResId) {
        runOnUiThread(() -> Toast.makeText(BleProfileServiceReady.this, messageResId, Toast.LENGTH_SHORT).show());
    }

    protected boolean isDeviceConnected() {
        return service != null && service.isConnected();
    }


    protected int getDefaultDeviceName() {
        return R.string.template_default_name;
    }

    /**
     * Checks the {@link BleProfileService#EXTRA_DEVICE} in the given intent and compares it with the connected BluetoothDevice object.
     * @param intent intent received via a broadcast from the service
     * @return true if the data in the intent apply to the connected device, false otherwise
     */
    protected boolean isBroadcastForThisDevice(final Intent intent) {
        final BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BleProfileService.EXTRA_DEVICE);
        return bluetoothDevice != null && bluetoothDevice.equals(bluetoothDevice);
    }

}
