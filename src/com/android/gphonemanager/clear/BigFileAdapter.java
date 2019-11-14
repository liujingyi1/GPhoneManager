package com.android.gphonemanager.clear;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.gphonemanager.R;


public class BigFileAdapter extends BaseAdapter {
	private ArrayList<File> file_adap;
	public LayoutInflater inflater;
	private int id;
	public static Map<Integer, Boolean> isSelected;

	public BigFileAdapter(Context context, ArrayList<File> file_adap) {
		this.file_adap = file_adap;
		inflater = LayoutInflater.from(context);
		init();
	}

	private void init() {
		isSelected = new HashMap<Integer, Boolean>();
		for (int i = 0; i < file_adap.size(); i++) {
			isSelected.put(i, false);
		}
	}

	@Override
	public int getCount() {

		return file_adap.size();
	}

	@Override
	public Object getItem(int position) {

		return position;
	}

	@Override
	public long getItemId(int position) {

		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;

		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.scandemo, null);
			holder.name = (TextView) convertView.findViewById(R.id.text_name);
			holder.size = (TextView) convertView.findViewById(R.id.text_size);
			holder.type = (TextView) convertView.findViewById(R.id.text_type);
			holder.checkbox = (CheckBox) convertView
					.findViewById(R.id.checkBox);
			holder.photo = (ImageView) convertView
					.findViewById(R.id.image_photo);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();

		}

		holder.name.setText(file_adap.get(position).getName());
		int s = MyUtil.FileType(file_adap.get(position).getName());
		if (s == (R.string.video)) {
			id = R.drawable.clear_video;
		} else if (s == (R.string.music)) {
			id = R.drawable.clear_music;
		} else if (s == (R.string.other)) {
			id = R.drawable.clear_other;
		} else if (s == (R.string.file)) {
			id = R.drawable.clear_doc;
		} else if (s == (R.string.compressed)) {
			id = R.drawable.clear_cul;
		}
		holder.photo.setImageResource(id);
		holder.size.setText(MyUtil.FileSize((double) file_adap.get(position)
				.length()));

		holder.checkbox.setChecked(isSelected.get(position));

		holder.type.setText(MyUtil.FileType(file_adap.get(position).getName()));
		return convertView;
	}

	public final class ViewHolder {
		public ImageView photo;
		public CheckBox checkbox;
		public TextView name, size, type;
	};

}
