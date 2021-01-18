package com.onodera.BleApp.template;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.onodera.BleApp.battery.BatteryManager;
import com.onodera.BleApp.parser.TemplateParser;
import com.onodera.BleApp.template.callback.AccelerometerDataCallback;

import java.util.UUID;

import no.nordicsemi.android.ble.WriteRequest;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.log.LogContract;

public class HapbeatManager extends BatteryManager<HapbeatManagerCallbacks> {
    // TODO Replace the services and characteristics below to match your device.

    public static final UUID BASE_UUID = UUID.fromString("0200180A-4202-37BB-EA11-139884E095EA");
    /**
     * The service UUID.
     */
    //static final UUID SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb"); // Heart Rate service
    static final UUID HAPBEAT_SERVICE_UUID = UUID.fromString("AD3EBCDE-B686-40A6-E94B-012E2B3BEA8D"); // Accelerometer service
    /**
     * A UUID of a characteristic with notify property.
     */
    //private static final UUID MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb"); // Heart Rate Measurement
    private static final UUID HAPBEAT_CHARACTERISTIC_UUID = UUID.fromString("AD3EBCDF-B686-40A6-E94B-012E2B3BEA8D"); // Accelerometer Measurement
    /**
     * A UUID of a characteristic with read property.
     */
    //private static final UUID READABLE_CHARACTERISTIC_UUID = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb"); // Body Sensor Location
    /**
     * Some other service UUID.
     */
    //private static final UUID OTHER_SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"); // Generic Access service
    //private static final UUID OTHER_SERVICE_UUID = UUID.fromString("02001800-4202-37BB-EA11-139884E095EA"); // Generic Access service
    /**
     * A UUID of a characteristic with write property.
     */
    //private static final UUID WRITABLE_CHARACTERISTIC_UUID = UUID.fromString("00002A00-0000-1000-8000-00805f9b34fb"); // Device Name

    // TODO Add more services and characteristics references.
    //private BluetoothGattCharacteristic requiredCharacteristic, deviceNameCharacteristic, optionalCharacteristic;
    private BluetoothGattCharacteristic requiredCharacteristic;
    private boolean useLongWrite = true;

    public HapbeatManager(final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BatteryManagerGattCallback getGattCallback() {
        return new HapbeatManagerGattCallback();
    }

    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery,
     * receiving indication, etc.
     */
    private class HapbeatManagerGattCallback extends BatteryManagerGattCallback {

        @Override
        protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            // TODO Initialize required characteristics.
            // It should return true if all has been discovered (that is that device is supported).
            final BluetoothGattService service = gatt.getService(HAPBEAT_SERVICE_UUID);
            if (service != null) {
                requiredCharacteristic = service.getCharacteristic(HAPBEAT_CHARACTERISTIC_UUID);
            }

            boolean writeRequest = false;
            boolean writeCommand = false;
            if (requiredCharacteristic != null) {
                final int rxProperties = requiredCharacteristic.getProperties();
                writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
                writeCommand = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;

                // Set the WRITE REQUEST type when the characteristic supports it.
                // This will allow to send long write (also if the characteristic support it).
                // In case there is no WRITE REQUEST property, this manager will divide texts
                // longer then MTU-3 bytes into up to MTU-3 bytes chunks.
                if (writeCommand)
                    requiredCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                else
                    useLongWrite = false;
            }

            return requiredCharacteristic != null && (writeRequest || writeCommand);
        }
		/*
		@Override
		protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
			// Initialize Battery characteristic
			super.isOptionalServiceSupported(gatt);

			// TODO If there are some optional characteristics, initialize them there.
			final BluetoothGattService service = gatt.getService(SERVICE_UUID);
			if (service != null) {
				//optionalCharacteristic = service.getCharacteristic(READABLE_CHARACTERISTIC_UUID);
			}
			//return optionalCharacteristic != null;
			return false;
		} */

        @Override
        protected void onDeviceDisconnected() {
            // Release Battery Service
            super.onDeviceDisconnected();

            // TODO Release references to your characteristics.
            requiredCharacteristic = null;
            useLongWrite = true;
            //deviceNameCharacteristic = null;
            //optionalCharacteristic = null;
        }
    }

    /**
     * Sends the given text to RX characteristic.
     */
    public void send(byte[] value) {
        // Are we connected?
        if (requiredCharacteristic == null)
            return;

        final WriteRequest request = writeCharacteristic(requiredCharacteristic, value)
                .with((device, data) -> log(LogContract.Log.Level.APPLICATION,
                        "\"" + data + "\" sent"));

        request.enqueue();
    }
}
