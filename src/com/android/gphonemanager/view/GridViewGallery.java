package com.android.gphonemanager.view;

import java.util.ArrayList;
import java.util.List;

import com.android.gphonemanager.adapter.ChannelInfoBean;
import com.android.gphonemanager.adapter.ViewPagerGVItemAdapter;
import com.android.gphonemanager.adapter.ViewPagerGridviewAdapter;
import com.android.gphonemanager.utils.Utils;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.support.v4.view.PagerAdapter;
import android.text.style.LineHeightSpan.WithDensity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.AdapterView.OnItemClickListener;
import com.android.gphonemanager.R;

public class GridViewGallery extends LinearLayout {

	private Context mContext;
	
	private List<ChannelInfoBean> mDataList;
	
	private ViewPager viewPager;
	private LinearLayout viewGroupDots;
	private ImageView[] dots;
	
	private int currentIndex;
	private int viewPagerSize;
	private int pageItemCount = 6;
	
	private List<View> listViews;
	
	public GridViewGallery(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		mContext = context;
		mDataList = null;
		initView();
	}
	
    @SuppressWarnings("unchecked")
	public GridViewGallery(Context context, List<?> list) {  
        super(context);  
        this.mContext = context;  
        this.mDataList = (List<ChannelInfoBean>) list;  
        initView();  
        initDots();  
        setAdapter();  
    }  
	
	private void setAdapter() {
		listViews = new ArrayList<View>();
		for (int i = 0; i < viewPagerSize; i++) {
			listViews.add(getViewPagerItem(i));
		}
		viewPager.setAdapter(new ViewPagerGridviewAdapter(listViews));  
	}
	
	private void initView() {
     View view = LayoutInflater.from(mContext).inflate(R.layout.main_gallery_content, null);  
     viewPager = (ViewPager) view.findViewById(R.id.viewpager);  
     viewGroupDots = (LinearLayout) view.findViewById(R.id.dots_group);
     addView(view);  
	}
	
    private void initDots() {
    	  
        int width = Utils.getWindowWidth(mContext);  
        int high = Utils.getWindowHeight(mContext);  
     
//        int col = (width / 200) > 2 ? (width /200) : 3;
//        int row = (high/400) >4 ? (high/400):3;

        //pageItemCount = col * row;  //每一页可装item
        int t=1;
        if(mDataList.size() % pageItemCount==0){
        	t=0;
        }
        viewPagerSize = mDataList.size() / pageItemCount + t;  
  
        if (0 < viewPagerSize) {  
        	viewGroupDots.removeAllViews();  
            if (1 == viewPagerSize) {  
            	viewGroupDots.setVisibility(View.GONE);  
            } else if (1 < viewPagerSize) {  
            	viewGroupDots.setVisibility(View.VISIBLE);  
                for (int j = 0; j < viewPagerSize; j++) {  
                    ImageView image = new ImageView(mContext);  
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(10, 10);  //dot的宽高
                    params.setMargins(3, 0, 3, 0);  
                    image.setBackgroundResource(R.drawable.play_hide);  
                    viewGroupDots.addView(image, params);  
                }  
            }  
        }  
        if (viewPagerSize != 1) {  
            dots = new ImageView[viewPagerSize];  
            for (int i = 0; i < viewPagerSize; i++) {
            	//从布局中填充dots数组
                dots[i] = (ImageView) viewGroupDots.getChildAt(i);  
                //dots[i].setEnabled(true);  
                //dots[i].setTag(i);  
            }  
            currentIndex = 0;  //当前页
            //dots[currentIndex].setEnabled(false);  
            dots[currentIndex].setBackgroundResource(R.drawable.play_display);
            viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {  
  
                @Override  
                public void onPageSelected(int arg0) {  
                    setCurDot(arg0);  
                }  
  
                @Override  
                public void onPageScrolled(int arg0, float arg1, int arg2) {  
                    // TODO Auto-generated method stub  
  
                }  
  
                @Override  
                public void onPageScrollStateChanged(int arg0) {  
                    // TODO Auto-generated method stub  
  
                }  
            });  
        }  
    }  
  
    /** 当前底部小圆点 */  
    private void setCurDot(int positon) {  
        if (positon < 0 || positon > viewPagerSize - 1 || currentIndex == positon) {  
            return;  
        } 
        for(int i=0;i<dots.length;i++){
        	dots[i].setBackgroundResource(R.drawable.play_hide);
        }
        //dots[positon].setEnabled(false);  
       // dots[currentIndex].setEnabled(true);  
        dots[positon].setBackgroundResource(R.drawable.play_display);
        currentIndex = positon;  
    }  
 
	
	private View getViewPagerItem(int index) {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.main_viewpage_gridview, null);
		GridView gridView = (GridView) layout.findViewById(R.id.gridview);
		
		int width = Utils.getWindowWidth(mContext);
//		int col = (width / 200) > 2 ? (width / 200) : 3;
		int col = 3;
		
		gridView.setNumColumns(col);
		
		ViewPagerGVItemAdapter adapter = new ViewPagerGVItemAdapter(mContext, mDataList, index, pageItemCount);
		
		gridView.setAdapter(adapter);
		
		gridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (null != mDataList.get(position + currentIndex * pageItemCount).getOnClickListener())  
					mDataList.get(position + currentIndex * pageItemCount).getOnClickListener().ongvItemClickListener(position + currentIndex * pageItemCount); 
			}  
		});
		
		return gridView;
	}
	
    public int getViewWidth(View view){
        view.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        return view.getMeasuredWidth();
    }

    public int getViewHeight(View view){
        view.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        return view.getMeasuredHeight();
    }

}
