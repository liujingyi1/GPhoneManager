package com.android.gphonemanager.freezeapp;

import java.util.ArrayList;
import java.util.List;

import com.android.gphonemanager.BaseActivity;
import com.android.gphonemanager.utils.FastBlur;
import com.android.gphonemanager.view.IceAnimationLayout;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gphonemanager.R;

public class FreezeActivity extends BaseActivity implements IFreezeView {
	private static final String TAG = "Gmanager.FPFreezeActivity";
	
	private static final String KEY_IN_NORMAL_SCREEN = "mInNormalScreen";
	private static final String TAG_FREEZE_FRAGMENT = "FreezeAppFragment";
	private static final String TAG_NORMAL_FRAGMENT = "NormalAppFragment";

	public static final int LOADER_ID_FREEZED_APP = 1;
	public static final int LOADER_ID_NORMAL_APP = 2;

	private FreezeAppFragment mFreezeFragment;
	private FreezeNormalAppFragment mNormalFragment;

	private FreezePresenter presenter;
	private IFreezeModel model;

	private boolean mInNormalScreen = false;

	private IceAnimationLayout mAnimBgView;
	private ImageView mStaticImgview;
	private TextView mLabelView;

	private Drawable mBlurBgDrawable;

	private ConfirmDialogFragment confirmDialog;

	private static final int[] iceBrokenDrawables = {
			R.drawable.ice_broken_0000, R.drawable.ice_broken_0001,
			R.drawable.ice_broken_0002, R.drawable.ice_broken_0003,
			R.drawable.ice_broken_0004, R.drawable.ice_broken_0005,
			R.drawable.ice_broken_0006, R.drawable.ice_broken_0007,
			R.drawable.ice_broken_0008, R.drawable.ice_broken_0009,
			R.drawable.ice_broken_0010, R.drawable.ice_broken_0011,
			R.drawable.ice_broken_0012, R.drawable.ice_broken_0013,
			R.drawable.ice_broken_0014, R.drawable.ice_broken_0015,
			R.drawable.ice_broken_0016, R.drawable.ice_broken_0017,
			R.drawable.ice_broken_0018, R.drawable.ice_broken_0019,
			R.drawable.ice_broken_0020, R.drawable.ice_broken_0021,
			R.drawable.ice_broken_0022, R.drawable.ice_broken_0023,
			R.drawable.ice_broken_0024, R.drawable.ice_broken_0025,
			R.drawable.ice_broken_0026, R.drawable.ice_broken_0027, };

	private static final int[] iceFrozeDrawables = {
			R.drawable.ice_frozen_0000, R.drawable.ice_frozen_0001,
			R.drawable.ice_frozen_0002, R.drawable.ice_frozen_0003,
			R.drawable.ice_frozen_0004, R.drawable.ice_frozen_0005,
			R.drawable.ice_frozen_0006, R.drawable.ice_frozen_0007,
			R.drawable.ice_frozen_0008, R.drawable.ice_frozen_0009,
			R.drawable.ice_frozen_0010, R.drawable.ice_frozen_0011,
			R.drawable.ice_frozen_0012, R.drawable.ice_frozen_0013,
			R.drawable.ice_frozen_0014, R.drawable.ice_frozen_0015,
			R.drawable.ice_frozen_0016, R.drawable.ice_frozen_0017,
			R.drawable.ice_frozen_0018, R.drawable.ice_frozen_0019,
			R.drawable.ice_frozen_0020, R.drawable.ice_frozen_0021,
			R.drawable.ice_frozen_0022, R.drawable.ice_frozen_0023,
			R.drawable.ice_frozen_0024, R.drawable.ice_frozen_0025,
			R.drawable.ice_frozen_0026, R.drawable.ice_frozen_0027,
			R.drawable.ice_frozen_0028, R.drawable.ice_frozen_0029,
			R.drawable.ice_frozen_0030, R.drawable.ice_frozen_0031,
			R.drawable.ice_frozen_0032, R.drawable.ice_frozen_0033,
			R.drawable.ice_frozen_0034, R.drawable.ice_frozen_0035,
			R.drawable.ice_frozen_0036, R.drawable.ice_frozen_0037,
			R.drawable.ice_frozen_0038, R.drawable.ice_frozen_0039,
			R.drawable.ice_frozen_0040, };

