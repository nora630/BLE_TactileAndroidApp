package com.onodera.BleApp.template;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.onodera.BleApp.battery.BatteryManager;
import com.onodera.BleApp.parser.TemplateParser;
import com.onodera.BleApp.template.callback.AccelerometerDataCallback;

import java.util.UUID;

import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.log.LogContract;

public class HapbeatManager extends BatteryManager<HapbeatManagerCallbacks> {
    // TODO Replace the services and characteristics below to match your device.

    public static final UUID BASE_UUID = UUID.fromString("AD3EBCDE-B686-40A6-E94B-012E2B3BEA8D");
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
        protected void initialize() {
            // Initialize the Battery Manager. It will enable Battery Level notifications.
            // Remove it if you don't need this feature.
            super.initialize();

            // TODO Initialize your manager here.
            // Initialization is done once, after the device is connected. Usually it should
            // enable notifications or indications on some characteristics, write some data or
            // read some features / version.
            // After the initialization is complete, the onDeviceReady(...) method will be called.

            // Increase the MTU
			/*
			requestMtu(43)
					.with((device, mtu) -> log(LogContract.Log.Level.APPLICATION, "MTU changed to " + mtu))
					.done(device -> {
						// You may do some logic in here that should be done when the request finished successfully.
						// In case of MTU this method is called also when the MTU hasn't changed, or has changed
						// to a different (lower) value. Use .with(...) to get the MTU value.
					})
					.fail((device, status) -> log(Log.WARN, "MTU change not supported"))
					.enqueue();
			*/
            // Set notification callback
            setNotificationCallback(requiredCharacteristic)
                    // This callback will be called each time the notification is received
                    .with(new AccelerometerDataCallback() {
                        @Override
                        public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
                            log(LogContract.Log.Level.APPLICATION, TemplateParser.parse(data));
                            super.onDataReceived(device, data);
                        }

                        @Override
                        public void onSampleValueReceived(@NonNull final BluetoothDevice device, final byte[] value) {
                            // Let's lass received data to the service
                            //callbacks.onSampleValueReceived(device, value);
                        }

                        @Override
                        public void onInvalidDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
                            log(Log.WARN, "Invalid data received: " + data);
                        }
                    });

            // Enable notifications
            enableNotifications(requiredCharacteristic)
                    // Method called after the data were sent (data will contain 0x0100 in this case)
                    .with((device, data) -> log(Log.DEBUG, "Data sent: " + data))
                    // Method called when the request finished successfully. This will be called after .with(..) callback
                    .done(device -> log(LogContract.Log.Level.APPLICATION, "Notifications enabled successfully"))
                    // Methods called in case of an error, for example when the characteristic does not have Notify property
                    .fail((device, status) -> log(Log.WARN, "Failed to enable notifications"))
                    .enqueue();
        }

        @Override
        protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            // TODO Initialize required characteristics.
            // It should return true if all has been discovered (that is that device is supported).
            final BluetoothGattService service = gatt.getService(HAPBEAT_SERVICE_UUID);
            if (service != null) {
                requiredCharacteristic = service.getCharacteristic(HAPBEAT_CHARACTERISTIC_UUID);
            }
            //final BluetoothGattService otherService = gatt.getService(OTHER_SERVICE_UUID);
            //if (otherService != null) {
            //deviceNameCharacteristic = otherService.getCharacteristic(WRITABLE_CHARACTERISTIC_UUID);
            //}
            //return requiredCharacteristic != null && deviceNameCharacteristic != null;
            return requiredCharacteristic != null;
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
            //deviceNameCharacteristic = null;
            //optionalCharacteristic = null;
        }

		/*
		@Override
		protected void onDeviceReady() {
			super.onDeviceReady();

			// Initialization is now ready.
			// The service or activity has been notified with TemplateManagerCallbacks#onDeviceReady().
			// TODO Do some extra logic here, of remove onDeviceReady().

			// Device is ready, let's read something here. Usually there is nothing else to be done
			// here, as all had been done during initialization.
			readCharacteristic(optionalCharacteristic)
					.with((device, data) -> {
						// Characteristic value has been read
						// Let's do some magic with it.
						if (data.size() > 0) {
							final Integer value = data.getIntValue(Data.FORMAT_UINT8, 0);
							log(LogContract.Log.Level.APPLICATION, "Value '" + value + "' has been read!");
						} else {
							log(Log.WARN, "Value is empty!");
						}
					})
					.enqueue();
		}*/
    }

    // TODO Define manager's API

    /**
     * This method will write important data to the device.
     *
     * @param parameter parameter to be written.
     */
    void performAction(final String parameter) {
		/*
		log(Log.VERBOSE, "Changing device name to \"" + parameter + "\"");
		// Write some data to the characteristic.
		writeCharacteristic(deviceNameCharacteristic, Data.from(parameter))
				// If data are longer than MTU-3, they will be chunked into multiple packets.
				// Check out other split options, with .split(...).
				.split()
				// Callback called when data were sent, or added to outgoing queue in case
				// Write Without Request type was used.
				.with((device, data) -> log(Log.DEBUG, data.size() + " bytes were sent"))
				// Callback called when data were sent, or added to outgoing queue in case
				// Write Without Request type was used. This is called after .with(...) callback.
				.done(device -> log(LogContract.Log.Level.APPLICATION, "Device name set to \"" + parameter + "\""))
				// Callback called when write has failed.
				.fail((device, status) -> log(Log.WARN, "Failed to change device name"))
				.enqueue();
	*/
    }
}
