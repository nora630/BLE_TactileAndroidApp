package com.onodera.BleApp.template.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.BleManagerCallbacks;

public interface AccelerometerBleManagerCallbacks {
    void onAccelConnecting(@NonNull BluetoothDevice device);

    void onAccelConnected(@NonNull BluetoothDevice device);

    void onAccelDisconnecting(@NonNull BluetoothDevice device);

    void onAccelDisconnected(@NonNull BluetoothDevice device);

    void onAccelLinkLossOccurred(@NonNull BluetoothDevice device);

    void onAccelServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound);

    void onAccelReady(@NonNull BluetoothDevice device);

    void onAccelBondingRequired(@NonNull BluetoothDevice device);

    void onAccelBonded(@NonNull BluetoothDevice device);

    void onAccelBondingFailed(@NonNull BluetoothDevice device);

    void onAccelError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode);

    void onAccelNotSupported(@NonNull BluetoothDevice device);
}
