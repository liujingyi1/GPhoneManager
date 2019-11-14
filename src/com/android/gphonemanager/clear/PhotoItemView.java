package com.android.gphonemanager.clear;


import java.text.AttributedCharacterIterator.Attribute;
import java.util.jar.Attributes;

import com.android.gphonemanager.clear.PhotoFragment.PhotoClickListener;
import com.android.gphonemanager.clear.ScanType.FileInfo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.gphonemanager.R;

public class PhotoItemView extends FrameLayout {
    protected  PhotoClickListener mListener;
    private int mColumnCount;

    private ImageView imageView;
    private ImageView imageViewFrame;
    private CheckBox checkBox;
    FileInfo mFileInfo;
    private boolean isSelect = false;
    
    FileIconHelper mFileIconHelper;

    public PhotoItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setIconHelper(FileIconHelper fileIconHelper) {
    	this.mFileIconHelper = fileIconHelper;
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        imageView = (ImageView)findViewById(R.id.image_view);
        imageViewFrame = (ImageView)findViewById(R.id.image_view_frame);
        checkBox =  (CheckBox)findViewById(R.id.checkbox);
        
        OnClickListener listener = createClickListener();
        setOnClickListener(listener);
        checkBox.setOnClickListener(listener);
    }

    public void loadData(FileInfo fileinfo) {
        this.mFileInfo = fileinfo;
        
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)this.getLayoutParams();
        
        mFileIconHelper.setIcon(fileinfo, imageView, checkBox, params.width, params.height);
        checkBox.setChecked(fileinfo.isCheck);
//        isSelect = presetCityInfo.isSelect() > 0;
        setSelected(isSelect);
    }

    protected OnClickListener createClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
            	
            	if (mListener == null) {
            		return;
            	} else {
                	switch (v.getId()) {
    				case R.id.checkbox:
    					mListener.onPhotoSelected(mFileInfo, checkBox.isChecked());
    					break;

    				default:
    					mListener.onPhotoClick(mFileInfo);
    					break;
    				}
            	}
            }
        };
    }

    public void setListener(PhotoClickListener listener){
        mListener = listener;
    }

    public interface Listener {
        void onCitySelected(FileInfo ci);
    }
}
