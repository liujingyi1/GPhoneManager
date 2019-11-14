package com.android.gphonemanager.clear;


import android.R.integer;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.ContactsContract.Contacts.Data;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.PopupWindow.OnDismissListener;

import java.io.File;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.android.gphonemanager.R;
import com.android.gphonemanager.clear.ScanType.FileInfo;

public class PhotoFragment extends SelectBaseFragment {

	View mRootView;
	ListView mListView;
	TextView mEmptyView;
	PhotoAdapter mListAdapter;
	List<FileInfo> mPhotoList = new ArrayList<FileInfo>();
	
	int COLUME_COUNT = 4;
	private GroupType mGroupType;
	
	
	
	private enum GroupType {
			GroupByDate, Similars
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.photo_fragment, container, false);
		
		return mRootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mContext = getContext();
		mSelectAllView.setVisibility(View.VISIBLE);
		mSettingView.setVisibility(View.VISIBLE);
		mListView = (ListView)mRootView.findViewById(R.id.list_view);
		mEmptyView = (TextView)mRootView.findViewById(R.id.no_item);
		
		mGroupType = GroupType.GroupByDate;
		
		configData();

 		mListAdapter = new PhotoAdapter(getContext(), mPhotoList, getActivity().getLayoutInflater(),
				mListView, mClickListener);
		mListView.setAdapter(mListAdapter);
		
		
	}
	
	public class PhotoAdapter extends BaseAdapter {

		List<FileInfo> mDataList;
		Context context;
		LayoutInflater inflater;
		List<RowConfig> rowList =  new ArrayList<RowConfig>();
		View aView;
		
		ListView listView;
		PhotoClickListener listener;
		private int rootWidth;
		private int mItemSpace = 3;
		
		public PhotoAdapter(Context context, List<FileInfo> dataList, LayoutInflater inflater,
				ListView listView, PhotoClickListener listener) {
			this.context = context;
			this.mDataList = dataList;
			this.inflater = inflater;
			this.listener = listener;
			this.listView = listView;

			initData();
		}
		
		public void setData(List<FileInfo> data) {
			mDataList = data;
		}
		
		public void initData() {
			
			int group = -1;
			int size = mDataList.size();
			int i = 0;
			String tag = "start";
			
			rowList.clear();
			
			while (i < size) {
				
				int j = 0;
				for (; j < COLUME_COUNT && i+j<size; j++) {
					FileInfo fileInfo = mDataList.get(i+j);
					if (!fileInfo.tag.equals(tag)) {
						
						if (j == 0) {
							RowConfig rowConfig = new RowConfig();
							rowConfig.isTitle = true;
							rowList.add(rowConfig);
							tag = fileInfo.tag;
							group++;
							break;
						} else {
							RowConfig rowConfig = new RowConfig();
							rowConfig.position = i;
							rowConfig.count = j;
							rowConfig.group = group;
							rowList.add(rowConfig);
							
							i += j;
							
							group++;
							tag=fileInfo.tag;
							RowConfig rowConfig1 = new RowConfig();
							rowConfig1.isTitle = true;
							rowList.add(rowConfig1);
							
							break;
						}
					}
					if (i+j == size-1) {
						RowConfig rowConfig = new RowConfig();
						rowConfig.position = i;
						rowConfig.count = j+1;
						rowConfig.group = group;
						rowList.add(rowConfig);
						i += COLUME_COUNT;
						break;
					}
				}
				
				if (j == COLUME_COUNT) {
					RowConfig rowConfig = new RowConfig();
					rowConfig.position = i;
					rowConfig.count = j;
					rowConfig.group = group;
					rowList.add(rowConfig);
					i += COLUME_COUNT;
				}
			}
		}
		
		@Override
		public int getCount() {
			return rowList == null ? 0 :  rowList.size();
		}

		@Override
		public ArrayList<FileInfo> getItem(int position) {

			RowConfig rowConfig = rowList.get(position);
			if (rowConfig.isTitle) {
				return null;
			} else {
				int columnCount = rowConfig.count;
				ArrayList<FileInfo> resultList = new ArrayList<FileInfo>(columnCount);
				
				int count = 0;
				for (int i = 0; i < position; i++) {
					 count = count + (rowList.get(i).isTitle ? 0 : rowList.get(i).count);
				}
				
				for (int i = 0; i < columnCount; i++) {
					resultList.add(mDataList.get(count+i));
				}
				return resultList;
			}
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
	        
	        ArrayList<FileInfo> fileInfos = getItem(position);
	        
	        if (fileInfos == null) {
				convertView = inflater.inflate(R.layout.photo_group_title, null);
				TextView textView = (TextView)convertView.findViewById(R.id.title);

				if (mGroupType != GroupType.Similars) {
					ArrayList<FileInfo> temp = getItem(position+1);
					textView.setText(temp.get(0).tag);
				}
				return convertView;
	        }

	        PhotoRow photoRow;
	        if (convertView == null || convertView.getTag() == null) {
	        	photoRow = new PhotoRow(mContext, position);
	        	photoRow.setTag(position);
	        } else {
	        	photoRow = (PhotoRow)convertView;
	        }
	        photoRow.configureRow(fileInfos, position == getCount() - 1);

	        return photoRow;
		}
		
		class RowConfig {
//			boolean first;
//			boolean lase;
			int group;
			boolean isTitle = false;
			int count;
			int position;
		}
		
	    private class PhotoRow extends FrameLayout {

	        int position;

	        public PhotoRow(Context context, int position) {
	            super(context);
	            setFocusable(false);
	            this.position = position;
	        }

	        public void configureRow(ArrayList<FileInfo> list, boolean isLastRow) {
	            int columnCount = list.size();
	            for (int columnCounter = 0; columnCounter < columnCount; columnCounter++) {
	            	FileInfo data = list.get(columnCounter);
	                addItem(data, columnCounter, isLastRow);
	            }
	            int childCount = getChildCount();
	            if (columnCount < childCount) {
	            	for (int i = columnCount; i < childCount;i++) {
	            		getChildAt(i).setVisibility(View.GONE);
	            	}
	            }
	        }

	        private void addItem(FileInfo fileInfo, int childIndex, boolean islastRow) {
	            final PhotoItemView itemview;
	            if (getChildCount() <= childIndex) {
	                itemview = (PhotoItemView) inflate(mContext, R.layout.photo_item_view, null);
	                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
	                        ViewGroup.LayoutParams.WRAP_CONTENT,
	                        ViewGroup.LayoutParams.WRAP_CONTENT);
	                params.setMargins(
	                        0,
	                        mItemSpace / 2,
	                        0,
	                        mItemSpace / 2);
	                
	                int rootWidth = mListView.getWidth();
	                int childWidth = (rootWidth - (COLUME_COUNT+1) * mItemSpace) / COLUME_COUNT;
	                
	                params.height = childWidth;
	                params.width = childWidth;
	                itemview.setLayoutParams(params);
	                itemview.setListener(listener);
	                itemview.setIconHelper(mFileIconHelper);
	                addView(itemview);
	            } else {
	                itemview = (PhotoItemView)getChildAt(childIndex);
	                if (itemview.getVisibility() == View.GONE) {
	                	itemview.setVisibility(View.VISIBLE);
	                }
	            }
	            itemview.loadData(fileInfo);
	        }

	        @Override
	        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
	            //super.onLayout(changed, left, top, right, bottom);
	            final int count = getChildCount();
	            
	            int mRootWidth = mListView.getWidth();
	            
	            int childLeft = mItemSpace;
	            
	            View child = null;
	            
	            for (int i = 0; i < count; i++) {
	                child = getChildAt(i);
	                int childWidth = child.getMeasuredWidth();
	                child.layout(childLeft, mItemSpace / 2, childLeft + childWidth, mItemSpace / 2 + child.getMeasuredHeight());
	                childLeft += mItemSpace + childWidth;
	            }
	        }

	        @Override
	        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	        }
	    }
		
	}
	
	PhotoClickListener mClickListener = new PhotoClickListener() {
		
		@Override
		public void onPhotoClick(FileInfo fileInfo) {
			
			MyUtil.OpenFile(mContext, fileInfo);
		}

		@Override
		public void onPhotoSelected(FileInfo fi, boolean check) {
			int index = mPhotoList.indexOf(fi);
			mPhotoList.get(index).isCheck = check;
			
			selectSize += check ? mPhotoList.get(index).size : -mPhotoList.get(index).size;
			
			selectCount += check ? 1 : -1;
			refreshUI();
		}
	};
	

	public interface PhotoClickListener {

		void onPhotoClick(FileInfo fi);
		void onPhotoSelected(FileInfo fi, boolean add);

	}

	
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
            if (getView() == null) {
                return;
            }
            switch (msg.what) {
        	case MSG_CLEAR_START: {
        		
        		mListener.showProgressBar(true);
        	
        		break;
        	}
        	case MSG_CLEAR_COMPLETE: {
        		onClearFinished();
        	
        		break;
        	}
        }
		}
	};

	@Override
	public void onClearAction() {
		deleteThread = new Thread(new Runnable() {

			@Override
			public void run() {
				clearSize = 0;
				
		        Message msg = mHandler.obtainMessage(MSG_CLEAR_START);
		        mHandler.sendMessage(msg);
				
				ContentResolver resolver = getActivity().getContentResolver();
				Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				for (FileInfo fileInfo : mPhotoList) {
					if (fileInfo.isCheck) {
						
						File file = new File(fileInfo.path);
						boolean ret = file.delete();
						if (ret) {
							String where = MediaStore.Images.Media.DATA + "='" + fileInfo.path + "'";
							
							int result = resolver.delete(uri, where, null);
							
							if (result > 0) {
								clearSize += fileInfo.size;
								fileInfo.isDelete = true;
							}
						}
					}
				}
				
		        Message msg1 = mHandler.obtainMessage(MSG_CLEAR_COMPLETE);
		        mHandler.sendMessage(msg1);
			}
			
		});
		deleteThread.start();
	}
	
	protected void configData() {
		mPhotoList.clear();
		
		selectSize = 0;
		long size = 0;
		for (FileInfo fileInfo : mData.picList.mPictureList) {
			if (!fileInfo.isDelete) {
				size += fileInfo.size;
				mPhotoList.add(fileInfo);
				allCount++;
				if (fileInfo.isCheck) {
					selectSize += fileInfo.size;
					selectCount++;
				}
			}
		}
		mData.picList.size = size;
		
		switch (mGroupType) {
			case Similars:
				configSimilarGroup();
				break;
	
			default:
				configDateGroup();
				break;
		}
		
		if (mListAdapter != null) {
			if (mPhotoList.size() == 0) {
				mEmptyView.setVisibility(View.VISIBLE);
				mListView.setVisibility(View.GONE);
			} else {
				mEmptyView.setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);
                mListAdapter.setData(mPhotoList);
                mListAdapter.initData();
                mListAdapter.notifyDataSetChanged();
			}
		}
		refreshUI();
	}
	
	private void configDateGroup() {
 		Calendar calendar = Calendar.getInstance();
 		calendar.setTimeInMillis(System.currentTimeMillis());
 		
 		int year = calendar.get(Calendar.YEAR);
 		int month = calendar.get(Calendar.MONTH)+1;
 		int day = calendar.get(Calendar.DAY_OF_MONTH);
 		
 		Date lineDateDay = new Date(year-1900,month-1, day);
 		Date lineDateMonty = new Date(year-1900,month-2, day);
 		Date lineDateYear = new Date(year-1-1900,month-1, day);
 		
 		for (int i = 0; i < mPhotoList.size(); i++) {
 			FileInfo info = mPhotoList.get(i);
 			
 			if (info.lastModify*1000 > lineDateMonty.getTime()) {
 				info.tag = MyUtil.formatDateString(mContext, info.lastModify*1000);
 			} else if (info.lastModify*1000 > lineDateYear.getTime()) {
 				info.tag = mContext.getString(R.string.a_month_ago);
 			}  else {
 				info.tag = mContext.getString(R.string.a_year_ago);
			}
 		}
	}
	
	private void configSimilarGroup() {
		mPhotoList = SimilarPhoto.find(mContext, mPhotoList);
	}
	
	@Override
	public void selectAll(boolean select) {
		selectSize = 0;
		for (FileInfo fileInfo : mPhotoList) {
			fileInfo.isCheck = select;
			selectSize += fileInfo.size;
		}
		
		selectCount = select ? allCount : 0;
		selectSize = select ? selectSize : 0;
		
		refreshUI();
		if (mListAdapter != null) {
			mListAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onSetting() {
		popupDialog();
	}
	
	private void popupDialog() {
		View popupView = getActivity().getLayoutInflater().inflate(R.layout.photo_menu, null);
		
		View date = popupView.findViewById(R.id.date);
		View similar = popupView.findViewById(R.id.similar);
		
		View rootView = getRootView(getActivity());
		
		int itemHeight = getResources().getDimensionPixelSize(R.dimen.photo_popup_item_height);
		int height = itemHeight * 2;
		int width = getResources().getDimensionPixelSize(R.dimen.photo_popup_menu_width);
		
		Resources resources = getResources();
		int actionBarHeight = resources.getDimensionPixelSize(R.dimen.actionbar_size);
		int resourceid = resources.getIdentifier("status_bar_height", "dimen", "android");
		int statusHeight = resources.getDimensionPixelSize(resourceid);
		
		
		PopupWindow window = new PopupWindow(popupView, width, height);
		window.setFocusable(true);
		window.setOutsideTouchable(true);
		window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
		window.setAnimationStyle(R.style.menu_popup_anim);
		window.update();
		window.showAtLocation(rootView, Gravity.TOP|Gravity.RIGHT, 0, actionBarHeight+statusHeight);
		
		date.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mGroupType != GroupType.GroupByDate) {
					mGroupType = GroupType.GroupByDate;
					window.dismiss();
					configData();
				} else {
					window.dismiss();
				}
			}
		});
		similar.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mGroupType != GroupType.Similars) {
					mGroupType = GroupType.Similars;
					window.dismiss();
					configData();
				} else {
					window.dismiss();
				}
			}
		});
	}
	
    private View getRootView(Activity context)  
    {  
        return ((ViewGroup)context.findViewById(android.R.id.content)).getChildAt(0);  
    }  

	@Override
	public void onClearFinished() {
		configData();
		super.onClearFinished();
	}
}
