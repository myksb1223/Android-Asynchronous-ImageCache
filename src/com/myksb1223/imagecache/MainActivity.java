package com.myksb1223.imagecache;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.ImageView;

import com.myksb1223.imagecache.ImageAsyncTask.BitmapDownloaderTask;

public class MainActivity extends Activity {
	private ImageCacheApplication singleton;
	private GridView gridView;	
	private ImageCustomAdapter mAdapter;
	private CacheContainer cacheContainer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		singleton = ImageCacheApplication.getInstance();
		
		cacheContainer = new CacheContainer(this);
		mAdapter = new ImageCustomAdapter(this);
		
		gridView = (GridView)findViewById(R.id.gridView);
		gridView.setAdapter(mAdapter);
		
		loadingThread mThread = new loadingThread("http://koding.classup.co/sessions/make_url");
		mThread.start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    	case R.id.clear_memCache:
	    		cacheContainer.clearMemoryCache();
	    		break;
	    	case R.id.clear_diskCache:
	    		cacheContainer.clearDiskCache();
	        break;
	    	case R.id.clear_all:
	    		cacheContainer.clear();
	    		break;
	    }
  		mAdapter.notifyDataSetChanged();	    
	    return true;	    
	}

	class loadingThread extends Thread {
		String mAddr, receiveString;
		boolean isFail;
		
		public loadingThread(String addr) {
			this.mAddr = addr;
			this.receiveString = null;
			this.isFail = false;
		}
		
		public void run() {
  		StringBuilder html = new StringBuilder();
  		try {
  			System.setProperty("http.keepAlive", "false");  			
  			URL url = new URL(mAddr);
  			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
  			
  			if(conn != null) {
  				conn.setRequestMethod("GET");
  				conn.setConnectTimeout(1000);
  				conn.setUseCaches(false);
  				
  				if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
  					BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
  					for(;;) {
  						String line = br.readLine();
  						if(line == null) {
  							break;
  						}						
  						html.append(line + '\n');						
  					}
  					
  					br.close();
  					receiveString = html.toString();
  				}
  				
  				conn.disconnect();
  			}
  		}
  		catch(Exception ex) {
  			isFail = true; 			
  		}
			
  		int failConnection = 0;
  		if(isFail) {
  			failConnection = 1;
  		}
  		
  		Message msg = new Message();
  		msg.arg1 = failConnection;
  		msg.obj = receiveString;
  		
  		mAfterLoading.sendMessage(msg);
  		
		}
	}
	
	InnerHandler mAfterLoading = new InnerHandler(this);	
	
	static class InnerHandler extends Handler {
		private final WeakReference<MainActivity> mActivity;
		private String receiveString;
		
		public InnerHandler(MainActivity activity) {
			mActivity = new WeakReference<MainActivity>(activity);
		}
		
  	@Override
  	public void handleMessage(Message msg) {
  		MainActivity activity = mActivity.get();
  		receiveString = (String)msg.obj;
  		
  		if(msg.arg1 == 1) {
  			Log.d("MainActivity", "Fail connection");
  		}  		
  		
  		try {
				JSONObject values = new JSONObject(receiveString);
				HashMap<String, Object> data = (HashMap<String, Object>)JSONHelper.toMap(values);
				
				for(int i=0; i<data.size(); i++) {
					activity.singleton.urls.add((String)data.get(Integer.toString(i)));
				}

				activity.mAdapter.notifyDataSetChanged();
			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
  	}
	}
	  
	public void stopTask() {
		for(Entry<ImageView, ImageAsyncTask> entry : singleton.imageTaskCache.entrySet()) {
			ImageAsyncTask task = entry.getValue();
			task.cancel(true);
		}
		
		for(Entry<String, BitmapDownloaderTask> entry : singleton.taskCache.entrySet()) {
			BitmapDownloaderTask task = entry.getValue();
			task.cancel(true);
		}
	}
	
	public void onBackPressed() {
		singleton.urls.clear();
//		cacheContainer.clear();
		stopTask();
		singleton.imageTaskCache.clear();		
		singleton.taskCache.clear();		
		finish();
	}
}
