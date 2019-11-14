package com.android.gphonemanager.adapter;

import java.util.List;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

public class ViewPagerGridviewAdapter extends PagerAdapter {
	
	private List<View> mLists;
	public ViewPagerGridviewAdapter(List<View> data) {
		mLists = data;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mLists.size();
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		// TODO Auto-generated method stub
		return arg0 == (arg1);
	}
	
	@Override
	public Object instantiateItem(View arg0, int arg1) {
		try {
			ViewGroup parent = (ViewGroup) mLists.get(arg1).getParent();
			if (parent != null) {
				parent.removeAllViews();
			}
			
			//container.addView(v);
			((ViewPager) arg0).addView(mLists.get(arg1),0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mLists.get(arg1);
	}
	
	@Override
	public void destroyItem(View arg0, int arg1, Object arg2) {

		try {
            ((ViewPager) arg0).removeView(mLists.get(arg1));
		} catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
		}
	}

}
