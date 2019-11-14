package com.android.gphonemanager.clear;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.android.gphonemanager.clear.ScanType.FileInfo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.gphonemanager.R;

public class BigFileFragment extends SelectBaseFragment {

	private static final String TAG = "BigFileFragment";
	
	ListView mListView;
	FileAdapter mAdapter;
	List<FileInfo> mFileList = new ArrayList<FileInfo>();
	View mRootView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.big_file_fragment, container, false);
		
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
		mListView.setOnItemClickListener(mAdapter);
	}
	
	private void onListItemClick(int position) {
		boolean check;
		
		check = mFileList.get(position).isCheck;
		mFileList.get(position).isCheck = !check;
		
		selectSize += !check ? mFileList.get(position).size : -mFileList.get(position).size;
		
		selectCount += !check ? 1 : -1;
		
		mAdapter.notifyDataSetChanged();
		refreshUI();
	}
	
	private class FileAdapter extends BaseAdapter implements AdapterView.OnItemClickListener{

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
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)convertView.getTag();
			}
			
			viewHolder.label.setText(item.name);
			viewHolder.size.setText(Formatter.formatFileSize(mContext, item.size));
			viewHolder.check.setChecked(item.isCheck);
			
			mFileIconHelper.setIcon(item, viewHolder.image, viewHolder.imageFrame);
			
			return convertView;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			onListItemClick(position);
		}
		
		class ViewHolder {
			public ImageView image;
			public ImageView imageFrame;
			public TextView label;
			public TextView summary;
			public TextView size;
			public CheckBox check;
		}
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
				
				for (FileInfo fileInfo : mFileList) {
					if (fileInfo.isCheck) {
						File file = new File(fileInfo.path);
						boolean ret = file.delete();
						if (ret) {
							clearSize += fileInfo.size;
							fileInfo.isDelete = true;
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
		long size = 0;
		for (FileInfo fileInfo : mData.bigFileList.mBigFileList) {
			if (!fileInfo.isDelete) {
				size += fileInfo.size;
				mFileList.add(fileInfo);
				allCount++;
				if (fileInfo.isCheck) {
					selectSize += fileInfo.size;
					selectCount++;
				}
			}
		}

		mData.bigFileList.size = size;
		
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
