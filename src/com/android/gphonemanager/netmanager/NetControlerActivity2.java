package com.android.gphonemanager.netmanager;

import com.android.gphonemanager.BaseActivity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.android.gphonemanager.R;

public class NetControlerActivity2 extends BaseActivity implements OnClickListener {
	
	private static final String TAG = "NetControlerActivity";
	
	private static final String KEY_IN_FIREWALL_SCREEN = "mInNormalScreen";
	private static final String TAG_FIREWALL_FRAGMENT = "FireWallFragment";
	private static final String TAG_BACKGROUND_FRAGMENT = "BackgroundFragment";

	public static final int LOADER_ID_FREEZED_APP = 1;
	public static final int LOADER_ID_NORMAL_APP = 2;
	
	FireWallFragment mFirewallFragment;
	BackgroundFragment mBackgroundFragment;
	
	private boolean mInFreewallScreen = true;
	
	Context mContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.net_controler_activity2);
		
		mContext = getApplicationContext();
		
		ActionBar actionBar = getActionBar();
		if (actionBar != null ) {
			Spinner spinner = (Spinner)actionBar.getCustomView().findViewById(R.id.spinner1);
			spinner.setVisibility(View.VISIBLE);
			String[] items = getResources().getStringArray(R.array.net_control_spinner_list);
			ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, 
					R.layout.spinner_text, items);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
			spinner.setOnItemSelectedListener(mSpinnerItemSelect);
			int offset = getResources().getDimensionPixelSize(R.dimen.actionbar_size);
			spinner.setDropDownVerticalOffset(offset);
		}
		
		setTitle(getString(R.string.net_manager));
		
		mFirewallFragment = new FireWallFragment();
		mBackgroundFragment = new BackgroundFragment();
		
		if (savedInstanceState == null) {
			final FragmentTransaction transaction = getFragmentManager()
					.beginTransaction();
			transaction.add(R.id.fragment_content, mFirewallFragment,
					TAG_FIREWALL_FRAGMENT);
			transaction.add(R.id.fragment_content, mBackgroundFragment,
					TAG_BACKGROUND_FRAGMENT);
			
	        transaction.hide(mFirewallFragment);
	        transaction.hide(mBackgroundFragment);
	        transaction.commitAllowingStateLoss();
			mInFreewallScreen = true;
		} else {
			mInFreewallScreen = savedInstanceState
					.getBoolean(KEY_IN_FIREWALL_SCREEN);
		}
	}
	
	OnItemSelectedListener mSpinnerItemSelect = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			if (position == 0) {
				if (!mInFreewallScreen) {
					mInFreewallScreen = true;
					showBackgroundFragment(false);
				}
			} else {
				if (mInFreewallScreen) {
					mInFreewallScreen = false;
					showBackgroundFragment(true);
				}
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub
			
		}
	};
	
	@Override
	public void onAttachFragment(Fragment fragment) {
		if (fragment instanceof FireWallFragment) {
			mFirewallFragment = (FireWallFragment)fragment;
		} else if (fragment instanceof BackgroundFragment) {
			mBackgroundFragment = (BackgroundFragment)fragment;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		
//		getLoaderManager().restartLoader(LOADER_ID_FREEZED_APP,
//				null, freezeAppCallbacks);
//		getLoaderManager().restartLoader(LOADER_ID_NORMAL_APP,
//				null, normalAppCallbacks);
		
		if (mInFreewallScreen) {
			showBackgroundFragment(false);
		} else {
			showBackgroundFragment(true);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_IN_FIREWALL_SCREEN, mInFreewallScreen);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void showBackgroundFragment(boolean show) {
		
		final FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();

		if (show) {
			transaction.hide(mFirewallFragment);
			transaction.show(mBackgroundFragment);
			transaction.commit();
		} else {
			transaction.hide(mBackgroundFragment);
			transaction.show(mFirewallFragment);
			transaction.commit();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.settings: {
				mInFreewallScreen = !mInFreewallScreen;
				 showBackgroundFragment(!mInFreewallScreen);
				
				break;	
		}
		
		default:
	}
	}
	
}
