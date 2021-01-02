package com.onodera.BleApp.template;

import com.onodera.BleApp.battery.BatteryManagerCallbacks;
import com.onodera.BleApp.template.callback.HapbeatCharacteristicCallback;

public interface HapbeatManagerCallbacks extends BatteryManagerCallbacks, HapbeatCharacteristicCallback {
}
