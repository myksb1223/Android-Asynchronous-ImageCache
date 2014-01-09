package com.myksb1223.imagecache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ImageCustomAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private ImageCacheApplication singleton;
	private ImageDownloader downloader;
	
	public ImageCustomAdapter(Context cfx) {
		this.singleton = ImageCacheApplication.getInstance();
  	this.mInflater = LayoutInflater.from(cfx);
		this.downloader = new ImageDownloader();
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return singleton.urls.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return singleton.urls.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder holder = null;		 
	
		if(convertView == null) {
			holder = new ViewHolder();
			 
			convertView = mInflater.inflate(R.layout.image_row_data, null);
			holder.imageView = (ImageView)convertView.findViewById(R.id.imageView);
			holder.rootLayout = (LinearLayout)convertView.findViewById(R.id.rootLayout);
			holder.rootLayout.setLayoutParams(new GridView.LayoutParams(singleton.widthPixel, singleton.widthPixel));
					 
			convertView.setTag(holder);			 
		}
		else {
			holder = (ViewHolder)convertView.getTag();
		}
		
		downloader.download(holder.imageView, position);
		
		return convertView;
	}

	class ViewHolder {	
		LinearLayout rootLayout;
		ImageView imageView; 
	}  
}
