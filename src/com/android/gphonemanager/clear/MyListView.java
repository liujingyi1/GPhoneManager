package com.android.gphonemanager.clear;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.android.gphonemanager.R;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MyListView extends ListView implements OnScrollListener,
		OnClickListener {

	public MyListView(Context context) {
		super(context, null);
		initDragListView(context);
	}

	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initDragListView(context);
	}

	private enum DListViewState {
		LV_NORMAL, LV_PULL_REFRESH, LV_RELEASE_REFRESH, LV_LOADING;
	}

	private enum DListViewLoadingMore {
		LV_NORMAL, LV_LOADING, LV_OVER;
	}

	private View mHeadView;
	private TextView mRefreshTextview;
	private TextView mLastUpdateTextView;
	private ImageView mArrowImageView;
	private ProgressBar mHeadProgressBar;

	int mHeadViewWidth;
	private int mHeadViewHeight;

	private Animation animation, reverseAnimation;

	private int mFirstItemIndex = -1;

	private boolean mIsRecord = false;

	private int mStartY, mMoveY;

	private DListViewState mlistViewState = DListViewState.LV_NORMAL;

	private DListViewLoadingMore loadingMoreState = DListViewLoadingMore.LV_NORMAL;

	private final static int RATIO = 2;

	private boolean mBack = false;

	private OnRefreshLoadingMoreListener onRefreshLoadingMoreListener;

	private boolean isScroller = true;

	public void setOnRefreshListener(
			OnRefreshLoadingMoreListener onRefreshLoadingMoreListener) {
		this.onRefreshLoadingMoreListener = onRefreshLoadingMoreListener;
	}

	public void initDragListView(Context context) {

		String time = getCurrentTime();

		initHeadView(context, time);

		setOnScrollListener(this);
	}

	public String getCurrentTime(String format) {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
		String currentTime = sdf.format(date);
		return currentTime;
	}

	public String getCurrentTime() {
		return getCurrentTime("");
	}

	public void initHeadView(Context context, String time) {
		mHeadView = LayoutInflater.from(context).inflate(R.layout.head, null);
		mArrowImageView = (ImageView) mHeadView
				.findViewById(R.id.head_arrowImageView);
		mArrowImageView.setMinimumWidth(60);

		mHeadProgressBar = (ProgressBar) mHeadView
				.findViewById(R.id.head_progressBar);

		mRefreshTextview = (TextView) mHeadView
				.findViewById(R.id.head_tipsTextView);

		mLastUpdateTextView = (TextView) mHeadView
				.findViewById(R.id.head_lastUpdatedTextView);

		mLastUpdateTextView.setText(getResources().getString(
				R.string.recent_updata)
				+ time);

		measureView(mHeadView);

		mHeadViewWidth = mHeadView.getMeasuredWidth();
		mHeadViewHeight = mHeadView.getMeasuredHeight();

		addHeaderView(mHeadView, null, false);

		mHeadView.setPadding(0, -1 * mHeadViewHeight, 0, 0);

		initAnimation();
	}

	private void initAnimation() {

		animation = new RotateAnimation(0, -180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		animation.setInterpolator(new LinearInterpolator());
		animation.setDuration(250);
		animation.setFillAfter(true);

		reverseAnimation = new RotateAnimation(-180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		reverseAnimation.setInterpolator(new LinearInterpolator());
		reverseAnimation.setDuration(250);
		reverseAnimation.setFillAfter(true);
	}

	@SuppressWarnings("deprecation")
	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
					MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0,
					MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {

		case MotionEvent.ACTION_DOWN:
			doActionDown(ev);
			break;

		case MotionEvent.ACTION_MOVE:
			doActionMove(ev);
			break;

		case MotionEvent.ACTION_UP:
			doActionUp(ev);
			break;
		default:
			break;
		}

		if (isScroller) {
			return super.onTouchEvent(ev);
		} else {
			return true;
		}

	}

	void doActionDown(MotionEvent event) {
		if (mIsRecord == false && mFirstItemIndex == 0) {
			mStartY = (int) event.getY();
			mIsRecord = true;
		}
	}

	void doActionMove(MotionEvent event) {
		mMoveY = (int) event.getY();

		if (mIsRecord == false && mFirstItemIndex == 0) {
			mStartY = (int) event.getY();
			mIsRecord = true;
		}

		if (mIsRecord == false || mlistViewState == DListViewState.LV_LOADING) {
			return;
		}

		int offset = (mMoveY - mStartY) / RATIO;

		switch (mlistViewState) {

		case LV_NORMAL: {

			if (offset > 0) {

				mHeadView.setPadding(0, offset - mHeadViewHeight, 0, 0);
				switchViewState(DListViewState.LV_PULL_REFRESH);
			}

		}
			break;

		case LV_PULL_REFRESH: {
			setSelection(0);

			mHeadView.setPadding(0, offset - mHeadViewHeight, 0, 0);
			if (offset < 0) {

				isScroller = false;
				switchViewState(DListViewState.LV_NORMAL);
				Log.e("jj", "isScroller=" + isScroller);
			} else if (offset > mHeadViewHeight) {
				switchViewState(DListViewState.LV_RELEASE_REFRESH);
			}
		}
			break;

		case LV_RELEASE_REFRESH: {
			setSelection(0);

			mHeadView.setPadding(0, offset - mHeadViewHeight, 0, 0);

			if (offset >= 0 && offset <= mHeadViewHeight) {
				mBack = true;
				switchViewState(DListViewState.LV_PULL_REFRESH);
			} else if (offset < 0) {
				switchViewState(DListViewState.LV_NORMAL);
			} else {

			}
		}
			break;
		default:
			return;
		}
		;
	}

	public void doActionUp(MotionEvent event) {
		mIsRecord = false;
		isScroller = true;
		mBack = false;

		if (mlistViewState == DListViewState.LV_LOADING) {
			return;
		}

		switch (mlistViewState) {

		case LV_NORMAL:

			break;

		case LV_PULL_REFRESH:
			mHeadView.setPadding(0, -1 * mHeadViewHeight, 0, 0);
			switchViewState(DListViewState.LV_NORMAL);
			break;

		case LV_RELEASE_REFRESH:
			mHeadView.setPadding(0, 0, 0, 0);
			switchViewState(DListViewState.LV_LOADING);
			onRefresh();
			break;
		default:
			break;
		}

	}

	private void switchViewState(DListViewState state) {

		switch (state) {

		case LV_NORMAL: {
			mArrowImageView.clearAnimation();
			mArrowImageView.setImageResource(R.drawable.clear_download_gif3);
		}
			break;

		case LV_PULL_REFRESH: {
			mHeadProgressBar.setVisibility(View.GONE);
			mArrowImageView.setVisibility(View.VISIBLE);
			mRefreshTextview.setText(getResources().getString(
					R.string.push_refresh));
			mArrowImageView.clearAnimation();

			if (mBack) {
				mBack = false;
				mArrowImageView.clearAnimation();
				mArrowImageView.startAnimation(reverseAnimation);
			}
		}
			break;

		case LV_RELEASE_REFRESH: {
			mHeadProgressBar.setVisibility(View.GONE);
			mArrowImageView.setVisibility(View.VISIBLE);
			mRefreshTextview.setText(getResources()
					.getString(R.string.more_get));
			mArrowImageView.clearAnimation();
			mArrowImageView.startAnimation(animation);
		}
			break;

		case LV_LOADING: {
			Log.e("!!!!!!!!!!!", "convert to IListViewState.LVS_LOADING");
			mHeadProgressBar.setVisibility(View.VISIBLE);
			mArrowImageView.clearAnimation();
			mArrowImageView.setVisibility(View.GONE);
			mRefreshTextview.setText(getResources().getString(
					R.string.refresh_ing));
		}
			break;
		default:
			return;
		}

		mlistViewState = state;

	}

	private void onRefresh() {
		if (onRefreshLoadingMoreListener != null) {
			onRefreshLoadingMoreListener.onRefresh();
		}
	}

	public void onRefreshComplete() {
		mHeadView.setPadding(0, -1 * mHeadViewHeight, 0, 0);
		switchViewState(DListViewState.LV_NORMAL);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {

	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		mFirstItemIndex = firstVisibleItem;
	}

	public interface OnRefreshLoadingMoreListener {

		void onRefresh();

	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub

	}

}
