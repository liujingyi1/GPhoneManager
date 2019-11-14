package com.android.gphonemanager.netmanager;

import com.android.gphonemanager.netmanager.NetControlerActivity.TabPagerAdapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
//import android.graphics.Outline;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
//import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Lightweight implementation of ViewPager tabs. This looks similar to traditional actionBar tabs,
 * but allows for the view containing the tabs to be placed anywhere on screen. Text-related
 * attributes can also be assigned in XML - these will get propogated to the child TextViews
 * automatically.
 */
public class ViewPagerTabs extends HorizontalScrollView implements ViewPager.OnPageChangeListener {

    ViewPager mPager;
    private ViewPagerTabStrip mTabStrip;

    /**
     * Linearlayout that will contain the TextViews serving as tabs. This is the only child
     * of the parent HorizontalScrollView.
     */
    final int mTextStyle;
    final ColorStateList mTextColor;
    final int mTextSize;
    final boolean mTextAllCaps;
    int mPrevSelected = -1;
    int mSidePadding;

    private LayoutInflater mInflater;

    //add JWYYL-248 songqingming 20141218 (start)
    private int mTabTitleSize = 0;

    public void setTabTitleSize(int tabTitleSize) {
        mTabTitleSize = tabTitleSize;
    }
    //add JWYYL-248 songqingming 20141218 (end)

    /*private static final ViewOutlineProvider VIEW_BOUNDS_OUTLINE_PROVIDER =
            new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };*/

    private static final int TAB_SIDE_PADDING_IN_DPS = 10;

    // TODO: This should use <declare-styleable> in the future
    private static final int[] ATTRS = new int[] {
        android.R.attr.textSize,
        android.R.attr.textStyle,
        android.R.attr.textColor,
        android.R.attr.textAllCaps
    };

    /**
     * Simulates actionbar tab behavior by showing a toast with the tab title when long clicked.
     */
    private class OnTabLongClickListener implements OnLongClickListener {
        final int mPosition;

        public OnTabLongClickListener(int position) {
            mPosition = position;
        }

        @Override
        public boolean onLongClick(View v) {
            final int[] screenPos = new int[2];
            getLocationOnScreen(screenPos);

            final Context context = getContext();
            final int width = getWidth();
            final int height = getHeight();
            final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

            Toast toast = Toast.makeText(context, mPager.getAdapter().getPageTitle(mPosition),
                    Toast.LENGTH_SHORT);

            // Show the toast under the tab
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    (screenPos[0] + width / 2) - screenWidth / 2, screenPos[1] + height);

            toast.show();
            return true;
        }
    }

    public ViewPagerTabs(Context context) {
        this(context, null);
    }

    public ViewPagerTabs(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPagerTabs(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFillViewport(true);

        mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSidePadding = (int) (getResources().getDisplayMetrics().density * TAB_SIDE_PADDING_IN_DPS);

        final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        mTextSize = a.getDimensionPixelSize(0, 0);
        mTextStyle = a.getInt(1, 0);
        mTextColor = a.getColorStateList(2);
        mTextAllCaps = a.getBoolean(3, false);

        mTabStrip = new ViewPagerTabStrip(context);
        addView(mTabStrip,
                new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        a.recycle();

        // enable shadow casting from view bounds
        //setOutlineProvider(VIEW_BOUNDS_OUTLINE_PROVIDER);
    }

    public void setViewPager(ViewPager viewPager) {
        mPager = viewPager;
        addTabs(mPager.getAdapter());
    }

    private void addTabs(PagerAdapter adapter) {
        mTabStrip.removeAllViews();

        TabPagerAdapter temp = (TabPagerAdapter)adapter;
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            addTab(adapter.getPageTitle(i), i);
            //addTab(adapter.getPageTitle(i), temp.getPageIconResId(i), i);
        }
    }

    private void addTab(CharSequence tabTitle, int tabImgResId, final int position) {
//    	TabItemView itemView = (TabItemView)mInflater.inflate(R.layout.tab_item_view, null);
//        itemView.setItemContent(tabTitle, tabImgResId);
//    	
//    	itemView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mPager.setCurrentItem(position/*getRtlPosition(position)*/);
//            }
//        });
//
//        itemView.setOnLongClickListener(new OnTabLongClickListener(position));
//
//        mTabStrip.addView(itemView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//                LayoutParams.MATCH_PARENT, 1));
//        // Default to the first child being selected
//        if (position == 0) {
//            mPrevSelected = 0;
//            itemView.setSelected(true);
//        }
    }

    private void addTab(CharSequence tabTitle, final int position) {
        final TextView textView = new TextView(getContext());
        textView.setText(tabTitle);
        //textView.setBackgroundResource(R.drawable.view_pager_tab_background);
//        textView.setBackgroundColor(0x3FFFFFFF);
        textView.setGravity(Gravity.CENTER);
        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(position/*getRtlPosition(position)*/);
            }
        });

        textView.setOnLongClickListener(new OnTabLongClickListener(position));

        // Assign various text appearance related attributes to child views.
        if (mTextStyle > 0) {
            textView.setTypeface(textView.getTypeface(), mTextStyle);
        }
        if (mTextSize > 0) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        }

        //add JWYYL-248 songqingming 20141218 (start)
        if (mTabTitleSize > 0) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTabTitleSize);
        }
        //add JWYYL-248 songqingming 20141218 (end)

        if (mTextColor != null) {
            textView.setTextColor(mTextColor);
        }
        textView.setAllCaps(mTextAllCaps);
        textView.setPadding(mSidePadding, 0, mSidePadding, 0);
        mTabStrip.addView(textView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT, 1));
        // Default to the first child being selected
        if (position == 0) {
            mPrevSelected = 0;
            textView.setSelected(true);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //position = getRtlPosition(position);
        int tabStripChildCount = mTabStrip.getChildCount();
        if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
            return;
        }

        mTabStrip.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        //position = getRtlPosition(position);
        if (mPrevSelected >= 0) {
             mTabStrip.getChildAt(mPrevSelected).setSelected(false);
            //((TabItemView)mTabStrip.getChildAt(mPrevSelected)).setItemSelected(false);
        }
        
        final View selectedChild = mTabStrip.getChildAt(position);
        selectedChild.setSelected(true);
        
        /*final TabItemView selectedChild = (TabItemView)mTabStrip.getChildAt(position);
        selectedChild.setItemSelected(true);*/

        // Update scroll position
        final int scrollPos = selectedChild.getLeft() - (getWidth() - selectedChild.getWidth()) / 2;
        smoothScrollTo(scrollPos, 0);
        mPrevSelected = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    /*private int getRtlPosition(int position) {
        if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            return mTabStrip.getChildCount() - 1 - position;
        }
        return position;
    }*/
}

