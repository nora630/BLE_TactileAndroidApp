package com.onodera.BleApp.template;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.onodera.BleApp.profile.BleProfileService;
import com.onodera.BleApp.scanner.ScannerFragment;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.log.ILogSession;

public abstract class BleProfileServiceTwoDevicesActivity extends AppCompatActivity implements
        ScannerFragment.OnDeviceSelectedListener, BleManagerCallbacks {
    private static final String TAG = "BleProfileServiceTwoDevicesActivity";
    private static final String SIS_ACCELEROMETER_NAME = "accelerometer_name";
    private static final String SIS_ACCELEROMETER = "accelerometer";
    private static final String SIS_HAPBEAT_NAME = "hapbeat_name";
    private static final String SIS_HAPBEAT = "hapbeat";
    private static final String LOG_URI = "log_uri";
    protected static final int REQUEST_ENABLE_BT = 2;

    private AccelerometerService.TemplateBinder accelService;
    private HapbeatService.TemplateBinder hapbeatService;

    private TextView accelerometerNameView;
    private TextView hapbeatNameView;
    private Button connectAccelButton;
    private Button connectHapbeatButton;

    private ILogSession logSession;
    private BluetoothDevice bluetoothAccelerometerDevice;
    private BluetoothDevice bluetoothHapbeatDevice;
    private String AccelerometerName;
    private String HapbeatName;

    private final BroadcastReceiver commonBroadcastReceiver = new BroadcastReceiver() {
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
    };

}
