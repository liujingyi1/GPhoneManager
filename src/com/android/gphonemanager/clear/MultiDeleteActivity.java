package com.android.gphonemanager.clear;

import com.android.gphonemanager.BaseActivity;
import com.android.gphonemanager.R;
import com.android.gphonemanager.clear.SelectBaseFragment.FragmentListener;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.format.Formatter;
import fr.castorflex.android.circularprogressbar.CircularProgressBar;

public class MultiDeleteActivity extends BaseActivity implements OnClickListener{
	private static final String TAG = "MultiDeleteActivity";
	
	int mType;
	SelectBaseFragment mListFragment;
	ScanType mData;
	Context mContext;
	CleanerService mCleanerService;
	TextView clearButton;
	View mProgressBar;
	
	private static int DELETE_RESULT = 9;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.multi_delete_activity);
		
		Intent intent = getIntent();
		mType = intent.getFlags();
		
		TypedArray titleArray = getResources().obtainTypedArray(R.array.clean_titles);
		setTitle(getString(titleArray.getResourceId(mType, 0)));
		
		mContext = getApplicationContext();
		
		initView();
		
		bindService(new Intent(mContext, CleanerService.class), 
				mServiceConnection, Context.BIND_AUTO_CREATE);
		
		
		
//		configureListFragment();
//		mData = (ScanType.UninstallApp)intent.getSerializableExtra("data");
	}
	
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i("jingyi", "MultiDeleteActivity onServiceDisconnected");
			mCleanerService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i("jingyi", "MultiDeleteActivity onServiceConnected");
			mCleanerService = ((CleanerService.CleanerServiceBinder)service).getService();
			mData = mCleanerService.getData();
			
			configureListFragment();
		}
	};
	
	public void configureListFragment() {
		
		Log.i("jingyi", "configureListFragment...");
		
		switch (mType) {
		case ScanType.ACTION_CACHE:
			mListFragment = new DeleteCacheFragment();
			break;
		case ScanType.ACTION_APPUNINSTALL:
			mListFragment = new APPUninstallFragment();
			break;
		case ScanType.ACTION_BIGFILE:
			mListFragment = new BigFileFragment();
			break;
		case ScanType.ACTION_PHOTO:
			mListFragment = new PhotoFragment();
			break;
		case ScanType.ACTION_MEDIA:
			mListFragment = new VideoFragment();
			break;

		default:
			break;
		}
		
		mListFragment.setData(mData);
		mListFragment.setFileIconHelper(new FileIconHelper(this));
		mListFragment.setListener(mFragmentListener);
		
        getFragmentManager().beginTransaction()
        .replace(R.id.fragment_content, mListFragment)
        .commitAllowingStateLoss();
	}
	
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.delete_button:
				popupDialog();
				
				//mListFragment.onClearAction();
				break;	
				
			case R.id.select_all: {
				boolean select = (boolean)v.getTag();
				mListFragment.selectAll(!select);
				break;	
			}
			
			case R.id.settings: {
				mListFragment.onSetting();
				break;	
			}
			
			default:
		}
		
	}
	
	private void popupDialog() {
		View popupView = getLayoutInflater().inflate(R.layout.popup_window, null);
		
		View cleanView = popupView.findViewById(R.id.clean);
		View cancelView = popupView.findViewById(R.id.cancle);
		
		View rootView = getRootView(this);
		
		int height = getResources().getDimensionPixelSize(R.dimen.popup_window_height);
		
		PopupWindow window = new PopupWindow(popupView, rootView.getWidth(), height);
		window.setFocusable(true);
		window.setOutsideTouchable(true);
		window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
		window.setAnimationStyle(R.style.popup_anim);
		window.update();
		window.showAtLocation(rootView, Gravity.BOTTOM, 0, -height);
		WindowManager.LayoutParams lParams = getWindow().getAttributes();
		lParams.alpha = 0.5f;
		getWindow().setAttributes(lParams);
		
		window.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss() {
				WindowManager.LayoutParams lParams = getWindow().getAttributes();
				lParams.alpha = 1f;
				getWindow().setAttributes(lParams);
			}
		});
		
		
		cleanView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mListFragment.onClearAction();
				window.dismiss();
			}
		});
		cancelView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				window.dismiss();
				
			}
		});
	}
	
    private View getRootView(Activity context)  
    {  
        return ((ViewGroup)context.findViewById(android.R.id.content)).getChildAt(0);  
    }  
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.setFlags(mType);
		setResult(DELETE_RESULT, intent);
		super.onBackPressed();
	}
	
	private void initView() {
		clearButton = (TextView)findViewById(R.id.delete_button);
		clearButton.setOnClickListener(this);
		mProgressBar = findViewById(R.id.progressBar);
		ActionBar actionBar = getActionBar();
		if (actionBar != null ) {
			View selectView = actionBar.getCustomView().findViewById(R.id.select_all);
			selectView.setOnClickListener(this);
			View setting = actionBar.getCustomView().findViewById(R.id.settings);
			setting.setOnClickListener(this);
		}
	}

    private void showCircularProgressBar(boolean show) {
        if (show) {
        	clearButton.setEnabled(false);
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.startAnimation(AnimationUtils.loadAnimation(
                    mContext, android.R.anim.fade_out));
            mProgressBar.setVisibility(View.GONE);
            clearButton.setEnabled(true);
        }
    }
	
    FragmentListener mFragmentListener = new FragmentListener() {
		
		@Override
		public void showProgressBar(boolean show) {
			showCircularProgressBar(show);
		}

		@Override
		public void onClearFinished(boolean success, long size) {
			showProgressBar(false);
			
			String sizeStr = Formatter.formatFileSize(mContext, size);
			String info = getString(R.string.clean_up_storage_size, sizeStr);
			
			Toast.makeText(mContext, info, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void setCleanButton(String info, boolean isEnable) {
			clearButton.setText(info);
			clearButton.setEnabled(isEnable);
		}
	};
    
}
