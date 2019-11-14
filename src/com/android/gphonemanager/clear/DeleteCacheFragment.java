package com.android.gphonemanager.clear;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.android.gphonemanager.R;
import com.android.gphonemanager.clear.ScanType.ApkInfo;
import com.android.gphonemanager.clear.ScanType.FileInfo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager.CaptioningChangeListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.gphonemanager.clear.FileIconHelper;

import android.content.pm.IPackageDataObserver;


public class DeleteCacheFragment extends SelectBaseFragment {
	
	private static final String TAG = "DeleteCacheFragment";
	
	Context mContext;
	ExpandableListView mExpandListView;
	ExpandableListAdapter mExpandAdapter;
	List<APPInfo> cacheList = new ArrayList<APPInfo>();
	List<APPInfo> dataList = new ArrayList<APPInfo>();
	List<ApkInfo> newAPKList = new ArrayList<ApkInfo>();
	List<ApkInfo> redundantAPKList = new ArrayList<ApkInfo>();
	List<FileInfo> junkFileList = new ArrayList<FileInfo>();
	
	List<GroupItem> groupList = new ArrayList<>();
	
	class GroupItem{
		Drawable icon;
		String title;
		int count;
		int selectCount;
		long size;
		int type;
		boolean check;
	}
	
	private static final int GROUP_TYPE_CACHE = 0;
	private static final int GROUP_TYPE_DATA = 1;
	private static final int GROUP_TYPE_NEWAPK = 2;
	private static final int GROUP_TYPE_REDUNDANTAPK =3;
	private static final int GROUP_TYPE_JUNK = 4;
	
