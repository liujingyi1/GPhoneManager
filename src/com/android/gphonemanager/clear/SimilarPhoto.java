package com.android.gphonemanager.clear;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.android.gphonemanager.clear.ScanType.FileInfo;

public class SimilarPhoto {

    private static final String TAG = SimilarPhoto.class.getSimpleName();
    
    class PhotoItem {
    	int index;
    	long finger;
    }

    public static List<FileInfo> find(Context context, List<FileInfo> photos) {
//        calculateFingerPrint(context, photos);

        List<FileInfo> groups = new ArrayList<FileInfo>();
        List<FileInfo> temp = new ArrayList<>();

        for (int i = 0; i < photos.size(); i++) {
        	FileInfo photo = photos.get(i);

        	temp.clear();
            temp.add(photo);

            for (int j = i + 1; j < photos.size(); j++) {

            	FileInfo photo2 = photos.get(j);

                int dist = hamDist(photo.finger, photo2.finger);

                if (dist < 8) {
                    temp.add(photo2);
                    photos.remove(photo2);
                    j--;
                }
            }
            
            if (temp.size() > 1) {
            	for (FileInfo file : temp) {
            		file.tag = String.valueOf(i);
            		groups.add(file);
            	}
            }

        }
        photos.clear();
        return groups;
    }

    public static void calculateFingerPrint(Context context, List<FileInfo> photos) {
        float scale_width, scale_height;

        for (FileInfo p : photos) {
            Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), p.dbId, MediaStore.Images.Thumbnails.MICRO_KIND, null);
            if (bitmap == null) {
            	continue;
            }
            scale_width = 8.0f / bitmap.getWidth();
            scale_height = 8.0f / bitmap.getHeight();
            Matrix matrix = new Matrix();
            matrix.postScale(scale_width, scale_height);

            Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            p.finger = getFingerPrint(scaledBitmap);
//            p.setFinger(getFingerPrint(scaledBitmap));

            bitmap.recycle();
            scaledBitmap.recycle();
        }
    }

    private static long getFingerPrint(Bitmap bitmap) {
        double[][] grayPixels = getGrayPixels(bitmap);
        double grayAvg = getGrayAvg(grayPixels);
        return getFingerPrint(grayPixels, grayAvg);
    }

    private static long getFingerPrint(double[][] pixels, double avg) {
        int width = pixels[0].length;
        int height = pixels.length;

        byte[] bytes = new byte[height * width];

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (pixels[i][j] >= avg) {
                    bytes[i * height + j] = 1;
                    stringBuilder.append("1");
                } else {
                    bytes[i * height + j] = 0;
                    stringBuilder.append("0");
                }
            }
        }

        Log.d(TAG, "getFingerPrint: " + stringBuilder.toString());

        long fingerprint1 = 0;
        long fingerprint2 = 0;
        for (int i = 0; i < 64; i++) {
            if (i < 32) {
                fingerprint1 += (bytes[63 - i] << i);
            } else {
                fingerprint2 += (bytes[63 - i] << (i - 31));
            }
        }

        return (fingerprint2 << 32) + fingerprint1;
    }

    private static double getGrayAvg(double[][] pixels) {
        int width = pixels[0].length;
        int height = pixels.length;
        int count = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                count += pixels[i][j];
            }
        }
        return count / (width * height);
    }


    private static double[][] getGrayPixels(Bitmap bitmap) {
        int width = 8;
        int height = 8;
        double[][] pixels = new double[height][width];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pixels[i][j] = computeGrayValue(bitmap.getPixel(i, j));
            }
        }
        return pixels;
    }

    private static double computeGrayValue(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = (pixel) & 255;
        return 0.3 * red + 0.59 * green + 0.11 * blue;
    }

    private static int hamDist(long finger1, long finger2) {
        int dist = 0;
        long result = finger1 ^ finger2;
        while (result != 0) {
            ++dist;
            result &= result - 1;
        }
        return dist;
    }
}
