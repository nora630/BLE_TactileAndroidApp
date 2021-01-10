package com.onodera.BleApp.template.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.BleManagerCallbacks;

public interface HapbeatBleManagerCallbacks {
    void onHapbeatConnecting(@NonNull BluetoothDevice device);

    void onHapbeatConnected(@NonNull BluetoothDevice device);

    void onHapbeatDisconnecting(@NonNull BluetoothDevice device);

    void onHapbeatDisconnected(@NonNull BluetoothDevice device);

    void onHapbeatLinkLossOccurred(@NonNull BluetoothDevice device);

    void onHapbeatServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound);

    void onHapbeatReady(@NonNull BluetoothDevice device);

    void onHapbeatBondingRequired(@NonNull BluetoothDevice device);

    void onHapbeatBonded(@NonNull BluetoothDevice device);

    void onHapbeatBondingFailed(@NonNull BluetoothDevice device);

    void onHapbeatError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode);

    void onHapbeatNotSupported(@NonNull BluetoothDevice device);

}
