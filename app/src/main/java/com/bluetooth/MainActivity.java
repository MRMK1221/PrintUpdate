package com.bluetooth;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.bluetooth.service.*;

import com.bluetooth.common.base.BaseActivity;

import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlProgressListener;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.bucket.GetBucketRequest;
import com.tencent.cos.xml.model.bucket.GetBucketResult;
import com.tencent.cos.xml.model.tag.ListBucket;
import com.tencent.cos.xml.transfer.COSXMLDownloadTask;
import com.tencent.cos.xml.transfer.TransferConfig;
import com.tencent.cos.xml.transfer.TransferManager;
import com.tencent.cos.xml.transfer.TransferState;
import com.tencent.cos.xml.transfer.TransferStateListener;

@SuppressLint("HandlerLeak")
public class MainActivity extends BaseActivity {

	private TextView title;
	public BluetoothDevice device;
	private static BluetoothDevice myDevice;
	public static BluetoothAdapter myBluetoothAdapter;
	private static String TAG = "zpSDK";
	public String SelectedBDAddress;
	List<Map<String, String>> printDeviceList;
	List<String> printNameList;
	private BluetoothAdapter adapter;
	Button bu;
	ListView listView;
	List<String> lockFiles;
	private static OutputStream myOutStream = null;
	private static InputStream myInStream = null;

	private static BluetoothSocket mySocket = null;





	private CosXmlService cosXmlService;
	private TransferManager transferManager;
	private COSXMLDownloadTask cosxmlTask;
	private File downloadParentDir;

	List<String> netFileName;

