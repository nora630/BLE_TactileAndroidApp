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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.onodera.BleApp.AppHelpFragment;
import com.onodera.BleApp.R;
import com.onodera.BleApp.profile.BleProfileService;
import com.onodera.BleApp.profile.BleProfileServiceReadyActivity;
import com.onodera.BleApp.scanner.ScannerFragment;
import com.onodera.BleApp.template.callback.AccelerometerBleManagerCallbacks;
import com.onodera.BleApp.template.callback.HapbeatBleManagerCallbacks;
import com.onodera.BleApp.utility.DebugLogger;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.Logger;

public abstract class BleConnectActivity extends AppCompatActivity
        implements ScannerFragment.OnDeviceSelectedListener, AccelerometerBleManagerCallbacks,
                        HapbeatBleManagerCallbacks, AccelerometerService.AccelerometerListener {
    private static final String TAG = "BleConnectActivity";

    private static final String SIS_ACCEL_NAME = "accelerometer_name";
    private static final String SIS_ACCEL = "accelerometer";
    private static final String LOG_ACCEL_URI = "log_accelerometer_uri";
    private static final String SIS_HAPBEAT_NAME = "hapbeat_name";
    private static final String SIS_HAPBEAT = "hapbeat";
    private static final String LOG_HAPBEAT_URI = "log_hapbeat_uri";


    protected static final int REQUEST_ENABLE_BT = 2;

    protected AccelerometerService.TemplateBinder accelService;
    protected HapbeatService.TemplateBinder hapbeatService;

    private BleManagerCallbacks accelListener;
    private BleManagerCallbacks hapbeatListner;

    private TextView accelNameView;
    private Button accelConnectButton;
    private TextView hapbeatNameView;
    private Button hapbeatConnectButton;


    private ILogSession accelLogSession;
    private ILogSession hapbeatLogSession;
    private BluetoothDevice accelBluetoothDevice;
    private BluetoothDevice hapbeatBluetoothDevice;
    private String accelName;
    private String hapbeatName;

    private SeekBar seekBarView;
    private TextView seekTextView;
    private int volumeScale = 50;

    private SeekBar lowSeekBarView;
    private TextView lowSeekTextView;
    private int lowValue = 20;

    private SeekBar highSeekBarView;
    private TextView highSeekTextView;
    private int highValue = 30;

    protected SharedPreferences pref;

    protected UUID mUuid;


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
            final int dev = intent.getIntExtra(BleProfileService.EXTRA_DISTINGUISH, 0);
            if (dev == AccelerometerService.ACCELEROMETER) {
                switch (action) {
                    case BleProfileService.BROADCAST_CONNECTION_STATE: {
                        final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);

                        switch (state) {
                            case BleProfileService.STATE_CONNECTED: {
                                accelName = intent.getStringExtra(BleProfileService.EXTRA_DEVICE_NAME);
                                onAccelConnected(bluetoothDevice);
                                break;
                            }
                            case BleProfileService.STATE_DISCONNECTED: {
                                onAccelDisconnected(bluetoothDevice);
                                accelName = null;
                                break;
                            }
                            case BleProfileService.STATE_LINK_LOSS: {
                                onAccelLinkLossOccurred(bluetoothDevice);
                                break;
                            }
                            case BleProfileService.STATE_CONNECTING: {
                                onAccelConnecting(bluetoothDevice);
                                break;
                            }
                            case BleProfileService.STATE_DISCONNECTING: {
                                onAccelDisconnecting(bluetoothDevice);
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
                            onAccelServicesDiscovered(bluetoothDevice, secondaryService);
                        } else {
                            onAccelNotSupported(bluetoothDevice);
                        }
                        break;
                    }
                    case BleProfileService.BROADCAST_DEVICE_READY: {
                        onAccelReady(bluetoothDevice);
                        break;
                    }
                    case BleProfileService.BROADCAST_BOND_STATE: {
                        final int state = intent.getIntExtra(BleProfileService.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                        switch (state) {
                            case BluetoothDevice.BOND_BONDING:
                                onAccelBondingRequired(bluetoothDevice);
                                break;
                            case BluetoothDevice.BOND_BONDED:
                                onAccelBonded(bluetoothDevice);
                                break;
                        }
                        break;
                    }
                    case BleProfileService.BROADCAST_ERROR: {
                        final String message = intent.getStringExtra(BleProfileService.EXTRA_ERROR_MESSAGE);
                        final int errorCode = intent.getIntExtra(BleProfileService.EXTRA_ERROR_CODE, 0);
                        onAccelError(bluetoothDevice, message, errorCode);
                        break;
                    }
                }
            } else if (dev == HapbeatService.HAPBEAT) {
                switch (action) {
                    case BleProfileService.BROADCAST_CONNECTION_STATE: {
                        final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);

                        switch (state) {
                            case BleProfileService.STATE_CONNECTED: {
                                hapbeatName = intent.getStringExtra(BleProfileService.EXTRA_DEVICE_NAME);
                                onHapbeatConnected(bluetoothDevice);
                                break;
                            }
                            case BleProfileService.STATE_DISCONNECTED: {
                                onHapbeatDisconnected(bluetoothDevice);
                                hapbeatName = null;
                                break;
                            }
                            case BleProfileService.STATE_LINK_LOSS: {
                                onHapbeatLinkLossOccurred(bluetoothDevice);
                                break;
                            }
                            case BleProfileService.STATE_CONNECTING: {
                                onHapbeatConnecting(bluetoothDevice);
                                break;
                            }
                            case BleProfileService.STATE_DISCONNECTING: {
                                onHapbeatDisconnecting(bluetoothDevice);
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
                            onHapbeatServicesDiscovered(bluetoothDevice, secondaryService);
                        } else {
                            onHapbeatNotSupported(bluetoothDevice);
                        }
                        break;
                    }
                    case BleProfileService.BROADCAST_DEVICE_READY: {
                        onHapbeatReady(bluetoothDevice);
                        break;
                    }
                    case BleProfileService.BROADCAST_BOND_STATE: {
                        final int state = intent.getIntExtra(BleProfileService.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                        switch (state) {
                            case BluetoothDevice.BOND_BONDING:
                                onHapbeatBondingRequired(bluetoothDevice);
                                break;
                            case BluetoothDevice.BOND_BONDED:
                                onHapbeatBonded(bluetoothDevice);
                                break;
                        }
                        break;
                    }
                    case BleProfileService.BROADCAST_ERROR: {
                        final String message = intent.getStringExtra(BleProfileService.EXTRA_ERROR_MESSAGE);
                        final int errorCode = intent.getIntExtra(BleProfileService.EXTRA_ERROR_CODE, 0);
                        onHapbeatError(bluetoothDevice, message, errorCode);
                        break;
                    }
                }
            }
        }
    };


    private ServiceConnection accelServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName Name, IBinder service) {
            final AccelerometerService.TemplateBinder bleService
                    = BleConnectActivity.this.accelService
                    = (AccelerometerService.TemplateBinder) service;
            accelBluetoothDevice = bleService.getBluetoothDevice();
            accelLogSession = bleService.getLogSession();
            Logger.d(accelLogSession, "Activity bound to the service");
            onServiceBound(bleService);

            accelService.setListener(BleConnectActivity.this);

            // Update UI
            accelName = bleService.getDeviceName();
            accelNameView.setText(accelName);
            accelConnectButton.setText(R.string.action_disconnect);

            // And notify user if device is connected
            if (bleService.isConnected()) {
                onAccelConnected(accelBluetoothDevice);
            } else {
                // If the device is not connected it means that either it is still connecting,
                // or the link was lost and service is trying to connect to it (autoConnect=true).
                onAccelConnecting(accelBluetoothDevice);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName Name) {
            // Note: this method is called only when the service is killed by the system,
            // not when it stops itself or is stopped by the activity.
            // It will be called only when there is critically low memory, in practice never
            // when the activity is in foreground.
            Logger.d(accelLogSession, "Activity disconnected from the service");
            accelNameView.setText(getDefaultDeviceName());
            accelConnectButton.setText(R.string.sensor_connect);

            accelService = null;
            accelName = null;
            accelBluetoothDevice = null;
            accelLogSession = null;
            onServiceUnbound();
        }
    };

    private ServiceConnection hapbeatServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName Name, IBinder service) {
            final HapbeatService.TemplateBinder bleService
                    = BleConnectActivity.this.hapbeatService
                    = (HapbeatService.TemplateBinder) service;
            hapbeatBluetoothDevice = bleService.getBluetoothDevice();
            hapbeatLogSession = bleService.getLogSession();
            Logger.d(hapbeatLogSession, "Activity bound to the service");
            onServiceBound(bleService);

            // Update UI
            hapbeatName = bleService.getDeviceName();
            hapbeatNameView.setText(hapbeatName);
            hapbeatConnectButton.setText(R.string.action_disconnect);

            //volumeScale = pref.getInt("volumeScale", volumeScale);
            //lowValue = pref.getInt("lowValue", lowValue);
            //highValue = pref.getInt("highValue", highValue);
            hapbeatService.setVolumeScale(volumeScale);
            hapbeatService.setLowValue(lowValue);
            hapbeatService.setHighValue(highValue);

            // And notify user if device is connected
            if (bleService.isConnected()) {
                onHapbeatConnected(hapbeatBluetoothDevice);
            } else {
                // If the device is not connected it means that either it is still connecting,
                // or the link was lost and service is trying to connect to it (autoConnect=true).
                onHapbeatConnecting(hapbeatBluetoothDevice);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName Name) {
            // Note: this method is called only when the service is killed by the system,
            // not when it stops itself or is stopped by the activity.
            // It will be called only when there is critically low memory, in practice never
            // when the activity is in foreground.
            Logger.d(hapbeatLogSession, "Activity disconnected from the service");
            hapbeatNameView.setText(getDefaultDeviceName());
            hapbeatConnectButton.setText(R.string.hapbeat_connect);

            hapbeatService = null;
            hapbeatName = null;
            hapbeatBluetoothDevice = null;
            hapbeatLogSession = null;
            onServiceUnbound();
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }

        // Restore the old log session
        if (savedInstanceState != null) {
            final Uri logAccelUri = savedInstanceState.getParcelable(LOG_ACCEL_URI);
            accelLogSession = Logger.openSession(getApplicationContext(), logAccelUri);
            final Uri logHapbeatUri = savedInstanceState.getParcelable(LOG_HAPBEAT_URI);
            hapbeatLogSession = Logger.openSession(getApplicationContext(), logHapbeatUri);
        }

        pref = getSharedPreferences("Pref", MODE_PRIVATE);


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



        LocalBroadcastManager.getInstance(this).registerReceiver(commonBroadcastReceiver, makeIntentFilter());
    }


    @Override
    protected void onStart() {
        super.onStart();

        /*
         * If the service has not been started before, the following lines will not start it.
         * However, if it's running, the Activity will bind to it and notified via serviceConnection.
         */
        final Intent service1 = new Intent(this, AccelerometerService.class);
        // We pass 0 as a flag so the service will not be created if not exists.
        bindService(service1, accelServiceConnection, 0);

        final Intent service2 = new Intent(this, HapbeatService.class);
        // We pass 0 as a flag so the service will not be created if not exists.
        bindService(service2, hapbeatServiceConnection, 0);

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
            if (accelService != null)
                accelService.setActivityIsChangingConfiguration(isChangingConfigurations());

            if (hapbeatService != null)
                hapbeatService.setActivityIsChangingConfiguration(isChangingConfigurations());

            unbindService(accelServiceConnection);
            unbindService(hapbeatServiceConnection);
            //accelService = null;
            //hapbeatService = null;

            Logger.d(accelLogSession, "Activity unbound from the service");
            Logger.d(hapbeatLogSession, "Activity unbound from the service");
            onServiceUnbound();
            accelName = null;
            hapbeatName = null;
            accelBluetoothDevice = null;
            hapbeatBluetoothDevice = null;
            accelLogSession = null;
            hapbeatLogSession = null;
        } catch (final IllegalArgumentException e) {
            // do nothing, we were not connected to the sensor
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        accelService = null;
        hapbeatService = null;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(commonBroadcastReceiver);
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
     * Called when activity binds to the service. The parameter is the object returned in {@link Service#onBind(Intent)} method in your service. The method is
     * called when device gets connected or is created while sensor was connected before. You may use the binder as a sensor interface.
     */
    protected void onServiceBound(BleProfileService.LocalBinder binder) {

    }

    /**
     * Called when activity unbinds from the service. You may no longer use this binder because the sensor was disconnected. This method is also called when you
     * leave the activity being connected to the sensor in the background.
     */
    protected void onServiceUnbound() {

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
    protected void setUpView() {
        // set GUI
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        accelConnectButton = findViewById(R.id.action_connect);
        accelNameView = findViewById(R.id.device_name);
        hapbeatConnectButton = findViewById(R.id.action_connect2);
        hapbeatNameView = findViewById(R.id.device_name2);

        seekBarView = findViewById(R.id.seekBar);
        seekTextView = findViewById(R.id.value2);

        SharedPreferences.Editor editor = pref.edit();
        volumeScale = pref.getInt("volumeScale", volumeScale);
        seekBarView.setProgress(volumeScale);

        //int p = seekBarView.getProgress();
        String s = "volume: " + volumeScale/10.0f;
        seekTextView.setText(s);

        seekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String s = "volume: " + i/10.0f;
                volumeScale = i;
                editor.putInt("volumeScale", volumeScale);
                editor.commit();
                if (hapbeatService!=null) hapbeatService.setVolumeScale(i);
                seekTextView.setText(s);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        lowSeekBarView = findViewById(R.id.low_seekBar);
        lowSeekTextView = findViewById(R.id.low_value);

        lowValue = pref.getInt("lowValue", lowValue);
        lowSeekBarView.setProgress(lowValue);
        //p = lowSeekBarView.getProgress();
        s = "0~50Hz: " + lowValue/10.0f;
        lowSeekTextView.setText(s);

        lowSeekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String s = "0~50Hz: " + i/10.0f;
                lowValue = i;
                editor.putInt("lowValue", lowValue);
                editor.commit();
                if (hapbeatService!=null) hapbeatService.setLowValue(i);
                lowSeekTextView.setText(s);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        highSeekBarView = findViewById(R.id.high_seekBar);
        highSeekTextView = findViewById(R.id.high_value);

        highValue = pref.getInt("highValue", highValue);
        highSeekBarView.setProgress(highValue);
        //p = highSeekBarView.getProgress();
        s = "50~500Hz: " + highValue/10.0f;
        highSeekTextView.setText(s);

        highSeekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String s = "50~500Hz: " + i/10.0f;
                highValue = i;
                editor.putInt("highValue", highValue);
                editor.commit();
                if (hapbeatService!=null) hapbeatService.setHighValue(i);
                highSeekTextView.setText(s);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SIS_ACCEL_NAME, accelName);
        outState.putParcelable(SIS_ACCEL, accelBluetoothDevice);
        if (accelLogSession != null)
            outState.putParcelable(LOG_ACCEL_URI, accelLogSession.getSessionUri());

        outState.putString(SIS_HAPBEAT_NAME, hapbeatName);
        outState.putParcelable(SIS_HAPBEAT, hapbeatBluetoothDevice);
        if (hapbeatLogSession != null)
            outState.putParcelable(LOG_HAPBEAT_URI, hapbeatLogSession.getSessionUri());
    }

    @Override
    protected void onRestoreInstanceState(final @NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        accelName = savedInstanceState.getString(SIS_ACCEL_NAME);
        accelBluetoothDevice = savedInstanceState.getParcelable(SIS_ACCEL);
        hapbeatName = savedInstanceState.getString(SIS_HAPBEAT_NAME);
        hapbeatBluetoothDevice = savedInstanceState.getParcelable(SIS_HAPBEAT);
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

			case R.id.action_about:
				//final AppHelpFragment fragment = AppHelpFragment.getInstance(getAboutTextId());
				//fragment.show(getSupportFragmentManager(), "help_fragment");
				break;
            default:
                return onOptionsItemSelected(id);
        }
        return true;
    }



    /**
     * Called when user press CONNECT or DISCONNECT button. See layout files -> onClick attribute.
     */
    public void onConnectClicked(final View view) {
        if (isBLEEnabled()) {
            if (accelService == null) {
                setDefaultUI();
                mUuid = getAccelFilterUUID();
                showDeviceScanningDialog(getAccelFilterUUID());
            } else {
                accelService.disconnect();
            }
        } else {
            showBLEDialog();
        }
    }

    public void onConnectClicked2(final View view) {
        if (isBLEEnabled()) {
            if (hapbeatService == null) {
                setDefaultUI();
                mUuid = getHapbeatFilterUUID();
                showDeviceScanningDialog(getHapbeatFilterUUID());
            } else {
                hapbeatService.disconnect();
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
    public void onDeviceSelected(@NonNull BluetoothDevice device, @Nullable String name) {
        final int titleId = getLoggerProfileTitle();
        if (mUuid.equals(AccelerometerManager.ACCEL_SERVICE_UUID)){
            if (titleId > 0) {
                accelLogSession = Logger.newSession(getApplicationContext(), getString(titleId), device.getAddress(), name);
                // If nRF Logger is not installed we may want to use local logger
                if (accelLogSession == null && getLocalAuthorityLogger() != null) {
                    accelLogSession = LocalLogSession.newSession(getApplicationContext(), getLocalAuthorityLogger(), device.getAddress(), name);
                }
            }
            accelBluetoothDevice = device;
            accelName = name;

            // The device may not be in the range but the service will try to connect to it if it reach it
            Logger.d(accelLogSession, "Creating service...");
            final Intent service = new Intent(this, AccelerometerService.class);
            service.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, device.getAddress());
            service.putExtra(BleProfileService.EXTRA_DEVICE_NAME, name);
            if (accelLogSession != null)
                service.putExtra(BleProfileService.EXTRA_LOG_URI, accelLogSession.getSessionUri());
            startService(service);
            Logger.d(accelLogSession, "Binding to the service...");
            bindService(service, accelServiceConnection, 0);
        } else if (mUuid.equals(HapbeatManager.HAPBEAT_SERVICE_UUID)) {
            if (titleId > 0) {
                hapbeatLogSession= Logger.newSession(getApplicationContext(), getString(titleId), device.getAddress(), name);
                // If nRF Logger is not installed we may want to use local logger
                if (hapbeatLogSession == null && getLocalAuthorityLogger() != null) {
                    hapbeatLogSession = LocalLogSession.newSession(getApplicationContext(), getLocalAuthorityLogger(), device.getAddress(), name);
                }
            }
            hapbeatBluetoothDevice = device;
            hapbeatName = name;

            // The device may not be in the range but the service will try to connect to it if it reach it
            Logger.d(hapbeatLogSession, "Creating service...");
            final Intent service = new Intent(this, HapbeatService.class);
            service.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, device.getAddress());
            service.putExtra(BleProfileService.EXTRA_DEVICE_NAME, name);
            if (hapbeatLogSession != null)
                service.putExtra(BleProfileService.EXTRA_LOG_URI, hapbeatLogSession.getSessionUri());
            startService(service);
            Logger.d(hapbeatLogSession, "Binding to the service...");
            bindService(service, hapbeatServiceConnection, 0);
            //hapbeatService.setNetwork(HapbeatService.Network.local);
        }
    }

    @Override
    public void onDialogCanceled() {

    }


    /**
     * Returns the default device name resource id. The real device name is obtained when connecting to the device. This one is used when device has
     * disconnected.
     *
     * @return the default device name resource id
     */
    protected int getDefaultDeviceName() {
        return R.string.template_default_name;
    }

    /**
     * The UUID filter is used to filter out available devices that does not have such UUID in their advertisement packet. See also:
     * {@link #isChangingConfigurations()}.
     *
     * @return the required UUID or <code>null</code>
     */
    protected UUID getAccelFilterUUID() {
        return AccelerometerManager.ACCEL_SERVICE_UUID;
    }

    protected UUID getHapbeatFilterUUID() {
        return HapbeatManager.HAPBEAT_SERVICE_UUID;
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

    /**
     * Shows the scanner fragment.
     *
     * @param filter               the UUID filter used to filter out available devices. The fragment will always show all bonded devices as there is no information about their
     *                             services
     */
    private void showDeviceScanningDialog(final UUID filter) {
        final ScannerFragment dialog = ScannerFragment.getInstance(filter);
        dialog.show(getSupportFragmentManager(), "scan_fragment");
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

    @Override
    public void onAccelConnecting(@NonNull BluetoothDevice device) {
        accelNameView.setText(accelName != null ? accelName : getString(R.string.not_available));
        accelConnectButton.setText(R.string.action_connecting);
    }

    @Override
    public void onAccelConnected(@NonNull BluetoothDevice device) {
        accelNameView.setText(accelName);
        accelConnectButton.setText(R.string.action_disconnect);
    }

    @Override
    public void onAccelDisconnecting(@NonNull BluetoothDevice device) {
        accelConnectButton.setText(R.string.action_disconnecting);
    }

    @Override
    public void onAccelDisconnected(@NonNull BluetoothDevice device) {
        accelConnectButton.setText(R.string.sensor_connect);
        accelNameView.setText(getDefaultDeviceName());

        try {
            Logger.d(accelLogSession, "Unbinding from the service...");
            unbindService(accelServiceConnection);
            accelService = null;

            Logger.d(accelLogSession, "Activity unbound from the service");
            onServiceUnbound();
            accelName = null;
            accelBluetoothDevice = null;
            accelLogSession = null;
        } catch (final IllegalArgumentException e) {
            // do nothing. This should never happen but does...
        }
    }

    @Override
    public void onAccelLinkLossOccurred(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onAccelServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {

    }

    @Override
    public void onAccelReady(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onAccelBondingRequired(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onAccelBonded(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onAccelBondingFailed(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onAccelError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {
        DebugLogger.e(TAG, "Error occurred: " + message + ",  error code: " + errorCode);
        showToast(message + " (" + errorCode + ")");
    }

    @Override
    public void onAccelNotSupported(@NonNull BluetoothDevice device) {
        showToast(R.string.not_supported);
    }

    @Override
    public void onHapbeatConnecting(@NonNull BluetoothDevice device) {
        hapbeatNameView.setText(hapbeatName != null ? hapbeatName : getString(R.string.not_available));
        hapbeatConnectButton.setText(R.string.action_connecting);
    }

    @Override
    public void onHapbeatConnected(@NonNull BluetoothDevice device) {
        hapbeatNameView.setText(hapbeatName);
        hapbeatConnectButton.setText(R.string.action_disconnect);
    }

    @Override
    public void onHapbeatDisconnecting(@NonNull BluetoothDevice device) {
        hapbeatConnectButton.setText(R.string.action_disconnecting);
    }

    @Override
    public void onHapbeatDisconnected(@NonNull BluetoothDevice device) {
        hapbeatConnectButton.setText(R.string.hapbeat_connect);
        hapbeatNameView.setText(getDefaultDeviceName());
        setDefaultUI();

        try {
            Logger.d(hapbeatLogSession, "Unbinding from the service...");
            unbindService(hapbeatServiceConnection);
            hapbeatService = null;

            Logger.d(hapbeatLogSession, "Activity unbound from the service");
            onServiceUnbound();
            hapbeatName = null;
            hapbeatBluetoothDevice = null;
            hapbeatLogSession = null;
        } catch (final IllegalArgumentException e) {
            // do nothing. This should never happen but does...
        }
    }

    @Override
    public void onHapbeatLinkLossOccurred(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onHapbeatServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {

    }

    @Override
    public void onHapbeatReady(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onHapbeatBondingRequired(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onHapbeatBonded(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onHapbeatBondingFailed(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onHapbeatError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {
        DebugLogger.e(TAG, "Error occurred: " + message + ",  error code: " + errorCode);
        showToast(message + " (" + errorCode + ")");
    }

    @Override
    public void onHapbeatNotSupported(@NonNull BluetoothDevice device) {
        showToast(R.string.not_supported);
    }

    /**
     * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
     *
     * @param message a message to be shown
     */
    protected void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(BleConnectActivity.this, message, Toast.LENGTH_LONG).show());
    }

    /**
     * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
     *
     * @param messageResId an resource id of the message to be shown
     */
    protected void showToast(final int messageResId) {
        runOnUiThread(() -> Toast.makeText(BleConnectActivity.this, messageResId, Toast.LENGTH_SHORT).show());
    }

    protected abstract void setDefaultUI();
}
