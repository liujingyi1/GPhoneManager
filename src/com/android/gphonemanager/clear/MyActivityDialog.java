package com.android.gphonemanager.clear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.android.gphonemanager.R;
public class MyActivityDialog extends Activity {
	TextView text_name,text_size,text_path;
	String name,size,path;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setFinishOnTouchOutside(true);
		setContentView(R.layout.dialog_demo);
		Intent i=getIntent();
		name= i.getStringExtra("name");
		size= i.getStringExtra("size");
		path= i.getStringExtra("path");
		text_name = (TextView) findViewById(R.id.text_name);
		text_size = (TextView) findViewById(R.id.size);
		text_path = (TextView) findViewById(R.id.path);
		text_name.setText(name);
		text_size.setText(size);
		text_path.setText(path);
	}
	
	
}
