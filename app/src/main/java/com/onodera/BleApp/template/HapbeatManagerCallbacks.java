package com.onodera.BleApp.template;

import com.onodera.BleApp.battery.BatteryManagerCallbacks;
import com.onodera.BleApp.template.callback.HapbeatCharacteristicCallback;

import no.nordicsemi.android.ble.BleManagerCallbacks;

public interface HapbeatManagerCallbacks extends BleManagerCallbacks, HapbeatCharacteristicCallback {
}
