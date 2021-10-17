/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.onodera.BleApp.template;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.onodera.BleApp.R;
import com.onodera.BleApp.template.network.ServerThread;
import com.onodera.BleApp.template.network.UdpClientService;
import com.onodera.BleApp.template.network.UdpServerService;
import com.onodera.BleApp.template.signal.GraphActivity;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;

/**
 * Modify the Template Activity to match your needs.
 */
public class BleMainActivity extends BleConnectActivity implements ServerThread.ServerListener {
	@SuppressWarnings("unused")
	private final String TAG = "TemplateActivity";

	private UdpClientService.LocalBinder mUdpClientService;
	private UdpServerService.LocalBinder mUdpServerService;
	private TextView valueView;
	//private SeekBar seekBarView;
	//private TextView seekTextView;
	private EditText editPhoneView;
	private TextView Phoneview;
	private Button   PhoneConnectButton;
	private Switch mSwitch;
	private TextView IpAddressView;
	private OutputControlService.LocalBinder outputControlService;
	private HapbeatService.Network mNetwork = HapbeatService.Network.local;

	//private TextView batteryLevelView;

	private ServiceConnection udpClientServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			BleMainActivity.this.mUdpClientService = (UdpClientService.LocalBinder) service;
			PhoneConnectButton.setText("DISCONNECT");

		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			PhoneConnectButton.setText("CONNECT");
			mUdpClientService = null;
		}
	};

	private ServiceConnection udpServerServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			BleMainActivity.this.mUdpServerService = (UdpServerService.LocalBinder) service;
			mUdpServerService.setListener(BleMainActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mUdpServerService = null;
		}
	};

	private ServiceConnection outputControlServiceConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			BleMainActivity.this.outputControlService = (OutputControlService.LocalBinder) service;
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			outputControlService = null;
		}
	};


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent service3;
		service3 = new Intent(BleMainActivity.this, UdpServerService.class);
		startService(service3);
		//final Intent volumeControlService;
		//volumeControlService = new Intent(BleMainActivity.this, OutputControlService.class);
		//startService(volumeControlService);

	}

	@Override
	protected void onStart() {
		super.onStart();
		final Intent service3;
		service3 = new Intent(BleMainActivity.this, UdpServerService.class);
		bindService(service3, udpServerServiceConnection, 0);
		final Intent service4 = new Intent(BleMainActivity.this, UdpClientService.class);
		bindService(service4, udpClientServiceConnection, 0);
		//final Intent volumeControlService;
		//volumeControlService = new Intent(BleMainActivity.this, OutputControlService.class);
		//bindService(volumeControlService, outputControlServiceConnection, 0);

		//Log.d("cdebug", getMessage());


	}


	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		// TODO modify the layout file(s). By default the activity shows only one field - the Heart Rate value as a sample
		setContentView(R.layout.activity_feature_template);
		setGUI();
	}

	/**
	 * Called after the view and the toolbar has been created.
	 */
	@Override
	protected void setUpView() {
		// set GUI
		super.setUpView();
		editPhoneView = findViewById(R.id.editPhoneText);
		IpAddressView = findViewById(R.id.ip_address);
		IpAddressView.setTextIsSelectable(true);
		//Phoneview = findViewById(R.id.phone_name);
		PhoneConnectButton = findViewById(R.id.phone_connect);
		mSwitch = findViewById(R.id.NetworkSwitch);
		mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				if (mSwitch.isChecked()){
					//if (hapbeatService!=null) hapbeatService.setNetwork(HapbeatService.Network.UDP);
					mNetwork = HapbeatService.Network.UDP;
				} else {
					//if (hapbeatService!=null) hapbeatService.setNetwork(HapbeatService.Network.local);
					mNetwork = HapbeatService.Network.local;
				}
			}
		});
	}

	private void setGUI() {
		// TODO assign your views to fields
		valueView = findViewById(R.id.value);
		/*
		seekBarView = findViewById(R.id.seekBar);
		seekTextView = findViewById(R.id.value2);

		int p = seekBarView.getProgress();
		String s = "volume: " + p/10.0;
		seekTextView.setText(s);

		 */
		//outputControlService.setVolumeScale(p);

		/*
		seekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				String s = "volume: " + i/10.0;
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

		 */

		//batteryLevelView = findViewById(R.id.battery);

		/*
		findViewById(R.id.action_set_name).setOnClickListener(v -> {
			if (isDeviceConnected()) {
				getService().performAction("Template");
			}
		});

		 */
	}

	/*
	@Override
	protected void onInitialize(final Bundle savedInstanceState) {
		//LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeIntentFilter());
	}*/

	@Override
	protected void onStop() {
		super.onStop();
		try {
			if (mUdpServerService != null)
				mUdpServerService.setActivityIsChangingConfiguration(isChangingConfigurations());
			if (mUdpClientService != null)
				mUdpClientService.setActivityIsChangingConfiguration(isChangingConfigurations());
			//if (outputControlService != null)
			//	outputControlService.setActivityIsChangingConfiguration(isChangingConfigurations());
			//unbindService(outputControlServiceConnection);
			unbindService(udpServerServiceConnection);
			unbindService(udpClientServiceConnection);
			//mUdpServerService = null;
			//mUdpClientService = null;
			//outputControlService = null;
		} catch (final IllegalArgumentException e){

		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Intent service = new Intent(BleMainActivity.this, UdpServerService.class);
		stopService(service);
		mUdpServerService = null;
		mUdpClientService = null;
		//LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
		String text = PhoneConnectButton.getText().toString();
		if(text=="DISCONNECT"){
			Intent service2 = new Intent(BleMainActivity.this, UdpClientService.class);
			stopService(service2);
			PhoneConnectButton.setText("CONNECT");
		}

	}

	@Override
	protected void setDefaultUI() {
		// TODO clear your UI
		valueView.setText(R.string.not_available_value);
		//batteryLevelView.setText(R.string.not_available);
	}

	@Override
	protected int getLoggerProfileTitle() {
		return R.string.template_feature_title;
	}


	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.signal, menu);
		return true;
	}

