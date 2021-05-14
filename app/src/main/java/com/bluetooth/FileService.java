package com.bluetooth;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class FileService extends Activity {
	ListView listView;
	TextView textView;
	File currentParent;
	File[] currentFiles;
	static String pathString;
	public static String getPathString() {
		return pathString;
	}

	public static void setPathString(String pathString) {
		FileService.pathString = pathString;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_dir);
		listView = (ListView) findViewById(R.id.list);
		textView = (TextView) findViewById(R.id.path);
		File root = new File(Environment.getExternalStorageDirectory().getPath());
		if (root.exists()) {
			currentParent = root;
			currentFiles = root.listFiles();
			inflateListView(currentFiles);
		}
		listView.setOnItemClickListener(new OnItemClickListener() {

			@SuppressLint("ShowToast")
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if (currentFiles[arg2].isFile()){
					Intent intent = new Intent();
					intent.putExtra("path", currentFiles[arg2].toString());
					setResult(12,intent);
					finish();
					return ;
				}
				File[] tmp = currentFiles[arg2].listFiles();
				if (tmp == null || tmp.length == 0) {
					Toast.makeText(FileService.this, "当前路径不可访问或该路径下没有文件", 20000)
							.show();
				} else {
					currentParent = currentFiles[arg2];
					currentFiles = tmp;
					inflateListView(currentFiles);
				}
			}
		});
		Button parent = (Button) findViewById(R.id.parent);
		parent.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				try {
					if (!currentParent.getCanonicalPath().equals("/mnt/sdcard")) {
						currentParent = currentParent.getParentFile();
						currentFiles = currentParent.listFiles();
						inflateListView(currentFiles);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void inflateListView(File[] files) {
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < files.length; i++) {
			Map<String, Object> listItem = new HashMap<String, Object>();
			if (files[i].isDirectory()) {
				listItem.put("icon", R.drawable.ic_folder);
			} else {
				listItem.put("icon", R.drawable.ic_file);
			}
			listItem.put("fileName", files[i].getName());
			listItems.add(listItem);
		}
		SimpleAdapter simpleAdapter = new SimpleAdapter(this, listItems,
				R.layout.layout_line, new String[] { "icon", "fileName" },
				new int[] { R.id.icon, R.id.file_name });
		listView.setAdapter(simpleAdapter);
		try {
			textView.setText("当前路径: " + currentParent.getCanonicalPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
