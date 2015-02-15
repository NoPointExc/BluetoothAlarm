package com.sey.bluetoothalarm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;
	// Name of the connected device
	private String mConnectedDeviceName = null;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	// Layout Views
	private TextView mTitle;
	// textView1~4
	private TextView[] textView;
	// sensors on/off
	private boolean sensorOn[] = { false, false, false, false };
	// Member object for the chat services
	private BluetoothChatService mChatService = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textView = new TextView[4];

		textView[0] = (TextView) findViewById(R.id.textView_0);
		textView[1] = (TextView) findViewById(R.id.textView_1);
		textView[2] = (TextView) findViewById(R.id.textView_2);
		textView[3] = (TextView) findViewById(R.id.textView_3);
		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				Log.d(TAG, "setup Bluetooth");
			// Initialize the BluetoothChatService to perform bluetooth
			// connections
			mChatService = new BluetoothChatService(this, mHandler);
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

		}
	}

	// the listener of the switch_sensor_1~switch_sensor_4
	public void onSwitchClick(View view) {
		// send out message in String "A/B""1~4" to toggle the sensors
		// for example, A1 suggests to turn on sensor1 , B1 suggests to turn off
		// the sensor 1.
		switch (view.getId()) {
		case R.id.switch_sensor_1:
			sensorOn[0] = !sensorOn[0];
			if (sensorOn[0]) {
				sendMessage("A1");
			} else {
				sendMessage("B1");
			}
			break;

		case R.id.switch_sensor_2:
			sensorOn[1] = !sensorOn[1];
			if (sensorOn[1]) {
				sendMessage("A2");
			} else {
				sendMessage("B2");
			}
			break;

		case R.id.switch_sensor_3:
			sensorOn[2] = !sensorOn[2];
			if (sensorOn[2]) {
				sendMessage("A3");
			} else {
				sendMessage("B3");
			}
			break;

		case R.id.switch_sensor_4:
			sensorOn[3] = !sensorOn[3];
			if (sensorOn[3]) {
				sendMessage("A4");
			} else {
				sendMessage("B4");
			}
			break;

		}
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					// mConversationArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				//byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				//String writeMessage = new String(writeBuf);
				// mConversationArrayAdapter.add("Me:  " + writeMessage);
				// ***************************Test output
				// message*****************
				Toast.makeText(getApplicationContext(), "success!",
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				if (readMessage.length() > 0 && readMessage.length() == 4) {
					// the message received here is readMessage
					// Warning!CAN NOT ENSURE readMessage is in "1111" formate
					updateSensorStates(readMessage);

				}
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				// if
				// (!msg.getData().getString(TOAST).contains("Unable to connect device"))
				// {
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				// }
				break;
			}
		}
	};

	/**
	 * update all textView.
	 * 
	 * @param message
	 *            A string receive from Bluetooth.
	 */
	private void updateSensorStates(String s) {

		try {
			for (int i = 0; i < 4; i++) {
				if (s.charAt(i) == '1') {
					// 1= warning. 0= OK
					textView[i].setText("WARNING");
					dial();
				} else {
					// update the textView
					textView[i].setText("OK");
				}

			}
		} catch (NumberFormatException e) {
			Log.e(TAG, "updateSensorStates(String s)_NumberFormatException");
		} catch (Exception e) {
			Log.e(TAG,
					"updateSensorStates(String s)_Exception:" + e.getMessage());
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mChatService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				Log.d(TAG, "setup Bluetooth");
				// Initialize the BluetoothChatService to perform bluetooth
				// connections
				mChatService = new BluetoothChatService(this, mHandler);
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	/**
	 * Dial 911
	 * 
	 * @param none
	 * 
	 */

	private void dial() {
		// NOTE: No one other than system app is allowed to dial emergency
		// number
		Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"
				+ "911"));
		// Intent dialIntent=new Intent(Intent.ACTION_CALL,
		// Uri.parse("tel:"+"911"));
		this.startActivity(dialIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

}
