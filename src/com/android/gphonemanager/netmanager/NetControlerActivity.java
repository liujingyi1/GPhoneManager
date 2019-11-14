package com.android.gphonemanager.netmanager;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.android.gphonemanager.BaseActivity;
import com.android.gphonemanager.R;
import com.android.gphonemanager.netmanager.ViewPagerTabs;

public class NetControlerActivity extends BaseActivity {
	//private Button mSetingsBtn = null;
	public static final String TO_WHERE = "toWhere";
	public static final String TO_STATS = "toMobile";
	public static final String TO_FIREWALL = "toWifi";
	public static final String TO_SETTINGS = "toBackground";
	
    private MobileFragment mMobileFragment;
    private WifiFragment mWifiFragment;
    private BackgroundFragment mBackgroundFragment;
	
    /** ViewPager for swipe */
    private ViewPager mTabPager;
    private ViewPagerTabs mViewPagerTabs;
    private TabPagerAdapter mTabPagerAdapter;
    private String[] mTabTitles;
    private int[] mTabIcons;
    
    private final TabPagerListener mTabPagerListener = new TabPagerListener();
    
    private int mRequest;
    private boolean mFragmentInitialized;
    private boolean mIsRecreatedInstance;
    
    public interface TabState {
        public static int MOBILE = 0;
        public static int WIFI = 1;
        public static int BACKGROUND = 2;

        public static int COUNT = 3;
        public static int DEFAULT = MOBILE;
    }
    
    private int mCurrentTab = TabState.DEFAULT;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	mIsRecreatedInstance = (savedInstanceState != null);
    	setTitle(getString(R.string.net_manager));
    	
    	createViewsAndFragments(savedInstanceState);
    }
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    }
	
    @Override
    protected void onStart() {
    	super.onStart();
    }
    
    @Override
    protected void onStop() {
    	// TODO Auto-generated method stub
    	super.onStop();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	// TODO Auto-generated method stub
    	super.onNewIntent(intent);
    }
    
    private void createViewsAndFragments(Bundle savedState) {
    	setContentView(R.layout.net_controler_activity);
    	
    	mTabTitles = new String[TabState.COUNT];
        mTabTitles[TabState.MOBILE] = getString(R.string.mobile_tab_label);
        mTabTitles[TabState.WIFI] = getString(R.string.wifi_tab_label);
        mTabTitles[TabState.BACKGROUND] = getString(R.string.background_tab_label);
//        mTabIcons = new int[TabState.COUNT];
//        mTabIcons[TabState.STATS] = R.drawable.ic_tab_stats;
//        mTabIcons[TabState.FIREWALL] = R.drawable.ic_tab_firewall;
//        mTabIcons[TabState.SETTINGS] = R.drawable.ic_tab_settings;
        
        mTabPager = (ViewPager)findViewById(R.id.tab_pager);
        mTabPagerAdapter = new TabPagerAdapter();
        mTabPager.setAdapter(mTabPagerAdapter);
        mTabPager.setOnPageChangeListener(mTabPagerListener);
        
        mViewPagerTabs = (ViewPagerTabs) findViewById(R.id.tab_pager_header);
        mViewPagerTabs.setViewPager(mTabPager);
        
        final FragmentManager fragmentManager = getFragmentManager();

        // Hide all tabs (the current tab will later be reshown once a tab is selected)
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        final String MOBILE_TAG = "tab-pager-mobile";
        final String WIFI_TAG = "tab-pager-wifi";
        final String BACKGROUND_TAG = "tab-pager-background";
        
        mMobileFragment = (MobileFragment)
                fragmentManager.findFragmentByTag(MOBILE_TAG);
        mWifiFragment = (WifiFragment)
                fragmentManager.findFragmentByTag(WIFI_TAG);
        mBackgroundFragment = (BackgroundFragment)
                fragmentManager.findFragmentByTag(BACKGROUND_TAG);

        if (mMobileFragment == null) {
        	mMobileFragment = new MobileFragment();
        	mWifiFragment = new WifiFragment();
        	mBackgroundFragment = new BackgroundFragment();

            transaction.add(R.id.tab_pager, mMobileFragment, MOBILE_TAG);
            transaction.add(R.id.tab_pager, mWifiFragment, WIFI_TAG);
            transaction.add(R.id.tab_pager, mBackgroundFragment, BACKGROUND_TAG);
        }
    	
        
        // Hide all fragments for now.  We adjust visibility when we get onSelectedTabChanged()
        // from ActionBarAdapter.
        transaction.hide(mMobileFragment);
        transaction.hide(mWifiFragment);
        transaction.hide(mBackgroundFragment);

        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
        
        mTabPager.setCurrentItem(mCurrentTab);
    }
    
	private class TabPagerListener implements ViewPager.OnPageChangeListener {

        // This package-protected constructor is here because of a possible compiler bug.
        // PeopleActivity$1.class should be generated due to the private outer/inner class access
        // needed here.  But for some reason, PeopleActivity$1.class is missing.
        // Since $1 class is needed as a jvm work around to get access to the inner class,
        // changing the constructor to package-protected or public will solve the problem.
        // To verify whether $1 class is needed, javap PeopleActivity$TabPagerListener and look for
        // references to PeopleActivity$1.
        //
        // When the constructor is private and PeopleActivity$1.class is missing, proguard will
        // correctly catch this and throw warnings and error out the build on user/userdebug builds.
        //
        // All private inner classes below also need this fix.
        TabPagerListener() {}

        @Override
        public void onPageScrollStateChanged(int state) {
                mViewPagerTabs.onPageScrollStateChanged(state);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mViewPagerTabs.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
                mViewPagerTabs.onPageSelected(position);
                invalidateOptionsMenu();
        }
    }

    
    public class TabPagerAdapter extends PagerAdapter {
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;

        //private boolean mTabPagerAdapterSearchMode;

        private Fragment mCurrentPrimaryItem;

        public TabPagerAdapter() {
            mFragmentManager = getFragmentManager();
        }

        /*public boolean isSearchMode() {
            return mTabPagerAdapterSearchMode;
        }*/

        @Override
        public int getCount() {
            return TabState.COUNT;
        }

        /** Gets called when the number of items changes. */
        @Override
        public int getItemPosition(Object object) {
            if (object == mMobileFragment) {
                return 0; // Only 1 page in search mode
            } else if (object == mWifiFragment) {
                return 1;
            }else if (object == mBackgroundFragment) {
                return 2;
            }
            return POSITION_NONE;
        }

        @Override
        public void startUpdate(ViewGroup container) {
        }

        private Fragment getFragment(int position) {
            if (position == TabState.MOBILE) {
                return mMobileFragment;
            } else if (position == TabState.WIFI) {
                return mWifiFragment;
            } else if (position == TabState.BACKGROUND) {
            	return mBackgroundFragment;
            }
            throw new IllegalArgumentException("position: " + position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            
            Fragment f = getFragment(position);
            mCurTransaction.show(f);

            // Non primary pages are not visible.
            f.setUserVisibleHint(f == mCurrentPrimaryItem);
            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.hide((Fragment) object);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            Fragment fragment = (Fragment) object;
            if (mCurrentPrimaryItem != fragment) {
                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setUserVisibleHint(false);
                }
                if (fragment != null) {
                    fragment.setUserVisibleHint(true);
                }
                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabTitles[position];
        }

        @SuppressWarnings("unused")
	public int getPageIconResId(int position) {
            return mTabIcons[position];
        }
    }
}
