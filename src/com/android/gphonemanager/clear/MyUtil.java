package com.android.gphonemanager.clear;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import com.android.gphonemanager.R;
import com.android.gphonemanager.clear.ScanType.FileInfo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;


public class MyUtil {
	public static int FileType(String str) {
		int s;
		if (str.endsWith(".mp4") || str.endsWith(".avi")
				|| str.endsWith(".rmvb")) {
			s = R.string.video;
		} else if (str.endsWith(".jpg") || str.endsWith(".png")) {
			s = R.string.picture;
		} else if (str.endsWith(".mp3") || str.endsWith(".wma")) {
			s = R.string.music;
		} else if (str.endsWith(".txt") || str.endsWith("ASCll")
				|| str.endsWith("MIME")) {
			s = R.string.file;
		} else if (str.endsWith(".rar") || str.endsWith(".zip")) {
			s = R.string.compressed;
		} else if (str.endsWith(".txt") || str.endsWith(".doc")) {
			s = R.string.file;
		} else {
			s = R.string.other;
		}
		return s;
	}

	public static String FileSize(double size) {
		double s = size / 1024 / 1024;
		DecimalFormat df = new DecimalFormat("0.00");
		String num = df.format(s);
		return num + " M  ";
	}
	
	//liujingyi start
	public static String formatFileSize(long fileSize) {
		if (fileSize <= 0) {
			return "0";
		} else if (fileSize < 1024) {
			return fileSize + "B";
		} else if (fileSize >= 1024 && fileSize < 1024 * 1024) {
			return fileSize / 1024 + "K";
		} else {
			double s = fileSize / 1024 / 1024;
			DecimalFormat df = new DecimalFormat("0.00");
			String num = df.format(s);
			return num + " M  ";
		}
		
	}
	//liujingyi end
	
    public static String formatDateString(Context context, long time) {
        DateFormat dateFormat = android.text.format.DateFormat
                .getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat
                .getTimeFormat(context);
        Date date = new Date(time);
        return dateFormat.format(date);// + " " + timeFormat.format(date);
    }
    
    public static void OpenFile(Context context, FileInfo fileInfo) {
		String type = MimeUtils.getMimeType(fileInfo.path);
		
		File file = new File(fileInfo.path);
		Uri uri = FileProvider.getUriForFile(context, "com.android.gphonemanager", file);
		
		try {
	        Intent intent = new Intent();
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        intent.setAction(android.content.Intent.ACTION_VIEW);
	        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	        intent.setDataAndType(uri, type);
	        context.startActivity(intent);
		} catch (Exception e) {
			Toast.makeText(context, R.string.no_activity_found, Toast.LENGTH_SHORT).show();
		}

    }

}
