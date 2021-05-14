package com.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {

	private TextView title;
	//private Button btnOpen, btnSearch, btnFile, btnSet;
	private Button  btnSearch;

	private String NAME = "BLServer";

	//private MyOpenClickListener openListener;
	private MySearchClickListener searchListener;
	//private MyFileClickListener fileListener;
	//private MySetClickListener setListener;

	private AcceptService acceptThread;
	private BluetoothAdapter bluetoothAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_main);

		//btnOpen = (Button) findViewById(R.id.btn_open);
		btnSearch = (Button) findViewById(R.id.btn_search);
		//btnFile = (Button) findViewById(R.id.btn_file);
	//	btnSet = (Button) findViewById(R.id.btn_set);

		//openListener = new MyOpenClickListener();
		searchListener = new MySearchClickListener();
	//	fileListener = new MyFileClickListener();
	//	setListener = new MySetClickListener();

	//	btnOpen.setOnClickListener(openListener);
		btnSearch.setOnClickListener(searchListener);
		//btnFile.setOnClickListener(fileListener);
	//	btnSet.setOnClickListener(setListener);

		title = (TextView) findViewById(R.id.title);
		title.setText("zicox打印机固件空中升级程序");

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	}
	/*
	 * 打开蓝牙设备
	 */
	class MyOpenClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {

			Toast.makeText(MainActivity.this, "正在玩命开启蓝牙！", Toast.LENGTH_LONG)
					.show();
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					enableBluetooth();
					// 等待accept中...
					acceptThread = new AcceptService();
					acceptThread.start();
				}
			}, 2000);
		}

	}

	public int enableBluetooth() {
		if (bluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth Is Not Available!",
					Toast.LENGTH_LONG).show();
		}
		if (bluetoothAdapter.isEnabled()) {
			Toast.makeText(this, "蓝牙已经开启!", Toast.LENGTH_LONG).show();
		} else {
			bluetoothAdapter.enable();
			Toast.makeText(this, "蓝牙启动成功!", Toast.LENGTH_LONG).show();
		}

		// 允许附近检测该蓝牙设备
		Intent intent2 = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivity(intent2);
		return 0;
	}

	/*
	 * 搜索蓝牙设备
	 */
	class MySearchClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Intent intent = new Intent(MainActivity.this, SearchService.class);
			startActivity(intent);
		}
	}

	class MyFileClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Intent intent = new Intent(MainActivity.this, FileService.class);
			startActivity(intent);
		}
	}

	class MySetClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(MainActivity.this, SetService.class);
			startActivity(intent);
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String buf = msg.getData().getString("Msg");
			Toast.makeText(MainActivity.this, buf, Toast.LENGTH_LONG).show();
		}
	};

	private void ReadMessage(BluetoothSocket socket) {
		InputStream inputStream = null;
		try {
			inputStream = socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (true) {
			int count = 0;
			byte[] buf = new byte[4096];
			try {
				count = inputStream.read(buf);
				if (count > 0) {
					byte[] data = new byte[4096];
					for (int i = 0; i < count; i++) {
						data[i] = buf[i];
					}
					String s = new String(data);
					Message msg = new Message();
					Bundle bundle = new Bundle();
					bundle.putString("Msg", s);
					msg.setData(bundle);
					handler.sendMessage(msg);
					break;
				}
			} catch (IOException e) {
				break;
			}
		}
		try {
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public class AcceptService extends Thread {
		private BluetoothServerSocket bluetoothServerSocket;
		private BluetoothSocket bluetoothSocket;
		public AcceptService() {
			BluetoothServerSocket tmp = null;
			try {
				tmp = bluetoothAdapter
						.listenUsingRfcommWithServiceRecord(
								NAME,
								UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (tmp != null) {
				bluetoothServerSocket = tmp;
			}
		}
		public void run() {
			if (bluetoothAdapter.isDiscovering()) {
				bluetoothAdapter.cancelDiscovery();
			}
			while (true) {
				if (bluetoothServerSocket != null) {
					try {
						bluetoothSocket = bluetoothServerSocket.accept();
					} catch (IOException e) {
						break;
					}
					if (bluetoothSocket != null) {
						Log.v("123", "accept ok");
						ReadMessage(bluetoothSocket);
						try {
							bluetoothSocket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