	private ProgressDialog progressDialog;
	private ProgressDialog dialog;
	private String UpdataPath=null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_main);

		printDeviceList = new ArrayList<>();
		printNameList = new ArrayList<>();
		lockFiles = new ArrayList<>();
		netFileName = new ArrayList<>();
		listView = (ListView) findViewById(R.id.lv_bt);
		adapter = BluetoothAdapter.getDefaultAdapter();
		openLockFile();

		LockBlutoothDevice();

		ListBluetoothDevice();

		bu = (Button) findViewById(R.id.btn_scan);
		bu.setOnClickListener(new View.OnClickListener() {
			@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
			@Override
			public void onClick(View view) {
				ScanBlutoothDevice();
			}
		});


		title = (TextView) findViewById(R.id.title);
		title.setText("zicox?????????????????????????????????");


		progressDialog = ProgressDialog.show(MainActivity.this,"Loading...", "Please wait...", true, false);
		progressDialog.setCancelable(false);

        dialog = new ProgressDialog(this);
		// ?????????????????????
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		// ???????????????????????????
		dialog.setCancelable(false);
		dialog.setMax(100);
		dialog.setTitle("??????????????????");
		dialog.setMessage("????????????????????????--"+"\r\n?????????...?????????...");
		dialog.dismiss();


		initService();

	}

	void initService(){

		cosXmlService = CosServiceFactory.getCosXmlService(this, BuildConfig.COS_SECRET_ID, BuildConfig.COS_SECRET_KEY, false);
		TransferConfig transferConfig = new TransferConfig.Builder().build();
		transferManager = new TransferManager(cosXmlService, transferConfig);
		downloadParentDir = getExternalFilesDir("/");
		getObject();
	}

	void getObject()
	{
		final GetBucketRequest getBucketRequest = new GetBucketRequest(BuildConfig.COS_BUCKET_NAME);

		// ??????????????????????????????????????????1000
		getBucketRequest.setMaxKeys(100);

		// ???????????????????????????????????? Prefix???
		// ?????? Prefix ??? delimiter ????????????????????????????????????????????? Common Prefix???
		// ?????????????????? Common Prefix??????????????? Prefix???????????????????????????
		getBucketRequest.setDelimiter("/");

		// ????????????????????????
		cosXmlService.getBucketAsync(getBucketRequest, new CosXmlResultListener() {
			@Override
			public void onSuccess(CosXmlRequest request, CosXmlResult result) {
				final GetBucketResult getBucketResult = (GetBucketResult) result;
				uiAction(new Runnable() {
					@Override
					public void run() {
						if (getBucketResult.listBucket.contentsList.size()>0)
						{
							List<ListBucket.Contents> temp = getBucketResult.listBucket.contentsList;
							for (int i=0;i<temp.size();i++)
							{
								netFileName.add(temp.get(i).key);
							}
						}
						progressDialog.dismiss();
					}
				});
			}
			@Override
			public void onFail(CosXmlRequest cosXmlRequest, CosXmlClientException clientException, CosXmlServiceException serviceException) {
				//??????????????????loading  ???????????????loading
				clientException.printStackTrace();
				serviceException.printStackTrace();
			}
		});
	}


 @Override
 public void onResume() {

	 super.onResume();
	 openLockFile();
 }

	public void openLockFile() {

		File[] allfiles = getExternalFilesDir("/").listFiles();
		if (allfiles == null) {
			return;
		}
		for (int k = 0; k < allfiles.length; k++) {
			final File fi = allfiles[k];
			if (fi.isFile()) {
				int idx = fi.getPath().lastIndexOf(".");
				if (idx <= 0) {
					continue;
				}
				String suffix = fi.getPath().substring(idx);
				if (suffix.toLowerCase().equals(".upd")) {
					lockFiles.add(fi.getPath());
				}
			}
		}
	}

	String findLockFirmware(String printerName)
	{
		String newprint = printerName.split("_")[0];
		String FName=null;
		if (lockFiles.size()<=0)
			return null;
		for (int i=0;i<lockFiles.size();i++)
		{
			String newfir = lockFiles.get(i).split("-")[0];
			String[] newfir1 = newfir.split("/");
			String newfir2 = newfir1[newfir1.length-1];
			if (newfir2.equals(newprint)){
				return lockFiles.get(i);
			}

		}
		return FName;
	}

	String findNetFirmware(String printerName)
	{
		String newprint = printerName.split("_")[0];
		String FName=null;
		if (netFileName.size()<=0)
			return null;
		for (int i=0;i<netFileName.size();i++)
		{
			String newfir = netFileName.get(i).split("-")[0];
			if (newfir.equals(newprint)){
				return netFileName.get(i);
			}

		}
		return FName;
	}



	public void LockBlutoothDevice()
	{
		if ((myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
			Toast.makeText(this, "???????????????????????????", Toast.LENGTH_LONG).show();
		}
		Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0)
		{
			for (BluetoothDevice device : pairedDevices) {
				Map<String, String> map = new HashMap<String, String>();
				String str = "";
				for (int i=0;i<32;i++)
				{
					if (i>device.getName().length()-1)
						str+=" ";
					else
					   str+=String.valueOf(device.getName().charAt(i));
				}
				str+="?????????";
				map.put("DeviceName",str);
				map.put("BDAddress", device.getAddress());
				printDeviceList.add(map);
				printNameList.add(device.getName());
			}
		}
		setlistView();
	}



	public void ScanBlutoothDevice() {


		//checkPermission();
		if (adapter != null) {
			System.out.println("????????????????????????");
			//???????????????????????????
			if (!adapter.isEnabled()) {

					Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				//????????????????????????
				startActivity(intent);
			}

			if (Build.VERSION.SDK_INT >= 23) {
				if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
				}
			}


			IntentFilter intent = new IntentFilter();
			intent.addAction(BluetoothDevice.ACTION_FOUND);//??????????????????
			intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//??????????????????
			intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//????????????
			intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//????????????
			registerReceiver(searchDevices, intent);

			//?????????????????????????????????,???????????????????????????????????????????????????????????????
			adapter.startDiscovery();


			} else {
				System.out.println("???????????????????????????");
			}

		}

	public void checkPermission()
	{
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
			Log.i(TAG, "sdk < 28 Q");
			if (ActivityCompat.checkSelfPermission(this,
					Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
					|| ActivityCompat.checkSelfPermission(this,
					Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				String[] strings =
						{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
				ActivityCompat.requestPermissions(this, strings, 1);
			}
		} else {
			if (ActivityCompat.checkSelfPermission(this,
					Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
					|| ActivityCompat.checkSelfPermission(this,
					Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
					|| ActivityCompat.checkSelfPermission(this,
					"android.permission.ACCESS_BACKGROUND_LOCATION") != PackageManager.PERMISSION_GRANTED) {
				String[] strings = {android.Manifest.permission.ACCESS_FINE_LOCATION,
						android.Manifest.permission.ACCESS_COARSE_LOCATION,
						"android.permission.ACCESS_BACKGROUND_LOCATION"};
				ActivityCompat.requestPermissions(this, strings, 2);
			}
		}

	}


	private BroadcastReceiver searchDevices = new BroadcastReceiver() {

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(BluetoothDevice.ACTION_FOUND)) { //found device
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String str1 = device.getName();
				Log.e(TAG, "onReceive str: "+str1 );
					if (printNameList.indexOf(str1) == -1)// ??????????????????
				{
					Log.e(TAG, "onReceive str: "+device.getBluetoothClass().getDeviceClass());
					if (device.getBluetoothClass().getDeviceClass()==1664){
						Map<String, String> map = new HashMap<String, String>();
						String str = "";
						for (int i=0;i<32;i++)
						{
							if (i>device.getName().length()-1)
								str+=" ";
							else
								str+=String.valueOf(device.getName().charAt(i));
						}
						str+="?????????";
						map.put("DeviceName",str);
						map.put("BDAddress", device.getAddress());
						printNameList.add(device.getName());
						printDeviceList.add(map);
						setlistView();
					}

				}

			} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
				Toast.makeText(getBaseContext(), "????????????", Toast.LENGTH_SHORT).show();
				bu.setText("????????????");
			} else if (action
					.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				bu.setText("????????????");
				Toast.makeText(getBaseContext(), "??????????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
			}
		}
	};

	public void setlistView()
	{

		SimpleAdapter m_adapter = new SimpleAdapter(this, printDeviceList,
				android.R.layout.simple_list_item_2,
				new String[]{"DeviceName", "BDAddress"},
				new int[]{android.R.id.text1, android.R.id.text2}
		);

		listView.setAdapter(m_adapter);

	}


	public void ListBluetoothDevice() {

		listView.setOnItemClickListener(new ListView.OnItemClickListener() {
			@TargetApi(Build.VERSION_CODES.ECLAIR)
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				SelectedBDAddress = printDeviceList.get(arg2).get("BDAddress");
				((ListView) arg0).setTag(arg1);
				arg1.setBackgroundColor(Color.BLUE);
				String macString = printDeviceList.get(arg2).get("BDAddress");
				final String PrinterName = printDeviceList.get(arg2).get("DeviceName");
				myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				device = myBluetoothAdapter.getRemoteDevice(macString);

				//final Button button = new Button(SearchService.this);

				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);
				builder.setTitle("????????????")
						.setNegativeButton("??????", null);
				builder.setPositiveButton("????????????",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
												int which) {
								if (device.getName().split("_")[0].equals("XT423")) {
									updataXT423();
								}
								else {
									try {
										UpdataPath = findLockFirmware(device.getName());
										Log.e(TAG, "upData: "+UpdataPath );
										if(UpdataPath==null) {
											showBuilder("?????????????????????",2,device.getName());
										}else {
											String[] npath = UpdataPath.split("/");
											String temp = npath[npath.length-1];
											String gujianName = findNetFirmware(device.getName());
											if (!temp.equals(gujianName))
												showBuilder("?????????????????????",2,device.getName());
											else
												upData(myBluetoothAdapter, device,UpdataPath);
										}
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								dialog.dismiss();
							}

						});
				builder.show();
			}
		});
	}

	void updataXT423()
	{
		new AlertDialog.Builder(this)
				.setTitle("?????????")
				.setIcon(android.R.drawable.ic_dialog_info)
				.setSingleChoiceItems(new String[] {"XT423-205","XT423-405"}, 0,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								String FramName="";
								if (which==0)
								{
									FramName="XT423+205";

								}else if (which==1)
								{
									FramName="XT423+405";
								}
								try {
									UpdataPath = findLockFirmware(FramName);
									Log.e(TAG, "upData: "+UpdataPath );
									if(UpdataPath==null) {
										showBuilder("?????????????????????",2,FramName);
									}else {
										String[] npath = UpdataPath.split("/");
										String temp = npath[npath.length-1];
										String gujianName = findNetFirmware(FramName);
										if (!temp.equals(gujianName))
											showBuilder("?????????????????????",2,FramName);
										else
											upData(myBluetoothAdapter, device,UpdataPath);
									}
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
						}
				)
				.setNegativeButton("??????", null)
				.show();
	}

	/**
	 * ??????????????????
	 * @param state ?????? {@link TransferState}
	 */
	private void refreshState(final TransferState state) {
//		uiAction(new Runnable() {
//			@Override
//			public void run() {
//				tv_state.setText(state.toString());
//			}
//		});
	}

	/**
	 * ??????????????????
	 * @param progress ?????????????????????
	 * @param total ???????????????
	 */
	private void refreshProgress(final long progress, final long total) {
		uiAction(new Runnable() {
			@SuppressLint("SetTextI18n")
			@Override
			public void run() {
				dialog.setProgress((int) (100 * progress / total));
			}
		});
	}


	void download(String printerName)
	{
		final String gujianName = findNetFirmware(printerName);
		if (gujianName==null) {
			showBuilder("???????????????????????????????????????",0,"");
			return;
		}
		dialog.show();
		if (cosxmlTask == null) {
			cosxmlTask = transferManager.download(this, BuildConfig.COS_BUCKET_NAME, gujianName,
					downloadParentDir.toString(), gujianName);

			cosxmlTask.setTransferStateListener(new TransferStateListener() {
				@Override
				public void onStateChanged(final TransferState state) {
					refreshState(state);
				}
			});

			cosxmlTask.setCosXmlProgressListener(new CosXmlProgressListener() {
				@Override
				public void onProgress(final long complete, final long target) {
					refreshProgress(complete, target);
				}
			});

			cosxmlTask.setCosXmlResultListener(new CosXmlResultListener() {
				@SuppressLint("SetTextI18n")
				@Override
				public void onSuccess(CosXmlRequest request, CosXmlResult result) {
					cosxmlTask = null;
					toastMessage("????????????");
					Log.e(TAG, "onSuccess: "+"????????????");
					dialog.dismiss();
					UpdataPath = downloadParentDir.toString() + "/" + gujianName;
					openLockFile();
					showBuilder("?????????????????????????????????",1,"");

				}

				@Override
				public void onFail(CosXmlRequest request, CosXmlClientException exception, CosXmlServiceException serviceException) {
					if (cosxmlTask.getTaskState() != TransferState.PAUSED) {
						cosxmlTask = null;
						toastMessage("????????????");
						showBuilder("??????????????????????????????",3,device.getName());
					}
					if (exception != null) {
						exception.printStackTrace();
					}
					if (serviceException != null) {
						serviceException.printStackTrace();
					}
				}
			});

		}


	}

	void upData(final BluetoothAdapter BluetoothAdapter, final BluetoothDevice btDevice,String path) throws Exception {

//		if (Looper.myLooper() == null)
//			Looper.prepare();

		final ProgressDialog dialog = new ProgressDialog(this);

		String[] npath = path.split("/");

		// ?????????????????????
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		// ???????????????????????????
		dialog.setCancelable(false);
		dialog.setMax(100);
		dialog.setTitle("????????????");
		dialog.setMessage("????????????????????????--"+"\r\n?????????...?????????...");
		dialog.show();

		//Looper.loop();

		if (!SPPOpen(BluetoothAdapter, btDevice)) {
			dialog.dismiss();
			showBuilder("????????????????????????",0,"");
			return;
		}
		Log.e(TAG, "???????????????: -----------"+ btDevice.getName());

		/**--------------------------????????????????????????-----------------------------*/

		final InputStream myInStream1;
		try {
			myInStream1 = new FileInputStream(new File(path));
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			dialog.dismiss();
			showBuilder("?????????????????????",0,"");
			return;
		}

		byte[] b = null;
		try {
			b = new byte[myInStream1.available()];
			myInStream1.read(b);
		} catch (IOException e) {
			e.printStackTrace();
			dialog.dismiss();
			showBuilder("????????????",0,"");
			return;
		}


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

				// ????????????
				for (int i = 0; i < times; i++) {
					System.arraycopy(finalB,20+i*4096,data,0, 4096);
					if(!SPPWrite(data,data.length))
					{
						dialog.dismiss();
						showBuilder("????????????",0,"");
						return;
					}
					datalen-=4096;
					try {
						// ??????????????????
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
						dialog.dismiss();
						showBuilder("????????????",0,"");
						return;
					}
					// ???????????????
					dialog.setProgress(i);
				}
				if(datalen != 0){
					System.arraycopy(finalB,20+times*4096,data,0, datalen);
					if (!SPPWrite(data,datalen))
					{
						dialog.dismiss();
						showBuilder("????????????",0,"");
						return;
					}

				}
				dialog.setProgress(100);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
					dialog.dismiss();
					showBuilder("????????????",0,"");
					return;
				}
				//SPPWrite(b,b.length);
				try {
					myInStream1.close();
				} catch (IOException e) {
					e.printStackTrace();
					dialog.dismiss();
					showBuilder("????????????",0,"");
					return;
				}
				SPPClose();
				// ????????????????????????????????????
				dialog.dismiss();
				showBuilder("???????????????????????????????????????????????????",0,"");
			}
		}.start();

	}

	void dismiss()
	{
		dialog.dismiss();
	}
	void showBuilder(String str, int  a, final String gujian) {
		if (Looper.myLooper() == null)
			Looper.prepare();
		AlertDialog.Builder builder = new AlertDialog.Builder(
				MainActivity.this);
		builder.setTitle(str);
		if (a==0) {
			builder.setTitle(str);
			builder.setNegativeButton("?????????????????????????????????", null);

		} else if(a==1) {
			builder.setPositiveButton("?????????????????????????????????",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
											int which) {
							try {
								upData(myBluetoothAdapter, device,UpdataPath);
							} catch (Exception e) {
								e.printStackTrace();
							}
							dialog.dismiss();
						}
					});
		}else if(a==2){
			builder.setPositiveButton("?????????????????????????????????",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
											int which) {
							try {
								download(gujian);
							} catch (Exception e) {
								e.printStackTrace();
							}
							dialog.dismiss();
						}
					});
		}else if(a==3){
			builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
									int which) {
					dismiss();
				}
			});
			builder.setPositiveButton("?????????????????????????????????",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
											int which) {
							try {
								download(gujian);
							} catch (Exception e) {
								e.printStackTrace();
							}
							dialog.dismiss();
						}
					});
		}
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




}
