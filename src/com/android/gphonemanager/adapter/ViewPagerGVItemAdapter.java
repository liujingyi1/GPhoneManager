package com.android.gphonemanager.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.gphonemanager.R;

public class ViewPagerGVItemAdapter extends BaseAdapter {

	private List<ChannelInfoBean> mDataList;  
	private Context mContext;
	private int mIndex;
	private int mPageItemCount;
	private int mTotalSize;
	
    // private int itemRealNum;  
    @SuppressWarnings("unchecked")  
    public ViewPagerGVItemAdapter(Context context, List<?> list) {  
        this.mContext = context;  
        this.mDataList = (List<ChannelInfoBean>) list;  
    }  
    
    public ViewPagerGVItemAdapter(Context context, List<?> list, int index, int pageItemCount) {  
        this.mContext = context;  
        this.mIndex = index;  
        this.mPageItemCount = pageItemCount;  
        mDataList = new ArrayList<ChannelInfoBean>();  
        mTotalSize = list.size();  
        // itemRealNum=list.size()-index*pageItemCount;  
        // 当前页的item对应的实体在List<?>中的其实下标  
        int list_index = index * pageItemCount;  
        for (int i = list_index; i < list.size(); i++) {  
        	mDataList.add((ChannelInfoBean) list.get(i));  
        }  
  
    }  
	
	@Override
	public int getCount() {
		int size = mTotalSize / mPageItemCount;
		if (mIndex == size) {
			return mTotalSize - mPageItemCount * mIndex;
		} else {
			return mPageItemCount;
		}
	}

	@Override
	public Object getItem(int position) {
		return mDataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder iv;  
        if (convertView == null) { 
            iv = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.main_grid_view_item, null);
        	iv.iv_icon = (ImageView) convertView.findViewById(R.id.iv_gv_item_icon);  
        	iv.tv_name = (TextView) convertView.findViewById(R.id.tv_gv_item_Name);  
        	convertView.setTag(iv);
        }else { 
            iv = (ViewHolder) convertView.getTag();  
        }
        iv.updateViews(position, null);  
        return convertView;  
	}

    class ViewHolder{
        ImageView iv_icon;  
        TextView tv_name;  
      
        protected void updateViews(int position, Object inst) {  
            // 不管用  
            // iv_icon.setBackgroundResource(list_info.get(position).getIconID());  
            iv_icon.setImageResource(mDataList.get(position).getIconRes());  
            tv_name.setText(mDataList.get(position).getTitle());  
        }  
    } 
  
}