/*
	@Override
	protected boolean onOptionsItemSelected(final int itemId) {
		switch (itemId) {
			case R.id.sending_signal:
				final Intent intent = new Intent(getApplication(), GraphActivity.class);
				startActivity(intent);
				break;
		}
		return true;
	}
*/




	@Override
	protected int getDefaultDeviceName() {
		return R.string.template_default_name;
	}

	// Handling updates from the device
	@SuppressWarnings("unused")
	private void setValueOnView(@NonNull final BluetoothDevice device, final int value) {
		// TODO assign the value to a view
		valueView.setText(String.valueOf(value));
	}

	@SuppressWarnings("unused")
	public void onBatteryLevelChanged(@NonNull final BluetoothDevice device, final int value) {
		//batteryLevelView.setText(getString(R.string.battery, value));
	}

	public void onConnectPhoneClicked(final View view){
		String text = editPhoneView.getText().toString();


		if(mUdpClientService == null) {
			final Intent service = new Intent(this, UdpClientService.class);
			service.putExtra("IpAddress", text);
			startService(service);
			bindService(service, udpClientServiceConnection, 0);

		} else {
			mUdpClientService.disconnect();
			//unbindService(udpClientServiceConnection);
			mUdpClientService = null;
		}

	}

	public void onIpGetClicked(final View view){
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
			Network currentNetwork = connectivityManager.getActiveNetwork();
			if (currentNetwork==null) {
				IpAddressView.setText("NULL");
				return;
			}
			LinkProperties linkProperties = connectivityManager.getLinkProperties(currentNetwork);

			List<LinkAddress> addresses = linkProperties.getLinkAddresses();
			for (LinkAddress address : addresses) {
				InetAddress addr = address.getAddress();
				if (addr instanceof Inet4Address) {
					IpAddressView.setText(addr.toString());
					break;
				}
			}
		}
	}

/*
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			final BluetoothDevice device = intent.getParcelableExtra(AccelerometerService.EXTRA_DEVICE);

			if (AccelerometerService.BROADCAST_TEMPLATE_MEASUREMENT.equals(action)) {
				byte[] value = intent.getByteArrayExtra(AccelerometerService.EXTRA_DATA);

				// Update GUI
				int[] intValue = new int[20];
				for(int i=0; i<20; i++){
					intValue[i] = value[i] & 0xFF;
				}
				setValueOnView(device, intValue[1]);
			} else if (AccelerometerService.BROADCAST_BATTERY_LEVEL.equals(action)) {
				final int batteryLevel = intent.getIntExtra(AccelerometerService.EXTRA_BATTERY_LEVEL, 0);
				// Update GUI
				onBatteryLevelChanged(device, batteryLevel);
			}
		}
	};

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(AccelerometerService.BROADCAST_TEMPLATE_MEASUREMENT);
		intentFilter.addAction(AccelerometerService.BROADCAST_BATTERY_LEVEL);
		return intentFilter;
	}
 */

	@Override
	public void onAccelerometerSend(byte[] value) {
			if(mUdpClientService!=null) mUdpClientService.addDataToQueue(value);
			else if (hapbeatService!=null && mNetwork==HapbeatService.Network.local) hapbeatService.addDataToHapbeatQueue(value);
	}

	@Override
	public void onServerToHapbeatSend(byte[] value) {
		if (hapbeatService!=null && mNetwork==HapbeatService.Network.UDP) hapbeatService.addDataToHapbeatQueue(value);
	}

	/*
	@Override
	public void onDeviceSelected(@NonNull BluetoothDevice device, @Nullable String name) {
		super.onDeviceSelected(device, name);
		if (mUuid.equals(HapbeatManager.HAPBEAT_SERVICE_UUID)) {
			hapbeatService.setVolumeScale(seekBarView.getProgress());
		}
	} */
}