	private FreezeNormalAppFragment.OnItemClickListener mOnNormalItemClickListener = new FreezeNormalAppFragment.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View itemView,
				FreezeAppInfo info) {
				
				if (confirmDialog != null && confirmDialog.getDialog() != null) {
					return;
				}
				
			getBlurDrawable();
			showConfirmDialog(info);
		}
	};

	private FreezeAppFragment.OnItemClickListener mOnFreezeItemClickListener = new FreezeAppFragment.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View itemView,
				int position, FreezeAppInfo info) {
			if (position == 0) {
				toAddApp();
			} else {
				if (confirmDialog != null && confirmDialog.getDialog() != null) {
					return;
				}
			
				getBlurDrawable();
				showConfirmDialog(info);
			}
		}
	};
	
	private final LoaderCallbacks<List<FreezeAppInfo>> freezeAppCallbacks = new LoaderCallbacks<List<FreezeAppInfo>>() {
		FreezeAppsLoader loader;
		@Override
		public Loader<List<FreezeAppInfo>> onCreateLoader(int id, Bundle args) {
			loader = new FreezeAppsLoader(FreezeActivity.this,
					FreezeAppsLoader.FLAG_FREEZED_APPS, presenter);
			return loader;
		}

		@Override
		public void onLoadFinished(Loader<List<FreezeAppInfo>> loader,
				List<FreezeAppInfo> infos) {
			Log.d(TAG, "freeze-onLoadFinished");
			List<FreezeAppInfo> freezeInfo = new ArrayList<FreezeAppInfo>();
			freezeInfo.addAll(infos);
			mFreezeFragment.changeData(freezeInfo);
		}

		@Override
		public void onLoaderReset(Loader<List<FreezeAppInfo>> loader) {
		}
	};
	
	private final LoaderCallbacks<List<FreezeAppInfo>> normalAppCallbacks = new LoaderCallbacks<List<FreezeAppInfo>>() {
		FreezeAppsLoader loader;
		@Override
		public Loader<List<FreezeAppInfo>> onCreateLoader(int id, Bundle args) {
			loader = new FreezeAppsLoader(FreezeActivity.this,
					FreezeAppsLoader.FLAG_NORMAL_APPS, presenter);
			return loader;
		}

		@Override
		public void onLoadFinished(Loader<List<FreezeAppInfo>> loader,
				List<FreezeAppInfo> infos) {
			Log.d(TAG, "normal-onLoadFinished");
			List<FreezeAppInfo> normalInfo = new ArrayList<FreezeAppInfo>();
			normalInfo.addAll(infos);
			mNormalFragment.changeData(normalInfo);
		}

		@Override
		public void onLoaderReset(Loader<List<FreezeAppInfo>> loader) {
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.freeze_activity);
		
        Log.d(TAG, "onCreate");
		
        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        if (savedInstanceState != null) {
            getLoaderManager().destroyLoader(LOADER_ID_FREEZED_APP);
            getLoaderManager().destroyLoader(LOADER_ID_NORMAL_APP);
        }

		mAnimBgView = (IceAnimationLayout) findViewById(R.id.anim_container);
		mAnimBgView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			}
		});
		mStaticImgview = (ImageView) findViewById(R.id.static_img);
		mLabelView = (TextView) findViewById(R.id.label);

		model = new FreezeModelImpl(this);
		presenter = new FreezePresenter(this, model);
		
		mFreezeFragment = (FreezeAppFragment)fragmentManager.findFragmentByTag(TAG_FREEZE_FRAGMENT);
		mNormalFragment = (FreezeNormalAppFragment)fragmentManager.findFragmentByTag(TAG_NORMAL_FRAGMENT);
		
		if (mFreezeFragment == null) {
			mFreezeFragment = new FreezeAppFragment();
			mNormalFragment = new FreezeNormalAppFragment();
			
			transaction.add(R.id.container, mFreezeFragment,
					TAG_FREEZE_FRAGMENT);
			transaction.add(R.id.container, mNormalFragment,
					TAG_NORMAL_FRAGMENT);
			transaction.commit();
		}
		
		if (savedInstanceState == null) {
			mInNormalScreen = false;
		} else {
			mInNormalScreen = savedInstanceState
					.getBoolean(KEY_IN_NORMAL_SCREEN);
		}
		
		setTitle(getString(R.string.freeze_app));
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		if (fragment instanceof FreezeAppFragment) {
			mFreezeFragment = (FreezeAppFragment) fragment;
			mFreezeFragment.setOnItemClickListener(mOnFreezeItemClickListener);
		} else if (fragment instanceof FreezeNormalAppFragment) {
			mNormalFragment = (FreezeNormalAppFragment) fragment;
			mNormalFragment.setOnItemClickListener(mOnNormalItemClickListener);
		}
	}

	@Override
	public void onBackPressed() {
		if (mInNormalScreen) {
			hideNormalFragment(true);
			mInNormalScreen = false;
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_IN_NORMAL_SCREEN, mInNormalScreen);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		getLoaderManager().initLoader(LOADER_ID_FREEZED_APP, null, freezeAppCallbacks);
		getLoaderManager().initLoader(LOADER_ID_NORMAL_APP, null, normalAppCallbacks);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		//getLoaderManager().restartLoader(LOADER_ID_FREEZED_APP,
		//		null, freezeAppCallbacks);
		//getLoaderManager().restartLoader(LOADER_ID_NORMAL_APP,
		//		null, normalAppCallbacks);
		
		if (mInNormalScreen) {
			showNormalFragment(false);
		} else {
			hideNormalFragment(false);
		}
	}

	private LayerDrawable getStaticDrawable(Drawable appIcon) {
		Drawable[] drawables = new Drawable[2];
		drawables[0] = appIcon;
		drawables[1] = getResources().getDrawable(R.drawable.icon_ice);
		LayerDrawable ld = new LayerDrawable(drawables);
		return ld;
	}

	private void iceBrokenAnimation() {
		mAnimBgView.shakeChildWithAnim(0);
		mAnimBgView.setAnimRes(iceBrokenDrawables);
		mAnimBgView.startAnim();
	}

	private void iceFreezeAnimation() {
		mAnimBgView.setAnimRes(iceFrozeDrawables);
		mAnimBgView.startAnim();
	}

	private void setAnimationBg() {
		mAnimBgView.setVisibility(View.VISIBLE);
		mAnimBgView.setBackground(mBlurBgDrawable);
	}

	private Drawable getBlurDrawable() {
		Log.d(TAG, "getBlurDrawable");
		View v = mInNormalScreen ? mNormalFragment.getView() : mFreezeFragment
				.getView();
		Bitmap bm = createDraggedChildBitmap(v); // Screenshot
		bm = FastBlur.doBlur(scaleBitmap(bm, 180, 260), 15, false); // Gaussian blur
		// Log.d("sqm", "bm====" + bm);
		mBlurBgDrawable = new BitmapDrawable(getResources(), bm);
		return mBlurBgDrawable;
	}

	// scale bitmap
	private Bitmap scaleBitmap(Bitmap bm, int w, int h) {
		Bitmap b = bm;
		int width = b.getWidth();
		int height = b.getHeight();
		// Log.d("sqm", "width=" + width + ", height=" + height);
		float scaleW = w * 1.0f / width;
		float scaleH = h * 1.0f / height;

		Matrix m = new Matrix();
		m.postScale(scaleW, scaleH);
		Bitmap bb = Bitmap.createBitmap(b, 0, 0, width, height, m, true);
		// Log.d("sqm", "bb:width=" + bb.getWidth() + ", height=" +
		// bb.getHeight());
		return bb;
	}

	// screenshot method
	private Bitmap createDraggedChildBitmap(View view) {
		view.setDrawingCacheEnabled(true);
		final Bitmap cache = view.getDrawingCache();

		Bitmap bitmap = null;
		if (cache != null) {
			try {
				bitmap = cache.copy(Bitmap.Config.ARGB_8888, false);
			} catch (final OutOfMemoryError e) {
				Log.w(TAG, "Failed to copy bitmap from Drawing cache", e);
				bitmap = null;
			}
		}

		view.destroyDrawingCache();
		view.setDrawingCacheEnabled(false);

		return bitmap;
	}

	private void showConfirmDialog(FreezeAppInfo info) {
		// if (confirmDialog == null) {
		confirmDialog = new ConfirmDialogFragment(info);
		// }
		confirmDialog.show(getFragmentManager(),
				ConfirmDialogFragment.FRAGMENT_TAG);
	}

	private void toAddApp() {
		showNormalFragment(true);
		mInNormalScreen = true;
	}
	
	@Override
	protected void onHomeSelected() {
		if (mInNormalScreen) {
			hideNormalFragment(true);
			mInNormalScreen = false;
		} else {
			finish();
		}
	}

	@SuppressLint("ValidFragment")
	public class ConfirmDialogFragment extends DialogFragment {
		public static final String FRAGMENT_TAG = "confirm_dialog";
		private FreezeAppInfo mAppInfo;
		
		OnClickListener clickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.dialog_ok) {
					if (mAppInfo.isFreezed) {
						presenter.deleteFreezeApp(mAppInfo.applicationInfo.uid, mAppInfo);
					} else {
						presenter.addFreezeApp(
								mAppInfo.applicationInfo.uid, mAppInfo);
						
					}
					dismiss();
				} else if (v.getId() == R.id.dialog_cancel) {
					dismiss();
				}

			}
		};
		
		public ConfirmDialogFragment(FreezeAppInfo info) {
			mAppInfo = info;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					getActivity(), R.style.DialogTheme);
			final LayoutInflater dialogInflater = LayoutInflater.from(builder
					.getContext());

			final View view = dialogInflater.inflate(
					R.layout.freeze_confirm_dialog_layout, null, false);
			TextView msgView = (TextView) view.findViewById(R.id.dialog_msg);
			Button okBtn = (Button) view.findViewById(R.id.dialog_ok);
			Button cancelBtn = (Button) view.findViewById(R.id.dialog_cancel);
			
			okBtn.setOnClickListener(clickListener);
			cancelBtn.setOnClickListener(clickListener);
			String rawMsg = mAppInfo.isFreezed ? getString(R.string.app_unfreeze_dialog_message)
					: getString(R.string.app_freeze_dialog_message);
			msgView.setText(String.format(rawMsg, mAppInfo.name));
			builder.setView(view);

			return builder.create();
		}
	}
	
	private void hideNormalFragment(boolean animate) {
		if (animate) {
			TranslateAnimation animation = new TranslateAnimation(
					Animation.RELATIVE_TO_PARENT, 0,
					Animation.RELATIVE_TO_PARENT, 1,
					Animation.RELATIVE_TO_PARENT, 0,
					Animation.RELATIVE_TO_PARENT, 0);
			animation.setDuration(400);
			animation.setInterpolator(new AccelerateInterpolator());
			mNormalFragment.getView().startAnimation(animation);
		}
		final FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.hide(mNormalFragment);
		transaction.commit();
	}
	
	
	private void showNormalFragment(boolean animate) {
		final FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		if (animate) {	
			TranslateAnimation animation = new TranslateAnimation(
					Animation.RELATIVE_TO_PARENT, 1,
					Animation.RELATIVE_TO_PARENT, 0,
					Animation.RELATIVE_TO_PARENT, 0,
					Animation.RELATIVE_TO_PARENT, 0);
			animation.setInterpolator(new DecelerateInterpolator());
			animation.setDuration(400);
			mNormalFragment.getView().startAnimation(animation);
		}
		transaction.show(mNormalFragment);
		transaction.commit();
	}

	@Override
	public void addFreezeApp(FreezeAppInfo mAppInfo) {
		setAnimationBg();
		
		mStaticImgview.setImageDrawable(mAppInfo.icon);
		mLabelView.setText(mAppInfo.name);
		
		iceFreezeAnimation();
		
		String toast = String
				.format(getString(R.string.toast_app_freeze_success),
						mAppInfo.name);
		Toast.makeText(FreezeActivity.this, toast,
				Toast.LENGTH_SHORT).show();
		
//		mFreezeFragment.changeData(null);
//		mNormalFragment.changeData(null);
		mFreezeFragment.addItem(mAppInfo);
		mNormalFragment.deleteItem(mAppInfo);
	}

	@Override
	public void deleteFreezeApp(FreezeAppInfo mAppInfo) {
		setAnimationBg();
		
		mStaticImgview.setImageDrawable(mAppInfo.icon);
		mLabelView.setText(mAppInfo.name);
		
		iceBrokenAnimation();
		
		String toast = String
				.format(getString(R.string.toast_app_unfreeze_success),
						mAppInfo.name);
		Toast.makeText(FreezeActivity.this, toast,
				Toast.LENGTH_SHORT).show();
		
//		mFreezeFragment.changeData(null);
//		mNormalFragment.changeData(null);
		mFreezeFragment.deleteItem(mAppInfo);
		mNormalFragment.addItem(mAppInfo);
	}

}
