package com.bluetooth;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class SetService extends Activity {
	private TextView title;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_set);
		title = (TextView)findViewById(R.id.title);
		title.setText("设置选项");
		
	}
}
