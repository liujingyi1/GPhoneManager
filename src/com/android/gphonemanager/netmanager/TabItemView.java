package com.android.gphonemanager.netmanager;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TabItemView extends LinearLayout {

	private ImageView img;
	private TextView text;

	public TabItemView(Context context) {
		super(context);
	}

	public TabItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TabItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

//	@Override
//	protected void onFinishInflate() {
//		super.onFinishInflate();
//
//		img = (ImageView) findViewById(R.id.tab_item_img);
//		text = (TextView) findViewById(R.id.tab_item_text);
//	}

	public void setItemSelected(boolean selected) {
		img.setSelected(selected);
		text.setSelected(selected);
	}

	public void setItemContent(CharSequence tabTitle, int tabImgResId) {
		img.setImageResource(tabImgResId);
		text.setText(tabTitle);
	}

}
