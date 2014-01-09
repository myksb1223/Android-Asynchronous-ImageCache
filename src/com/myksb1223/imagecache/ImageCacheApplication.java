package com.myksb1223.imagecache;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import android.app.Application;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.myksb1223.imagecache.ImageAsyncTask.BitmapDownloaderTask;

public class ImageCacheApplication extends Application {
	private static ImageCacheApplication singleton;
	protected DisplayMetrics metrics;
	protected int widthSize;		
	protected int widthPixel;
	protected float pixelRate;
	protected String directory;
	protected LinkedList<String> urls;	
	protected LinkedHashMap<String, BitmapDownloaderTask> taskCache;
	protected LinkedHashMap<ImageView, ImageAsyncTask> imageTaskCache;
		
	//
	// This class is to use singleton pattern.
	//
	
	public static ImageCacheApplication getInstance() {
    return singleton;
	}	
	
	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		singleton.initializeInstance();
	}
	
	protected void initializeInstance() {
		metrics = getApplicationContext().getResources().getDisplayMetrics();
    pixelRate = (float) (metrics.densityDpi / 160.0);      
    widthSize = (metrics.widthPixels * 160 / metrics.densityDpi - 24)/2;
    widthPixel = (int)(widthSize * pixelRate);
		
		directory = getFilesDir().getAbsolutePath();
		taskCache = new LinkedHashMap<String, BitmapDownloaderTask>();
		imageTaskCache = new LinkedHashMap<ImageView, ImageAsyncTask>();
		urls = new LinkedList<String>();
		
    File file = new File(directory);
    if(!file.exists()) {
    	file.mkdir();
    }          		
	}
	
	// This method replaces url to filename.
	// Url has special characters, so We have to change these characters
	// to save bitmap file into device cache directory. 
	
	public String keyToFilename(String key) {
		String filename = key.replace(":", "_");
		filename = filename.replace("/", "_s_");
		filename = filename.replace("\\", "_bs_");
		filename = filename.replace("&", "_bs_");
		filename = filename.replace("*", "_start_");
		filename = filename.replace("?", "_q_");
		filename = filename.replace("|", "_or_");
		filename = filename.replace(">", "_gt_");
		filename = filename.replace("<", "_lt_");
		return filename;
	}	
	
}
