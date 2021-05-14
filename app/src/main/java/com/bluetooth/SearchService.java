package com.bluetooth;

//import java.io.ByteArrayOutputStream;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.R.array;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
//import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
//import android.view.View.OnClickListener;
import android.widget.AdapterView;
//import android.widget.AdapterView.OnItemClickListener;
//import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
//import android.widget.TextView;
import android.widget.Toast;

public class SearchService extends Activity {


	public static BluetoothAdapter myBluetoothAdapter;
	private static String TAG = "zpSDK";
	public String SelectedBDAddress;
	private static BluetoothDevice myDevice;
	//private TextView title;
	public BluetoothDevice device;
	private static OutputStream myOutStream = null;
	private static InputStream myInStream = null;

	private static BluetoothSocket mySocket = null;

	//private BluetoothAdapter bluetooth;
	private List<String> devicesList;
	private List<String> macAddressList;
	//private ListView listview;
	private ArrayAdapter<String> adapter;
	//private IntentFilter filter;

	//private String pathString;
	Context mcon;
	String Str;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_deviceslist);
		mcon = this;
		if (!ListBluetoothDevice())
			Toast.makeText(this, "没有找到蓝牙", Toast.LENGTH_LONG).show();//finish();

	}

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	public boolean ListBluetoothDevice() {

		device = null;

		//pathString = null;

		final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		ListView listView = (ListView) findViewById(R.id.devicesList);
		SimpleAdapter m_adapter = new SimpleAdapter(this, list,
				android.R.layout.simple_list_item_2,
				new String[]{"DeviceName", "BDAddress"},
				new int[]{android.R.id.text1, android.R.id.text2}
		);
		listView.setAdapter(m_adapter);

		if ((myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
			Toast.makeText(this, "没有找到蓝牙适配器", Toast.LENGTH_LONG).show();
			return false;
		}

		if (!myBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, 2);
		}

		Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() <= 0) return false;
		for (BluetoothDevice device : pairedDevices) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("DeviceName", device.getName());
			map.put("BDAddress", device.getAddress());
			list.add(map);
		}


		listView.setOnItemClickListener(new ListView.OnItemClickListener() {
			@TargetApi(Build.VERSION_CODES.ECLAIR)
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				SelectedBDAddress = list.get(arg2).get("BDAddress");
	        		/*if (((ListView)arg0).getTag() != null){
	        			((View)((ListView)arg0).getTag()).setBackgroundDrawable(null);
	        		}*/
				((ListView) arg0).setTag(arg1);
				arg1.setBackgroundColor(Color.BLUE);

				String macString = list.get(arg2).get("BDAddress");
				myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				device = myBluetoothAdapter.getRemoteDevice(macString);

				//final Button button = new Button(SearchService.this);

				AlertDialog.Builder builder = new AlertDialog.Builder(
						SearchService.this);
				builder.setTitle("更新固件")
						.setNegativeButton("取消", null);

				builder.setPositiveButton("开始更新",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
												int which) {
								try {
									upData(myBluetoothAdapter, device);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						});
				builder.show();
			}
		});

		return true;
	}


	void upData(final BluetoothAdapter BluetoothAdapter, final BluetoothDevice btDevice) throws Exception {
		final ProgressDialog dialog = new ProgressDialog(this);
		// 设置进度条类型
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		// 给进度条设置最大值
		dialog.setMax(100);
		dialog.setTitle("进度显示");
		dialog.setMessage("加载中...");
		dialog.show();

		/**--------------------------链接打印机-----------------------------*/
		if (!SPPOpen(BluetoothAdapter, btDevice)) {
			dialog.dismiss();
			showBuilder("正确链接蓝牙设备");
			return;
		}


		/**--------------------------打开本地更新文件-----------------------------*/

		final InputStream myInStream1;
		try {
			myInStream1 = mcon.getResources().getAssets().open("CCX-5.70.183pk.upd");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			dialog.dismiss();
			showBuilder("文件打开失败，请检查正确路径");
			return;
		}

		byte[] b = null;
		try {
			b = new byte[myInStream1.available()];
			myInStream1.read(b);
		} catch (IOException e) {
			e.printStackTrace();
			Str = "加载失败";
		}

		/**--------------------------向打印机请求打印机型号-----------------------------*/
		Thread.sleep(1000);
		SPPWrite(new byte[]{0x1F, 0x52, 0x10, 0x00, 0x20, 0x01, 0x4F, 0x45, 0x4D, 0x5F, 0x4D, 0x4F, 0x44, 0x45, 0x4C, 0x4E, 0x41, 0x4D, 0x45, 0x00}, 20);
		Thread.sleep(1000);
		byte[] temp = Read(800);
		byte[] temp1 = new byte[10];
		for (int i = 0; i < temp.length; i++) {
			temp1[i] = temp[i + 3];
			if (temp[i+3] == 0)
				break;
		}

		/**------------------------固件与机器是否吻合-----------------------------*/

		if (!compare(b,temp1))
		{
			dialog.dismiss();
			showBuilder("固件与打印机型号不匹配，请选择与之匹配的固件或打印机");
			return;
		}

		/**-----------------------固件升级-----------------------------*/

			final byte[] finalB = b;
			new Thread() {
				@Override
				public void run() {
					byte[] head=new byte[20];
					System.arraycopy(finalB,0,head,0, 20);
					SPPWrite(head,head.length);
					int datalen= finalB.length-20;
					int times=datalen/4096;
					byte[] data=new byte[4096];

					// 进展程度
					for (int i = 0; i < times; i++) {
						System.arraycopy(finalB,20+i*4096,data,0, 4096);
						SPPWrite(data,data.length);
						datalen-=4096;
						try {
							// 设置进展速度
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
							Str = "加载失败";
						}
						// 设置进度值
						dialog.setProgress(i);
					}
					if(datalen != 0){
						System.arraycopy(finalB,20+times*4096,data,0, datalen);
						SPPWrite(data,datalen);
					}
					dialog.setProgress(100);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						Str = "加载失败";
					}
					//SPPWrite(b,b.length);
					try {
						myInStream1.close();
					} catch (IOException e) {
						e.printStackTrace();
						Str = "加载失败";
					}
					SPPClose();
					Str = "加载成功，设备重启中，请稍后。。。";

					// 进度到达最大值，消失解散
					dialog.dismiss();

					showBuilder(Str);





				}
			}.start();

	}

	void showBuilder(String str) {
		if (Looper.myLooper() == null)
			Looper.prepare();
		AlertDialog.Builder builder = new AlertDialog.Builder(
				SearchService.this);
		builder.setTitle(str)
				.setNegativeButton("确定", null);
		builder.show();
		Looper.loop();
	}

	private UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	private boolean SPPOpen(BluetoothAdapter BluetoothAdapter, BluetoothDevice btDevice) {

		Log.e(TAG, "SPPOpen");
		myBluetoothAdapter = BluetoothAdapter;
		myDevice = btDevice;

		if (!myBluetoothAdapter.isEnabled()) {
			return false;
		}
		myBluetoothAdapter.cancelDiscovery();
		try {
			mySocket = myDevice.createRfcommSocketToServiceRecord(SPP_UUID);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		try {
			mySocket.connect();
		} catch (IOException e2) {
			return false;
		}

		try {
			myOutStream = mySocket.getOutputStream();
		} catch (IOException e3) {
			try {
				mySocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		try {
			myInStream = mySocket.getInputStream();
		} catch (IOException e3) {
			try {
				mySocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		Log.e(TAG, "SPPOpen OK");
		return true;
	}


	private void SPPClose() {
		try {
			mySocket.close();
		} catch (IOException e) {
		}
	}

	private static boolean SPPWrite(byte[] Data, int DataLen) {

		try {
			myOutStream.write(Data, 0, DataLen);
		} catch (IOException e) {

			return false;
		}
		return true;
	}


	private final BroadcastReceiver receiver = new BroadcastReceiver() {

		@TargetApi(Build.VERSION_CODES.ECLAIR)
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				for (int i = 0; i < devicesList.size(); i++) {
					if (devicesList.get(i).equals(device.getName())) {
						return;
					}
				}
				devicesList.add(device.getName());
				macAddressList.add(device.getAddress());
				adapter.notifyDataSetChanged();
			}
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				//unregisterReceiver(receiver);
			}
		}
	};

	/*private final BroadcastReceiver unReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			context.unregisterReceiver(this);
		};
	};*/


	boolean compare(byte []firmare,byte[] printerVer)
	{
		String fir = hex2Str(fromFirmareToVer(firmare));
		String ver =hex2Str(byte2Hex(printerVer));
		Log.e(TAG, "固件型号:-----------"+ fir);
		Log.e(TAG, "打印机型号: -----------"+ ver);
		if (fir.equals(ver))return true;
		if (fir.substring(0,2).equals(ver.substring(0,2)))return true;
		return false;
	}


	private static void flush()
	{
		try {
			myInStream.skip(myInStream.available());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static byte[] Read(int Timeout) {
		byte[] Data = new byte[1024];
		for (int i = 0; i < Timeout ; i++) {
			try {
				if (myInStream.available() >= 0)
					try {
						myInStream.read(Data);
						//Log.e("zpSDK", "Data" + Arrays.toString(Data));
						return (Data);
					} catch (IOException e) {
						return null;
					}
			} catch (IOException e) {
				return null;
			}
		}
		try {
			Thread.sleep(5L);
		} catch (InterruptedException e) {
			return null;
		}
		return null;

	}

	private static String byte2Hex(byte[] bytes){
		StringBuffer stringBuffer = new StringBuffer();
		String temp = null;
		for (int i=0;i<bytes.length;i++){
			temp = Integer.toHexString(bytes[i] & 0xFF);
			if (temp.length()==1){
//1得到一位的进行补0操作
				stringBuffer.append("0");
			}
			stringBuffer.append(temp);
		}

		//Log.e(TAG, "byte2Hex: ++++"+stringBuffer.toString());
		return stringBuffer.toString();
	}

	private static String hex2Str(String hex) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hex.length() - 1; i += 2) {
			String h = hex.substring(i, (i + 2));
			if (h.equals("00"))
				break;
			int decimal = Integer.parseInt(h, 16);
			sb.append((char) decimal);
		}
		//Log.e(TAG, "hex2Str:---------"+sb.toString());
		return sb.toString();
	}

	private static byte[] replaceZero(byte[] bytes) {
		int len = 0;
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == 0) {
				len = i;
				break;
			}
		}
		byte[] b = new byte[len];
		System.arraycopy(bytes, 0, b, 0, len);
		//Log.e("zpSDK","Data:"+new String(b));
		if(new String(b).equals("0"))
			return null;
		return b;
	}

	String fromFirmareToVer(byte []b)
	{

		int i=0;
		for (;i<b.length;i++)
		{
			if (b[i]==0)
				break;
		}
		byte []tm = new byte[i];
		System.arraycopy(b, 4, tm ,0, i);
		return  byte2Hex(tm);
	}



}
