package com.android.gphonemanager.clear;

import java.util.HashMap;

import com.android.gphonemanager.clear.FileIconLoader.IconLoadFinishListener;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import com.android.gphonemanager.clear.FileCategoryHelper.FileCategory;
import com.android.gphonemanager.clear.ScanType.FileInfo;
import com.android.gphonemanager.R;

public class FileIconHelper implements IconLoadFinishListener {

    private static final String LOG_TAG = "FileIconHelper";

    private static HashMap<ImageView, View> imageFrames = new HashMap<ImageView, View>();

    private static HashMap<String, Integer> fileExtToIcons = new HashMap<String, Integer>();

    private FileIconLoader mIconLoader;

    static {
        addItem(new String[] {
            "mp3", "ogg"
        }, R.drawable.file_icon_mp3);
        addItem(new String[] {
            "wma"
        }, R.drawable.file_icon_wma);
        addItem(new String[] {
            "wav"
        }, R.drawable.file_icon_wav);
        addItem(new String[] {
            "mid"
        }, R.drawable.file_icon_mid);
        addItem(new String[] {
                "mp4", "wmv", "mpeg", "m4v", "3gp", "3gpp", "3g2", "3gpp2", "asf"
        }, R.drawable.file_icon_video);
        addItem(new String[] {
                "jpg", "jpeg", "gif", "png", "bmp", "wbmp"
        }, R.drawable.file_icon_picture);
        addItem(new String[] {
                "txt", "log", "xml", "ini", "lrc"
        }, R.drawable.file_icon_txt);
        addItem(new String[] {
                "doc", "ppt", "docx", "pptx", "xsl", "xslx",
        }, R.drawable.file_icon_office);
        addItem(new String[] {
            "pdf"
        }, R.drawable.file_icon_pdf);
        addItem(new String[] {
            "zip"
        }, R.drawable.file_icon_zip);
        addItem(new String[] {
            "mtz"
        }, R.drawable.file_icon_theme);
        addItem(new String[] {
            "rar"
        }, R.drawable.file_icon_rar);
    }

    public FileIconHelper(Context context) {
        mIconLoader = new FileIconLoader(context, this);
    }

    private static void addItem(String[] exts, int resId) {
        if (exts != null) {
            for (String ext : exts) {
                fileExtToIcons.put(ext.toLowerCase(), resId);
            }
        }
    }

    public static int getFileIcon(String ext) {
        Integer i = fileExtToIcons.get(ext.toLowerCase());
        if (i != null) {
            return i.intValue();
        } else {
            return R.drawable.file_icon_default;
        }

    }

    public void setIcon(FileInfo fileInfo, ImageView fileImage, View fileImageFrame) {
        String filePath = fileInfo.path;
        long fileId = fileInfo.dbId;
        String extFromFilename = getExtFromFilename(filePath);
        FileCategory fc = FileCategoryHelper.getCategoryFromPath(filePath);
        fileImageFrame.setVisibility(View.GONE);
        boolean set = false;
        int id = getFileIcon(extFromFilename);
        fileImage.setImageResource(id);

        mIconLoader.cancelRequest(fileImage);
        switch (fc) {
            case Apk:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
                break;
            case Picture:
            case Video:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
                if (set)
                    fileImageFrame.setVisibility(View.VISIBLE);
                else {
                    fileImage.setImageResource(fc == FileCategory.Picture ? R.drawable.file_icon_picture
                            : R.drawable.file_icon_video);
                    imageFrames.put(fileImage, fileImageFrame);
                    set = true;
                }
                break;
            default:
                set = true;
                break;
        }

        if (!set)
            fileImage.setImageResource(R.drawable.file_icon_default);
    }
    
    //add liujingyi start
    public void setIcon(FileInfo fileInfo, ImageView fileImage, View fileImageFrame, int width, int height) {
        String filePath = fileInfo.path;
        long fileId = fileInfo.dbId;
        String extFromFilename = getExtFromFilename(filePath);
        FileCategory fc = FileCategoryHelper.getCategoryFromPath(filePath);
        fileImageFrame.setVisibility(View.GONE);
        boolean set = false;
        int id = getFileIcon(extFromFilename);
        fileImage.setImageResource(id);

        mIconLoader.cancelRequest(fileImage);
        switch (fc) {
            case Apk:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
                break;
            case Picture:
            case Video:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc, width, height);
                if (set)
                    fileImageFrame.setVisibility(View.VISIBLE);
                else {
                    fileImage.setImageResource(fc == FileCategory.Picture ? R.drawable.file_icon_picture
                            : R.drawable.file_icon_video);
                    imageFrames.put(fileImage, fileImageFrame);
                    set = true;
                }
                break;
            default:
                set = true;
                break;
        }

        if (!set)
            fileImage.setImageResource(R.drawable.file_icon_default);
    }
    //add liujingyi end

    @Override
    public void onIconLoadFinished(ImageView view) {
        View frame = imageFrames.get(view);
        if (frame != null) {
            frame.setVisibility(View.VISIBLE);
            imageFrames.remove(view);
        }
    }

    public static String getExtFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }
}
