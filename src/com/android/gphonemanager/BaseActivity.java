package com.android.gphonemanager;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.view.View;

import com.android.gphonemanager.R;

public abstract class BaseActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		if (actionBar != null ) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}
		
		initActionBar();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onHomeSelected();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	protected void onHomeSelected(){
		finish();
	};
	
	private void initActionBar() {
		ActionBar actionBar = getActionBar();
		if (actionBar != null ) {
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
			actionBar.setCustomView(R.layout.actionbar_view);
			actionBar.getCustomView().findViewById(R.id.img_back).setOnClickListener(
					new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							onBackPressed();
//							finish();
						}
					});
			actionBar.setElevation(0);
			
			View selectall = actionBar.getCustomView().findViewById(R.id.select_all);
			selectall.setTag(false);
		}
	}
	
	@Override
	public void setTitle(CharSequence title) {
		ActionBar actionBar = getActionBar();
		if (actionBar != null ) {
			((TextView)actionBar.getCustomView().findViewById(R.id.title)).setText(title);
		}
		super.setTitle(title);
	}
}
