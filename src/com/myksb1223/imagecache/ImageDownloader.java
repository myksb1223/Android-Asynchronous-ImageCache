package com.myksb1223.imagecache;

import android.widget.ImageView;

public class ImageDownloader {
	private ImageCacheApplication singleton;
	
	public ImageDownloader() {
		this.singleton = ImageCacheApplication.getInstance();
	}
	
	public void download(ImageView imageView, int position) {
		
		ImageAsyncTask task = singleton.imageTaskCache.get(imageView);
		if(task != null) {			
				task.cancel(true);
				singleton.imageTaskCache.remove(imageView);
				imageView.setImageBitmap(null);
				task = null;
		}
		
		task = new ImageAsyncTask();
		task.download(imageView, position);				
		singleton.imageTaskCache.put(imageView, task);			
		
	}
}