	View mRootView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.delete_cache_fragment, container, false);
		
		return mRootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		mContext = getContext();
		
		configureData();
		
		mExpandListView = (ExpandableListView)mRootView.findViewById(R.id.expand_list);
		mExpandAdapter = new ExpandAdapter();
		mExpandListView.setAdapter(mExpandAdapter);
		
		mExpandListView.setOnChildClickListener(new OnChildClickListener() {
			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				
				switch (groupList.get(groupPosition).type) {
					case GROUP_TYPE_CACHE: {
						 APPInfo item = cacheList.get(childPosition);
						 boolean check = !item.isCacheCheck;
						 cacheList.get(childPosition).isCacheCheck = check;
						 
						 long size = item.mSizeInfo.mCacheSize + item.mSizeInfo.mExternalCacheSize;
						 selectSize += check ? size : -size;
						 
						 groupList.get(groupPosition).selectCount += check ? 1 : -1;
						 break;
					}
					case GROUP_TYPE_DATA: {
						 APPInfo item = dataList.get(childPosition);
						 boolean check = !item.isCheck;
						 dataList.get(childPosition).isCheck = check;
						 
						 long size = item.mSizeInfo.mDataSize + item.mSizeInfo.mExternalDataSize;
						 selectSize += check ? size : -size;
						 
						 groupList.get(groupPosition).selectCount += check ? 1 : -1;
						 break;
					}
					case GROUP_TYPE_NEWAPK: {
						 ApkInfo item = newAPKList.get(childPosition);
						 boolean check = !item.isCheck;
						 newAPKList.get(childPosition).isCheck = check;
						 
						 selectSize += check ? item.size : -item.size;
						 
						 groupList.get(groupPosition).selectCount += check ? 1 : -1;
						 break;
					}
					case GROUP_TYPE_REDUNDANTAPK: {
						 ApkInfo item = redundantAPKList.get(childPosition);
						 boolean check = !item.isCheck;
						 redundantAPKList.get(childPosition).isCheck = check;
						 
						 selectSize += check ? item.size : -item.size;
						 
						 groupList.get(groupPosition).selectCount += check ? 1 : -1;
						 break;
					}
					case GROUP_TYPE_JUNK: {
						 FileInfo item = junkFileList.get(childPosition);
						 boolean check = !item.isCheck;
						 junkFileList.get(childPosition).isCheck = check;
						 
						 selectSize += check ? item.size : -item.size;
						 
						 groupList.get(groupPosition).selectCount += check ? 1 : -1;
						 break;
					}
					
				}
				refreshExpandUI(groupPosition);
				return false;
			}
		});

		mExpandListView.setOnGroupClickListener(new OnGroupClickListener() {
			
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				if (parent.isGroupExpanded(groupPosition)) {
					parent.collapseGroup(groupPosition);
				} else {
					parent.expandGroup(groupPosition, false);
				}
				return true;
			}
		});
	}

	class ExpandAdapter extends BaseExpandableListAdapter {

		private  final int[] EMPTY_STATE_SET = {};
		/** State indicating the group is expanded. */
		private  final int[] GROUP_EXPANDED_STATE_SET = { android.R.attr.state_expanded };
		/** State indicating the group is empty (has no children). */
		private  final int[] GROUP_EMPTY_STATE_SET = { android.R.attr.state_empty };
		/** State indicating the group is expanded and empty (has no children). */
		private  final int[] GROUP_EXPANDED_EMPTY_STATE_SET = { android.R.attr.state_expanded,
		        android.R.attr.state_empty };
		/** States for the group where the 0th bit is expanded and 1st bit is empty. */
		private  final int[][] GROUP_STATE_SETS = { EMPTY_STATE_SET, // 00
		        GROUP_EXPANDED_STATE_SET, // 01
		        GROUP_EMPTY_STATE_SET, // 10
		        GROUP_EXPANDED_EMPTY_STATE_SET // 11
		};
		
		@Override
		public final void onGroupCollapsed(int groupPosition) {
			onGroupCollapsedEx(groupPosition);
			notifyDataSetChanged();
		}
		
		@Override
		public final void onGroupExpanded(int groupPosition) {
			onGroupExpandedEx(groupPosition);
			notifyDataSetChanged();
		}
		
		protected void  setIndicatorState(Drawable indicator, int groupPosition, boolean expand) {
		    final int stateSetIndex = (expand ? 1 : 0) | // Expanded?
		            (getChildrenCount(groupPosition) == 0 ? 2 : 0); // Empty?
		    
		    indicator.setState(GROUP_STATE_SETS[stateSetIndex]);
//		    indicator.
		}
		
		protected void onGroupCollapsedEx(int groupPosition) {
			
		}
		
		protected void onGroupExpandedEx(int groupPosition) {
			
		}
		
		@Override
		public int getGroupCount() {
			return groupList.size();
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return groupList.get(groupPosition).count;
		}

		@Override
		public Object getGroup(int groupPosition) {
			
			return groupList.get(groupPosition);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			switch (groupList.get(groupPosition).type) {
			case GROUP_TYPE_CACHE:
				return cacheList.get(childPosition);
			case GROUP_TYPE_DATA:
				return dataList.get(childPosition);
			case GROUP_TYPE_NEWAPK:
				return newAPKList.get(childPosition);
			case GROUP_TYPE_REDUNDANTAPK:
				return redundantAPKList.get(childPosition);
			case GROUP_TYPE_JUNK:
				return junkFileList.get(childPosition);

			default:
				return null;
			}
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			
			GroupHolder groupHolder = null;
			
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().
						inflate(R.layout.group_item_view, null);
				groupHolder = new GroupHolder();
				
				groupHolder.titleText = (TextView) convertView.findViewById(R.id.group_title);
				groupHolder.summary = (TextView) convertView.findViewById(R.id.group_title_summary);
				groupHolder.size = (TextView) convertView.findViewById(R.id.size);
				groupHolder.check = (CheckBox) convertView.findViewById(R.id.checkbox);
				groupHolder.indicator = convertView.findViewById(R.id.indicator);
				groupHolder.select = convertView.findViewById(R.id.select);
				groupHolder.divider = convertView.findViewById(R.id.divider_line);
				groupHolder.itemGap = convertView.findViewById(R.id.item_gap);
				groupHolder.select.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						int position = (int)v.getTag();
						boolean isCheck = groupList.get(position).check;
						
						isCheck = !isCheck;
						onGroupCheckClick(position, isCheck);
					}
				});
				groupHolder.itemGap.setEnabled(false);
				groupHolder.itemGap.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
					}
				});
				
				convertView.setTag(groupHolder);
			} else {
				groupHolder = (GroupHolder)convertView.getTag();
			}
			
			
			setIndicatorState(groupHolder.indicator.getBackground(), groupPosition, isExpanded);
			
			groupHolder.titleText.setText(groupList.get(groupPosition).title);
			groupHolder.summary.setText(getGroupSummary(groupList.get(groupPosition).type));
			String sizeStr = Formatter.formatFileSize(mContext, groupList.get(groupPosition).size);
			groupHolder.size.setText(sizeStr);
			groupHolder.select.setTag(groupPosition);
			groupHolder.check.setChecked(groupList.get(groupPosition).check);
			
			groupHolder.itemGap.setVisibility(groupPosition == 0 ? View.GONE : View.VISIBLE);
			groupHolder.divider.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
			
			return convertView;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			
			ChildHolder childHolder = null;
			
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().
						inflate(R.layout.app_select_item, null);
				childHolder = new ChildHolder();
				childHolder.image = (ImageView) convertView
						.findViewById(R.id.image);
				childHolder.imageFrame = (ImageView) convertView
						.findViewById(R.id.file_image_frame);
				childHolder.label = (TextView) convertView.findViewById(R.id.label);
				childHolder.size = (TextView) convertView.findViewById(R.id.size);
				childHolder.check = (CheckBox) convertView.findViewById(R.id.checkbox);
				childHolder.install = (TextView) convertView.findViewById(R.id.installapp);
				
				convertView.setTag(childHolder);
			} else {
				childHolder = (ChildHolder)convertView.getTag();
			}
			
			
			childHolder.install.setVisibility(View.GONE);

			switch (groupList.get(groupPosition).type) {
				case GROUP_TYPE_CACHE: {
					APPInfo appInfo = cacheList.get(childPosition);
					childHolder.image.setImageDrawable(appInfo.iconBitmap);
					childHolder.label.setText(appInfo.label);
					String size = Formatter.formatFileSize(mContext, 
							appInfo.mSizeInfo.mCacheSize+appInfo.mSizeInfo.mExternalCacheSize);
					childHolder.size.setText(size);
					childHolder.check.setChecked(appInfo.isCacheCheck);
					break;
				}
				case GROUP_TYPE_DATA:{
					APPInfo appInfo = dataList.get(childPosition);
					childHolder.image.setImageDrawable(appInfo.iconBitmap);
					childHolder.label.setText(appInfo.label);
					String size = Formatter.formatFileSize(mContext, 
							appInfo.mSizeInfo.mDataSize+appInfo.mSizeInfo.mExternalDataSize);
					childHolder.size.setText(size);
					childHolder.check.setChecked(appInfo.isCheck);
					break;
				}
				case GROUP_TYPE_NEWAPK:{
					ApkInfo apkInfo = newAPKList.get(childPosition);
					childHolder.image.setImageDrawable(apkInfo.apk_icon);
					childHolder.label.setText(apkInfo.mLabel);
					String size = Formatter.formatFileSize(mContext, apkInfo.size);
					childHolder.size.setText(size);
					childHolder.check.setChecked(apkInfo.isCheck);
					
					String info = null;
					if (apkInfo.mType == ScanType.APK_UNINSTALLED) {
						info = getString(R.string.install_apk_text);
					} else if (apkInfo.mType == ScanType.APK_INSTALLED_UPDATE) {
						info = getString(R.string.update_apk_text);
					}
					
					if (info != null) {
						childHolder.install.setVisibility(View.VISIBLE);
						childHolder.install.setText(info);
						//if (childHolder.install.getTag() != null) {
							childHolder.install.setOnClickListener(new OnClickListener() {
								
								@Override
								public void onClick(View v) {
									MyUtil.OpenFile(mContext, apkInfo.fileInfo);
								}
							});
							childHolder.install.setTag(true);
						//}
					}
					
					//mFileIconHelper.setIcon(apkInfo.fileInfo, childHolder.image, childHolder.imageFrame);
					
					break;
				}
				case GROUP_TYPE_REDUNDANTAPK:{
					ApkInfo apkInfo = redundantAPKList.get(childPosition);
					childHolder.image.setImageDrawable(apkInfo.apk_icon);
					childHolder.label.setText(apkInfo.mLabel);
					String size = Formatter.formatFileSize(mContext, apkInfo.size);
					childHolder.size.setText(size);
					childHolder.check.setChecked(apkInfo.isCheck);
					break;
				}
				case GROUP_TYPE_JUNK:{
					FileInfo fileInfo = junkFileList.get(childPosition);
					childHolder.image.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_auto_start));
					childHolder.label.setText(fileInfo.name);
					String size = Formatter.formatFileSize(mContext, fileInfo.size);
					childHolder.size.setText(size);
					childHolder.check.setChecked(fileInfo.isCheck);
					
					mFileIconHelper.setIcon(fileInfo, childHolder.image, childHolder.imageFrame);
					break;
				}
				default:
					break;
			}
			
			View view = (View)convertView.findViewById(R.id.divider_line);
			view.setVisibility(View.GONE);
			
			return convertView;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		
		private String getGroupSummary(int type) {
			String[] strings = getResources().getStringArray(R.array.expand_group_summary);
			return strings[type];
		}
		
	}
	
	class GroupHolder {
		TextView titleText;
		TextView summary;
		TextView size;
		View select;
		View indicator;
		CheckBox check;
		View itemGap;
		View divider;
	}
	class ChildHolder {
		public ImageView image;
		public ImageView imageFrame;
		public TextView install;
		public TextView label;
		public TextView summary;
		public TextView size;
		public CheckBox check;
	}
	
	@Override
	public void onClearAction() {
		deleteApps();

	}
	
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (getView() == null) {
                return;
            }
            switch (msg.what) {
            	case MSG_CLEAR_START: {
            		Log.i("jingyi", "start ------------");
            		
            		mListener.showProgressBar(true);
            	
            		break;
            	}
            	case MSG_CLEAR_COMPLETE: {
            		completeCount++;
            		if (completeCount == 3) {
            			onClearFinished();
            		}
            	
            		break;
            	}
                case MSG_CLEAR_USER_DATA: {
                	if (mActivityManager == null) {
                		mActivityManager = (ActivityManager)
        		                getActivity().getSystemService(Context.ACTIVITY_SERVICE);
                	}
		            if (mClearDataObserver == null) { // Lazy initialization of observer
		            	mClearDataObserver = new ClearUserDataObserver();
		            }
		            String packagename = requestCachePackage(false);
		            if (packagename != null) {
		            	boolean ret = mActivityManager.clearApplicationUserData(packagename, mClearDataObserver);
		            	if (!ret) {
		            		Log.i("jingyi", packagename+" data can not be delete");
		            	}
		            } else {
				        Message msg1 = mHandler.obtainMessage(MSG_CLEAR_COMPLETE);
				        mHandler.sendMessage(msg1);
					}
                    break;
                }
                case MSG_CLEAR_CACHE: {
    				if (mPackageManager == null) {
    					mPackageManager = getActivity().getPackageManager();
    				}
                	
		            if (mClearCacheObserver == null) { // Lazy initialization of observer
		                mClearCacheObserver = new ClearCacheObserver();
		            }
		            String packagename = requestCachePackage(true);
		            if (packagename != null) {
		            	mPackageManager.deleteApplicationCacheFiles(packagename, mClearCacheObserver);
		            } else {
				        Message msg1 = mHandler.obtainMessage(MSG_CLEAR_COMPLETE);
				        mHandler.sendMessage(msg1);
		            }
                    break;
                }
            }
        }
        
        String requestCachePackage(boolean isCache) {
        	
        	if (isCache) {
				for (APPInfo appInfo : cacheList) {
		            if (appInfo.isCacheCheck) {
		            	appInfo.isCacheCheck = false;
		            	return appInfo.mPackageName;
		            }
				} 
        	} else {
    			for (APPInfo appInfo : dataList) {
    				if (appInfo.isCheck) {
    					appInfo.isCheck = false;
    	            	return appInfo.mPackageName;
    	            }
    			}
        	}
            return null;
        }
        
    };
	
    private ClearCacheObserver mClearCacheObserver;
    private ClearUserDataObserver mClearDataObserver;
    private static final int OP_SUCCESSFUL = 1001;
    private static final int OP_FAILED = 1002;
    private static final int MSG_CLEAR_USER_DATA = 1003;
    private static final int MSG_CLEAR_CACHE = 1004;
    
    private int completeCount = 0;
    
    PackageManager mPackageManager;
    ActivityManager mActivityManager;
    
	private void deleteApps() {
		deleteThread = new Thread(new Runnable() {
			@Override
			public void run() {
				
				clearSize = 0;
				completeCount = 0;
				
				Message msg =  mHandler.obtainMessage(MSG_CLEAR_START);
				mHandler.sendMessage(msg);
				
				for (ApkInfo info : newAPKList) {
					if (info.isCheck) {
						File file = new File(info.fileInfo.path);
						boolean ret = file.delete();
						if (ret) {
							clearSize += info.size;
							info.isDelete = true;
						} else {
							Log.i("jingyi", "file "+info.fileInfo.path+" delete fail");
						}
					}
				}
				
				for (ApkInfo info : redundantAPKList) {
					if (info.isCheck) {
						File file = new File(info.fileInfo.path);
						boolean ret = file.delete();
						if (ret) {
							clearSize += info.size;
							info.isDelete = true;
						} else {
							Log.i("jingyi", "file "+info.fileInfo.path+" delete fail");
						}
					}
				}
				
				for (FileInfo info : junkFileList) {
					if (info.isCheck) {
						File file = new File(info.path);
						boolean ret = file.delete();
						if (ret) {
							clearSize += info.size;
							info.isDelete = true;
						} else {
							Log.i("jingyi", "file "+info.path+" delete fail");
						}
					}
				}
				
		        Message msg1 = mHandler.obtainMessage(MSG_CLEAR_COMPLETE);
		        mHandler.sendMessage(msg1);
		        
		        final Message cachemsg = mHandler.obtainMessage(MSG_CLEAR_CACHE);
		        mHandler.sendMessage(cachemsg);
		        
		        final Message datamsg = mHandler.obtainMessage(MSG_CLEAR_USER_DATA);
		        mHandler.sendMessage(datamsg);
			}
		});
		deleteThread.start();
	}
	
	class ClearCacheObserver extends IPackageDataObserver.Stub {
	    public void onRemoveCompleted(final String packageName, final boolean succeeded) {
	    	
	    	synchronized (mProcessorDataLock) {
		    	
			    if (succeeded) {
				   for (APPInfo appInfo : cacheList) {
					   if (appInfo.mPackageName.equals(packageName)) {
						   long cacheSize = appInfo.mSizeInfo.mCacheSize;
						   long externalCache = appInfo.mSizeInfo.mExternalCacheSize;
						   
						   clearSize += cacheSize;
						   clearSize += externalCache;
						   
						   appInfo.mSizeInfo.mCacheSize = 0;
						   appInfo.mSizeInfo.mExternalCacheSize = 0;
						   
						   appInfo.isDelete = true;
					   }
				   }
			    }
		    	
		        final Message msg = mHandler.obtainMessage(MSG_CLEAR_CACHE);
		        msg.arg1 = succeeded ? OP_SUCCESSFUL : OP_FAILED;
		        mHandler.sendMessage(msg);
	    	}

	    }
	}
	
	class ClearUserDataObserver extends IPackageDataObserver.Stub {
	   public void onRemoveCompleted(final String packageName, final boolean succeeded) {
		   synchronized (mProcessorDataLock) {
			   
			   if (succeeded) {
				   for (APPInfo appInfo : dataList) {
					   if (appInfo.mPackageName.equals(packageName)) {
						   
						   long dataSize = appInfo.mSizeInfo.mDataSize;
						   long externalData = appInfo.mSizeInfo.mExternalDataSize;
						   
						   clearSize += dataSize;
						   clearSize += externalData;
						   
						   appInfo.mSize = appInfo.mSize - dataSize - externalData;
						   appInfo.mInternalSize = appInfo.mInternalSize - dataSize;
						   appInfo.mExternalSize = appInfo.mExternalSize - externalData;
						   appInfo.mSizeInfo.mDataSize = 0;
						   appInfo.mSizeInfo.mExternalDataSize = 0;
	                       
						   appInfo.mSizeStr = Formatter.formatFileSize(mContext, appInfo.mSize);
						   appInfo.mInternalSizeStr = Formatter.formatFileSize(mContext, appInfo.mInternalSize);
						   appInfo.mExternalSizeStr = Formatter.formatFileSize(mContext, appInfo.mExternalSize);
						   
						   appInfo.isDelete = true;
					   }
				   }
			   }
			   
		       final Message msg = mHandler.obtainMessage(MSG_CLEAR_USER_DATA);
		       msg.arg1 = succeeded ? OP_SUCCESSFUL : OP_FAILED;
		       mHandler.sendMessage(msg);
		   }
	    }
	}
	
    private final Object mProcessorDataLock = new Object();

	private void configureData() {
		
		cacheList.clear();
		dataList.clear();
		newAPKList.clear();
		redundantAPKList.clear();
		junkFileList.clear();
		groupList.clear();
		selectSize = 0;
		
		mData.appInfoList.appSize = 0;
		mData.appInfoList.cacheSize = 0;
		mData.appInfoList.dataSize = 0;
		for (APPInfo appInfo : mData.appInfoList.mAppInfoList) {
			if (appInfo.isAllowClearData && (appInfo.mSizeInfo.mDataSize 
					+ appInfo.mSizeInfo.mExternalDataSize) > 0) {
				dataList.add(appInfo);
				if (appInfo.isCheck) {
					selectSize += appInfo.mSizeInfo.mDataSize 
							+ appInfo.mSizeInfo.mExternalDataSize;
				}
			}
			
			if ((appInfo.mSizeInfo.mCacheSize + appInfo.mSizeInfo.mExternalCacheSize) > 0) {
				cacheList.add(appInfo);
				if (appInfo.isCacheCheck) {
					selectSize += appInfo.mSizeInfo.mCacheSize 
							+ appInfo.mSizeInfo.mExternalCacheSize;
				}
			}
			
			mData.appInfoList.appSize += appInfo.mSize;
			mData.appInfoList.dataSize += (appInfo.mSizeInfo.mDataSize
					+ appInfo.mSizeInfo.mExternalDataSize);
			mData.appInfoList.cacheSize += (appInfo.mSizeInfo.mCacheSize
					+ appInfo.mSizeInfo.mExternalCacheSize);
		}
		
		long apksize = 0;
		for (ApkInfo apkInfo : mData.junkList.mAPKList) {
			if (!apkInfo.isDelete) {
				if (apkInfo.mType == ScanType.APK_INSTALLED_UPDATE 
						|| apkInfo.mType == ScanType.APK_UNINSTALLED) {
					newAPKList.add(apkInfo);
				} else {
					redundantAPKList.add(apkInfo);
				}
				apksize += apkInfo.size;
				if (apkInfo.isCheck) {
					selectSize += apkInfo.size;
				}
			}
		}
		mData.junkList.apkSize = apksize;
		
		
		long longsize = 0;
		for (FileInfo info : mData.junkList.mLogList) {
			if (!info.isDelete) {
				longsize += info.size;
				junkFileList.add(info);
				if (info.isCheck) {
					selectSize += info.size;
				}
			}
		}
		mData.junkList.logSize = longsize;
		
		long syslogSize = 0;
		for (FileInfo info : mData.junkList.mSystemLogList) {
			if (!info.isDelete) {
				syslogSize += info.size;
				junkFileList.add(info);
				if (info.isCheck) {
					selectSize += info.size;
				}
			}
		}
		mData.junkList.systemLogSize = syslogSize;
		
		long thumbsize = 0;
		for (FileInfo info : mData.junkList.mThumbList) {
			if (!info.isDelete) {
				thumbsize += info.size;
				junkFileList.add(info);
				if (info.isCheck) {
					selectSize += info.size;
				}
			}
		}
		mData.junkList.thumbSize = thumbsize;
		
		long empty = 0;
		for (FileInfo info : mData.junkList.mEmptyList) {
			if (!info.isDelete) {
				junkFileList.add(info);
				if (info.isCheck) {
					selectSize += info.size;
				}
			}
		}
		mData.junkList.emptySzie = 0;
		
		long oldSize = 0;
		for (FileInfo info : mData.junkList.mOldList) {
			if (!info.isDelete) {
				oldSize +=  info.size;
				junkFileList.add(info);
				if (info.isCheck) {
					selectSize += info.size;
				}
			}
		}
		mData.junkList.oldSzie = oldSize;
		mData.junkList.size = apksize + longsize + syslogSize + thumbsize + oldSize;
			
		Resources resources = getActivity().getResources();
		
		if (cacheList.size() > 0) {
			GroupItem groupItem = new GroupItem();
			groupItem.icon = resources.getDrawable(R.drawable.main_ic_net_man);
			groupItem.title = resources.getString(R.string.group_title_cache);
			groupItem.type = GROUP_TYPE_CACHE;
			
			long size = 0;
			int selectCount=0;
			for (APPInfo appInfo : cacheList) {
				size += appInfo.mSizeInfo.mCacheSize;
				size += appInfo.mSizeInfo.mExternalCacheSize;
				selectCount += appInfo.isCacheCheck ? 1 : 0;
			}
			groupItem.size = size;
			groupItem.selectCount = selectCount;
			groupItem.count = cacheList.size();
			
			groupItem.check = (groupItem.count == groupItem.selectCount);
			
			groupList.add(groupItem);
		}
		
		if (dataList.size() > 0) {
			GroupItem groupItem = new GroupItem();
			groupItem.icon = resources.getDrawable(R.drawable.main_ic_net_man);
			groupItem.title = resources.getString(R.string.group_title_data);
			groupItem.type = GROUP_TYPE_DATA;
			
			long size = 0;
			int selectCount=0;
			for (APPInfo appInfo : dataList) {
				size += appInfo.mSizeInfo.mDataSize;
				size += appInfo.mSizeInfo.mExternalDataSize;
				selectCount += appInfo.isCheck ? 1 : 0;
			}
			groupItem.size = size;
			groupItem.selectCount = selectCount;
			groupItem.count = dataList.size();
			groupItem.check = (groupItem.count == groupItem.selectCount);
			groupList.add(groupItem);
		}
		
		if (newAPKList.size() > 0) {
			GroupItem groupItem = new GroupItem();
			groupItem.icon = resources.getDrawable(R.drawable.main_ic_net_man);
			groupItem.title = resources.getString(R.string.group_title_new_apk);
			groupItem.type = GROUP_TYPE_NEWAPK;
			
			long size = 0;
			int selectCount = 0;
			for (ApkInfo appInfo : newAPKList) {
				size += appInfo.size;
				selectCount += appInfo.isCheck ? 1 : 0;
			}
			groupItem.size = size;
			groupItem.selectCount = selectCount;
			groupItem.count = newAPKList.size();
			groupItem.check = (groupItem.count == groupItem.selectCount);
			groupList.add(groupItem);
		}
		
		if (redundantAPKList.size() > 0) {
			GroupItem groupItem = new GroupItem();
			groupItem.icon = resources.getDrawable(R.drawable.main_ic_net_man);
			groupItem.title = resources.getString(R.string.group_title_re_apk);
			groupItem.type = GROUP_TYPE_REDUNDANTAPK;
			
			long size = 0;
			int selectCount = 0;
			for (ApkInfo appInfo : redundantAPKList) {
				size += appInfo.size;
				selectCount += appInfo.isCheck ? 1 : 0;
			}
			groupItem.size = size;
			groupItem.selectCount = selectCount;
			groupItem.count = redundantAPKList.size();
			groupItem.check = (groupItem.count == groupItem.selectCount);
			groupList.add(groupItem);
		}
		
		if (junkFileList.size() > 0) {
			GroupItem groupItem = new GroupItem();
			groupItem.icon = resources.getDrawable(R.drawable.main_ic_net_man);
			groupItem.title = resources.getString(R.string.group_title_junk);
			groupItem.type = GROUP_TYPE_JUNK;
			
			long size = mData.junkList.size - mData.junkList.apkSize;
			
			int selectCount = 0;
			for (FileInfo fileInfo : junkFileList) {
				selectCount += fileInfo.isCheck ? 1 : 0;
			}
			
			groupItem.selectCount = selectCount;
			groupItem.size = size;
			groupItem.count = mData.junkList.mEmptyList.size()+
					mData.junkList.mLogList.size()+
					mData.junkList.mOldList.size()+
					mData.junkList.mSystemLogList.size()+
					mData.junkList.mThumbList.size();
			
			groupItem.check = (groupItem.count == groupItem.selectCount);
			groupList.add(groupItem);
		}
		
		refreshExpandUI(-1);
	}
	
	private void onGroupCheckClick(int groupPosition, boolean check) {
		int type = groupList.get(groupPosition).type;
		
		switch (type) {
		case GROUP_TYPE_CACHE:  {
			for (APPInfo info : cacheList) {
				if (info.isCacheCheck != check) {
					info.isCacheCheck = check;
					
					long size = info.mSizeInfo.mCacheSize + info.mSizeInfo.mExternalCacheSize;
					selectSize += check ? size : -size;
					
					groupList.get(groupPosition).selectCount += check ? 1 : -1;
					
				}
			}
			break;
		}
		case GROUP_TYPE_DATA: {
			for (APPInfo info : dataList) {
				if (info.isCheck != check) {
					info.isCheck = check;
					
					long size = info.mSizeInfo.mDataSize + info.mSizeInfo.mExternalDataSize;
					selectSize += check ? size : -size;
					
					groupList.get(groupPosition).selectCount += check ? 1 : -1;
				}
			}
			break;
		}
		case GROUP_TYPE_NEWAPK: {
			for (ApkInfo info : newAPKList) {
				if (info.isCheck != check) {
					info.isCheck = check;
					
					long size = info.size;
					selectSize += check ? size : -size;
					
					groupList.get(groupPosition).selectCount += check ? 1 : -1;
				}
			}
			break;
		}
		case GROUP_TYPE_REDUNDANTAPK: {
			for (ApkInfo info : redundantAPKList) {
				if (info.isCheck != check) {
					info.isCheck = check;
					
					long size = info.size;
					selectSize += check ? size : -size;
					
					groupList.get(groupPosition).selectCount += check ? 1 : -1;
				}
			}
			break;
		}
		case GROUP_TYPE_JUNK: {
			for (FileInfo info : junkFileList) {
				if (info.isCheck != check) {
					info.isCheck = check;
					
					long size = info.size;
					selectSize += check ? size : -size;
					
					groupList.get(groupPosition).selectCount += check ? 1 : -1;
				}
			}
			break;
		}
		default:
			break;
		}
		
		refreshExpandUI(groupPosition);
	}

	
	private void refreshExpandUI(int item) {
		
		if (item < 0) {
			if (mExpandListView != null) {
				GroupItem groupItem;
				for (int i = 0; i < groupList.size(); i++) {
					groupItem = groupList.get(i);
					groupList.get(i).check = (groupItem.count == groupItem.selectCount);
					if (mExpandListView.isGroupExpanded(i)) {
						mExpandListView.collapseGroup(i);
						mExpandListView.expandGroup(i);
					} else {
						mExpandListView.expandGroup(i);
						mExpandListView.collapseGroup(i);
					}
				}
			}
		} else if (item >=0 && item < groupList.size()){
			GroupItem groupItem;
			groupItem = groupList.get(item);
			groupList.get(item).check = (groupItem.count == groupItem.selectCount);
			
			
			if (mExpandListView.isGroupExpanded(item)) {
				mExpandListView.collapseGroup(item);
				mExpandListView.expandGroup(item);
			} else {
				mExpandListView.expandGroup(item);
				mExpandListView.collapseGroup(item);
			}
		}
		
		selectCount = 0;
		for (GroupItem groupItem : groupList) {
			selectCount += groupItem.selectCount;
		}
		String info;
		if (selectCount == 0) {
			info = getString(R.string.clear_key);
		} else {
			info = getString(R.string.clean_now, Formatter.formatFileSize(mContext, selectSize));
		}
		
		mListener.setCleanButton(info, selectCount == 0 ? false : true);
	}

	@Override
	public void onClearFinished() {
		configureData();
		super.onClearFinished();
	}
}
