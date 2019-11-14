package com.android.gphonemanager.clear;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.android.gphonemanager.clear.FileCategoryHelper.FileCategory;
import com.android.gphonemanager.clear.ScanType.FileInfo;

import android.R.layout;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.location.GpsStatus.Listener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.gphonemanager.R;

public class VideoFragment extends SelectBaseFragment {

	private static final String TAG = "BigFileFragment";
	
	ListView mListView;
	FileAdapter mAdapter;
	List<FileInfo> mFileList = new ArrayList<FileInfo>();
	View mRootView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.video_fragment, container, false);
		return mRootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mContext = getContext();
		mSelectAllView.setVisibility(View.VISIBLE);
		mListView = (ListView)mRootView.findViewById(R.id.file_list);
		
		configData();
		
		mAdapter = new FileAdapter();
		mListView.setAdapter(mAdapter);
//		mListView.setOnItemClickListener(mAdapter);
	}
	
	private void onListItemClick(int position) {
//		boolean check;
		
		FileInfo fileInfo = mFileList.get(position);
		
		MyUtil.OpenFile(mContext, fileInfo);
		
		mAdapter.notifyDataSetChanged();
	}
	
	private class FileAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mFileList.size();
		}

		@Override
		public Object getItem(int position) {
			return mFileList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder = null;
			
			FileInfo item = (FileInfo)getItem(position);
			
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().
						inflate(R.layout.app_select_item, null);
				
				viewHolder = new ViewHolder();
				viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
				viewHolder.imageFrame = (ImageView) convertView.findViewById(R.id.file_image_frame);
				viewHolder.label = (TextView) convertView.findViewById(R.id.label);
				viewHolder.size = (TextView) convertView.findViewById(R.id.size);
				viewHolder.check = (CheckBox) convertView.findViewById(R.id.checkbox);
				
				viewHolder.check.setOnClickListener(listener);
				viewHolder.image.setOnClickListener(listener);
				
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)convertView.getTag();
			}
			
//			viewHolder.image.setImageDrawable(
//					getActivity().getResources().getDrawable(R.drawable.ic_auto_start));
			viewHolder.label.setText(item.name);
			viewHolder.size.setText(Formatter.formatFileSize(mContext, item.size));
			viewHolder.check.setChecked(item.isCheck);
			
			viewHolder.check.setTag(position);
			viewHolder.image.setTag(position);
			
			mFileIconHelper.setIcon(item, viewHolder.image, viewHolder.imageFrame);
			
			return convertView;
		}

			
		
		class ViewHolder {
			public ImageView image;
			public ImageView imageFrame;
			public TextView label;
			public TextView summary;
			public TextView size;
			public CheckBox check;
		}

		View.OnClickListener listener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				switch (v.getId()) {
				case R.id.image: {
					int position = (int)(v.getTag());
					onListItemClick(position);
					break;
				}	
				case R.id.checkbox: {
					boolean check;
					int position = (int)(v.getTag());
					
					check = mFileList.get(position).isCheck;
					mFileList.get(position).isCheck = !check;
					
					selectSize += !check ? mFileList.get(position).size : -mFileList.get(position).size;
					selectCount += !check ? 1 : -1;
					
					mAdapter.notifyDataSetChanged();
					refreshUI();
					break;
				}
				default:
					break;
				}
				
			}
		};
		
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
				
		        FileCategoryHelper fileCategoryHelper = new FileCategoryHelper(mContext);
				
				for (FileInfo fileInfo : mFileList) {
					if (fileInfo.isCheck) {
						
						File file = new File(fileInfo.path);
						boolean ret = file.delete();
						if (ret) {
							FileCategory fileCategory = FileCategoryHelper.getCategoryFromPath(fileInfo.path);
							int result = fileCategoryHelper.delete(fileCategory, fileInfo.path);
							
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
		mFileList.clear();
		
		selectSize = 0;
		long videosize = 0;
		for (FileInfo fileInfo : mData.mediaList.mVideoList) {
			if (!fileInfo.isDelete) {
				videosize += fileInfo.size;
				mFileList.add(fileInfo);
				allCount++;
				if (fileInfo.isCheck) {
					selectSize += fileInfo.size;
					selectCount++;
				}
			}
		}
		mData.mediaList.videoSize = videosize;
		
		long audiosize = 0;
		for (FileInfo fileInfo : mData.mediaList.mAudioList) {
			if (!fileInfo.isDelete) {
				audiosize += fileInfo.size;
				mFileList.add(fileInfo);
				allCount++;
				if (fileInfo.isCheck) {
					selectSize += fileInfo.size;
					selectCount++;
				}
			}
		}
		mData.mediaList.videoSize = audiosize;
		
		mData.mediaList.size = audiosize + videosize;
		
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
		refreshUI();
	}

	public void selectAll(boolean select) {
		selectSize = 0;
		for (FileInfo fileInfo : mFileList) {
			fileInfo.isCheck = select;
			selectSize += fileInfo.size;
		}
		
		selectCount = select ? allCount : 0;
		selectSize = select ? selectSize : 0;
		
		refreshUI();
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onClearFinished() {
		configData();
		super.onClearFinished();
	}

}
