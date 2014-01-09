package com.myksb1223.imagecache;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

//
// This class is container to access memory and disk cache.
// To access cache, you have to use this container.
//

public class CacheContainer {
	private static String TAG = "CacheContainer";
	private static LruCache<String, Bitmap> mMemoryCache;
	private static FileCache mFileCache;	
	private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 5 * 1024; // 5MB
	private static String DEFAULT_CACHE_DIR = "httpBitmap";		
			
	public CacheContainer(Context context) {
    mFileCache = new FileCache(context, DEFAULT_CACHE_DIR);
		mMemoryCache = new LruCache<String, Bitmap>(DEFAULT_MEM_CACHE_SIZE);				
	}	
	
	public CacheContainer(Context context, String dir, int mSize) {
    mFileCache = new FileCache(context, dir);				
		mMemoryCache = new LruCache<String, Bitmap>(mSize);				
	}
	
	public CacheContainer(Context context, String dir, int mSize, int fSize) {
    mFileCache = new FileCache(context, dir, fSize);
		mMemoryCache = new LruCache<String, Bitmap>(mSize);				
	}
	
	public static void putMemory(String key, Object value) {
		mMemoryCache.put(key, (Bitmap)value);	
	}
	
	public static void putDisk(String key, String path) {
		mFileCache.write(path, key);
	}
	
	public static LruCache<String, Bitmap> getMemoryCache() {
		return mMemoryCache;
	}
	
	public static FileCache getDiskCache() {
		return mFileCache;
	}
	
	public static Object getMemory(String key) {
		if(mMemoryCache.get(key) != null) {
			return mMemoryCache.get(key);
		}
		
		return null;
	}
	
	public static Object getDisk(String key) {
		if(mFileCache.get(key) != null) {
			File cachedFile = mFileCache.get(key);					
			return ImageAsyncTask.showImage(cachedFile.getAbsolutePath());
		}
		
		return null;
	}
	
	public void clear() {
		mMemoryCache.evictAll();
		mFileCache.deleteAll();
	}
	
	public void clearDiskCache() {
		mFileCache.deleteAll();
	}
	
	public void clearMemoryCache() {
		mMemoryCache.evictAll();
	}
}
