package com.onodera.BleApp.template;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.onodera.BleApp.R;
import com.onodera.BleApp.profile.BleProfileService;
import com.onodera.BleApp.profile.BleProfileServiceReadyActivity;
import com.onodera.BleApp.scanner.ScannerFragment;
import com.onodera.BleApp.utility.DebugLogger;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.Logger;

public abstract class BleProfileServiceTwoDevicesActivity extends AppCompatActivity implements
        ScannerFragment.OnDeviceSelectedListener, BleManagerCallbacks {
    private static final String TAG = "BleProfileServiceTwoDevicesActivity";
    private static final String SIS_ACCELEROMETER_NAME = "accelerometer_name";
    private static final String SIS_ACCELEROMETER_DEVICE = "accelerometer";
    private static final String SIS_HAPBEAT_NAME = "hapbeat_name";
    private static final String SIS_HAPBEAT_DEVICE = "hapbeat";
    private static final String LOG_URI = "log_uri";
    protected static final int REQUEST_ENABLE_BT = 2;

    private AccelerometerService.TemplateBinder accelService;
    private HapbeatService.TemplateBinder hapbeatService;

    //private TextView accelerometerNameView;
    //private TextView hapbeatNameView;
    //private Button connectAccelButton;
    //private Button connectHapbeatButton;
    private UUID uuid;

    private ILogSession logSession;
    //private ILogSession hapbeatLogSession;
    private BluetoothDevice bluetoothAccelerometerDevice;
    private BluetoothDevice bluetoothHapbeatDevice;
    private String accelerometerName;
    private String hapbeatName;

    private BleProfileServiceReady<AccelerometerService.TemplateBinder> accelServiceReady = new BleProfileServiceReady<AccelerometerService.TemplateBinder>();
    private BleProfileServiceReady<HapbeatService.TemplateBinder> hapbeatServiceReady = new BleProfileServiceReady<HapbeatService.TemplateBinder>();




    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }

        // Restore the old log session
        if (savedInstanceState != null) {
            final Uri logUri = savedInstanceState.getParcelable(LOG_URI);
            logSession = Logger.openSession(getApplicationContext(), logUri);
        }

        // In onInitialize method a final class may register local broadcast receivers that will listen for events from the service
        onInitialize(savedInstanceState);
        // The onCreateView class should... create the view
        onCreateView(savedInstanceState);

        final Toolbar toolbar = findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        // Common nRF Toolbox view references are obtained here
        setUpView();
        // View is ready to be used
        onViewCreated(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(accelServiceReady.getCommonBroadcastReceiver(), makeIntentFilter());
        LocalBroadcastManager.getInstance(this).registerReceiver(hapbeatServiceReady.getCommonBroadcastReceiver(), makeIntentFilter());

    }

    @Override
    protected void onStart() {
        super.onStart();

        /*
         * If the service has not been started before, the following lines will not start it.
         * However, if it's running, the Activity will bind to it and notified via serviceConnection.
         */
        final Intent Service1 = new Intent(this, AccelerometerService.TemplateBinder.class);
        // We pass 0 as a flag so the service will not be created if not exists.
        bindService(Service1, accelServiceReady.getServiceConnection(), 0);

        final Intent Service2 = new Intent(this, HapbeatService.TemplateBinder.class);
        bindService(Service2, hapbeatServiceReady.getServiceConnection(), 0);

        /*
         * When user exited the UARTActivity while being connected, the log session is kept in
         * the service. We may not get it before binding to it so in this case this event will
         * not be logged (logSession is null until onServiceConnected(..) is called).
         * It will, however, be logged after the orientation changes.
         */
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            // We don't want to perform some operations (e.g. disable Battery Level notifications)
            // in the service if we are just rotating the screen. However, when the activity will
            // disappear, we may want to disable some device features to reduce the battery
            // consumption.
            if (accelServiceReady.getService() != null)
                accelServiceReady.getService().setActivityIsChangingConfiguration(isChangingConfigurations());

            unbindService(accelServiceReady.getServiceConnection());
            accelServiceReady.setServiceConnection(null);

            if (hapbeatServiceReady.getService() != null)
                hapbeatServiceReady.getService().setActivityIsChangingConfiguration(isChangingConfigurations());

            unbindService(hapbeatServiceReady.getServiceConnection());
            hapbeatServiceReady.setServiceConnection(null);

            Logger.d(logSession, "Activity unbound from the service");
            //onServiceUnbound();
            accelServiceReady.setDeviceName(null);
            hapbeatServiceReady.setDeviceName(null);
            accelServiceReady.setBluetoothDevice(null);
            hapbeatServiceReady.setBluetoothDevice(null);
            logSession = null;
        } catch (final IllegalArgumentException e) {
            // do nothing, we were not connected to the sensor
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(accelServiceReady.getCommonBroadcastReceiver());
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hapbeatServiceReady.getCommonBroadcastReceiver());
    }

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleProfileService.BROADCAST_CONNECTION_STATE);
        intentFilter.addAction(BleProfileService.BROADCAST_SERVICES_DISCOVERED);
        intentFilter.addAction(BleProfileService.BROADCAST_DEVICE_READY);
        intentFilter.addAction(BleProfileService.BROADCAST_BOND_STATE);
        intentFilter.addAction(BleProfileService.BROADCAST_ERROR);
        return intentFilter;
    }


    /**
     * You may do some initialization here. This method is called from {@link #onCreate(Bundle)} before the view was created.
     */
    protected void onInitialize(final Bundle savedInstanceState) {
        // empty default implementation
    }

    /**
     * Called from {@link #onCreate(Bundle)}. This method should build the activity UI, i.e. using {@link #setContentView(int)}.
     * Use to obtain references to views. Connect/Disconnect button, the device name view are manager automatically.
     *
     * @param savedInstanceState contains the data it most recently supplied in {@link #onSaveInstanceState(Bundle)}.
     *                           Note: <b>Otherwise it is null</b>.
     */
    protected abstract void onCreateView(final Bundle savedInstanceState);

    /**
     * Called after the view has been created.
     *
     * @param savedInstanceState contains the data it most recently supplied in {@link #onSaveInstanceState(Bundle)}.
     *                           Note: <b>Otherwise it is null</b>.
     */
    protected void onViewCreated(final Bundle savedInstanceState) {
        // empty default implementation
    }

    /**
     * Called after the view and the toolbar has been created.
     */
    protected final void setUpView() {
        // set GUI
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        accelServiceReady.setConnectButton(findViewById(R.id.accelerometer_connect));
        hapbeatServiceReady.setConnectButton(findViewById(R.id.hapbeat_connect));
        accelServiceReady.setDeviceNameView(findViewById(R.id.accelerometer_name));
        hapbeatServiceReady.setDeviceNameView(findViewById(R.id.hapbeat_name));
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SIS_ACCELEROMETER_NAME, accelServiceReady.getDeviceName());
        outState.putString(SIS_HAPBEAT_NAME, hapbeatServiceReady.getDeviceName());
        outState.putParcelable(SIS_ACCELEROMETER_DEVICE, accelServiceReady.getBluetoothDevice());
        outState.putParcelable(SIS_HAPBEAT_DEVICE, accelServiceReady.getBluetoothDevice());
        if (logSession != null)
            outState.putParcelable(LOG_URI, logSession.getSessionUri());
    }

    @Override
    protected void onRestoreInstanceState(final @NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        accelerometerName = savedInstanceState.getString(SIS_ACCELEROMETER_NAME);
        accelServiceReady.setDeviceName(accelerometerName);
        hapbeatName = savedInstanceState.getString(SIS_HAPBEAT_NAME);
        hapbeatServiceReady.setDeviceName(hapbeatName);
        bluetoothAccelerometerDevice = savedInstanceState.getParcelable(SIS_ACCELEROMETER_DEVICE);
        accelServiceReady.setBluetoothDevice(bluetoothAccelerometerDevice);
        bluetoothHapbeatDevice = savedInstanceState.getParcelable(SIS_HAPBEAT_DEVICE);
        hapbeatServiceReady.setBluetoothDevice(bluetoothHapbeatDevice);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.help, menu);
        return true;
    }

    /**
     * Use this method to handle menu actions other than home and about.
     *
     * @param itemId the menu item id
     * @return <code>true</code> if action has been handled
     */
    protected boolean onOptionsItemSelected(final int itemId) {
        // Overwrite when using menu other than R.menu.help
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
			/*
			case R.id.action_about:
				final AppHelpFragment fragment = AppHelpFragment.getInstance(getAboutTextId());
				fragment.show(getSupportFragmentManager(), "help_fragment");
				break;
			 */
            default:
                return onOptionsItemSelected(id);
        }
        return true;
    }

    /**
     * Called when user press CONNECT or DISCONNECT button. See layout files -> onClick attribute.
     */
    public void onConnectAccelClicked(final View view) {
        if (isBLEEnabled()) {
            if (accelServiceReady.getService() == null) {
                setDefaultUI();
                uuid = getFilterAccelUUID();
                showDeviceScanningDialog(uuid);
            } else {
                accelServiceReady.getService().disconnect();
            }
        } else {
            showBLEDialog();
        }
    }

    public void onConnectHapbeatClicked(final View view) {
        if (isBLEEnabled()) {
            if (hapbeatServiceReady.getService() == null) {
                setDefaultUI();
                uuid = getFilterHapbeatUUID();
                showDeviceScanningDialog(uuid);
            } else {
                hapbeatServiceReady.getService().disconnect();
            }
        } else {
            showBLEDialog();
        }
    }

    /**
     * Returns the title resource id that will be used to create logger session. If 0 is returned (default) logger will not be used.
     *
     * @return the title resource id
     */
    protected int getLoggerProfileTitle() {
        return 0;
    }

    /**
     * This method may return the local log content provider authority if local log sessions are supported.
     *
     * @return local log session content provider URI
     */
    protected Uri getLocalAuthorityLogger() {
        return null;
    }

    @Override
    public void onDeviceSelected(@NonNull final BluetoothDevice device, final String name) {
        final int titleId = getLoggerProfileTitle();
        if (titleId > 0) {
            logSession = Logger.newSession(getApplicationContext(), getString(titleId), device.getAddress(), name);
            // If nRF Logger is not installed we may want to use local logger
            if (logSession == null && getLocalAuthorityLogger() != null) {
                logSession = LocalLogSession.newSession(getApplicationContext(), getLocalAuthorityLogger(), device.getAddress(), name);
            }
        }

        if (uuid == getFilterAccelUUID()) {
            accelServiceReady.setBluetoothDevice(device);
            accelServiceReady.setDeviceName(name);
            // The device may not be in the range but the service will try to connect to it if it reach it
            Logger.d(logSession, "Creating service...");
            final Intent service = new Intent(this, AccelerometerService.class);
            service.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, device.getAddress());
            service.putExtra(BleProfileService.EXTRA_DEVICE_NAME, name);
            if (logSession != null)
                service.putExtra(BleProfileService.EXTRA_LOG_URI, logSession.getSessionUri());
            startService(service);
            Logger.d(logSession, "Binding to the service...");
            bindService(service, accelServiceReady.getServiceConnection(), 0);

        } else if (uuid == getFilterHapbeatUUID()) {
            hapbeatServiceReady.setBluetoothDevice(device);
            hapbeatServiceReady.setDeviceName(name);
            // The device may not be in the range but the service will try to connect to it if it reach it
            Logger.d(logSession, "Creating service...");
            final Intent service = new Intent(this, HapbeatService.class);
            service.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, device.getAddress());
            service.putExtra(BleProfileService.EXTRA_DEVICE_NAME, name);
            if (logSession != null)
                service.putExtra(BleProfileService.EXTRA_LOG_URI, logSession.getSessionUri());
            startService(service);
            Logger.d(logSession, "Binding to the service...");
            bindService(service, hapbeatServiceReady.getServiceConnection(), 0);
        }
    }


    @Override
    public void onDialogCanceled() {
        // do nothing
    }




    /**
     * The UUID filter is used to filter out available devices that does not have such UUID in their advertisement packet. See also:
     * {@link #isChangingConfigurations()}.
     *
     * @return the required UUID or <code>null</code>
     */
    protected UUID getFilterAccelUUID() {
        return AccelerometerManager.ACCEL_SERVICE_UUID;
    }

    protected UUID getFilterHapbeatUUID() {
        return HapbeatManager.HAPBEAT_SERVICE_UUID;
    }


    protected abstract void setDefaultUI();

    /**
     * Shows the scanner fragment.
     *
     * @param filter               the UUID filter used to filter out available devices. The fragment will always show all bonded devices as there is no information about their
     *                             services
     *
     */
    private void showDeviceScanningDialog(final UUID filter) {
        final ScannerFragment dialog = ScannerFragment.getInstance(filter);
        dialog.show(getSupportFragmentManager(), "scan_fragment");
    }

    /**
     * Returns the log session. Log session is created when the device was selected using the {@link ScannerFragment} and released when user press DISCONNECT.
     *
     * @return the logger session or <code>null</code>
     */
    protected ILogSession getLogSession() {
        return logSession;
    }

    private void ensureBLESupported() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    protected boolean isBLEEnabled() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    protected void showBLEDialog() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }


}
