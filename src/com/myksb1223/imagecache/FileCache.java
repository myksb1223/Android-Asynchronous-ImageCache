package com.myksb1223.imagecache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

// Basic concept.
// Write, delete and get are default functions in Cache.  
// And we have to check the size because Cache has limited size.
// Lock is very important.

public class FileCache {
	private static String TAG = "FileCache";
	private int DEFAULT_CACHE_SIZE = 10 * 1024 * 1024;
	private int cacheMaxSize;
	private int nowSize;
	private LinkedHashMap<String, FileInfo> cacheFileDatas;
	private final Object mFileCacheLock = new Object();
//	private OutputStream out;
	private File cacheDir;
	private ImageCacheApplication singleton;
//	String cacheDirName;
	
	public FileCache(Context context, String dirName) {
//		this.cacheDirName = dirName;
		this.cacheMaxSize = DEFAULT_CACHE_SIZE;
		initialized(context, dirName);
	}
	
	public FileCache(Context context, String dirName, int maxSize) {
//		this.cacheDirName = dirName;
		this.cacheMaxSize = maxSize;
		initialized(context, dirName);
	}
	
	public void initialized(Context context, String dirName) {
		cacheFileDatas = new LinkedHashMap<String, FileInfo>();
		cacheDir = checkCacheDirectory(context, dirName);
		new InnerAsyncTask().execute(cacheDir);
		singleton = ImageCacheApplication.getInstance();
	}
	
	class InnerAsyncTask extends AsyncTask<File, Void, Void> {
    @Override
    protected Void doInBackground(File... params) {
        synchronized (mFileCacheLock) {
            File cacheDir = params[0];
            File[] cacheFiles = cacheDir.listFiles();
            
            for(File file : cacheFiles) {
            	putCacheFileToMap(file);
            }            
            
            mFileCacheLock.notifyAll(); 
        }
        return null;
    }		
	}
	
	public File get(String fileName) {
		synchronized (mFileCacheLock) {
			FileInfo fInfo = cacheFileDatas.get(fileName);
			
			if(fInfo == null) {
				return null;				
			}
			
			if(fInfo.file.exists()) {
				moveFileInfoToLast(fileName, fInfo);
				Log.d(TAG, "get file From Cache");
				return fInfo.file;
			}
						
			removeCacheFileFromMap(fileName, fInfo);
			mFileCacheLock.notifyAll();			
		}
		
		return null;
	}
	
	public void write(String defaultPath, String fileName) {		
		synchronized (mFileCacheLock) {
			String newKey = singleton.keyToFilename(fileName);
			copyFile(defaultPath, newKey);
			putCacheFileToMap(new File(newKey));
			checkMaxSize();			
			Log.d(TAG, "Added file To Cache");
			mFileCacheLock.notifyAll();			
		}		
	}
	
	public void delete(String fileName) {
		synchronized (mFileCacheLock) {
			FileInfo fInfo = cacheFileDatas.get(fileName);
			if(fInfo == null) {
				return;
			}
			
			removeCacheFileFromMap(fileName, fInfo);
			fInfo.file.delete();
			
			Log.d(TAG, "Deleted file from Cache");
			mFileCacheLock.notifyAll();			
		}
	}
	
	public void deleteAll() {
		synchronized (mFileCacheLock) {		
			List<String> keys = new ArrayList<String>(cacheFileDatas.keySet());
			for (String key : keys) {
				delete(key);
			}			
		}
	}
	
	public void putCacheFileToMap(File file) {
		cacheFileDatas.put(file.getName(), new FileInfo(file));
		nowSize += (int)file.length();
	}
	
	public void removeCacheFileFromMap(String fileName, FileInfo fileInfo) {
		nowSize -= (int)fileInfo.file.length();
		cacheFileDatas.remove(fileName);
	}
	
	public void moveFileInfoToLast(String fileName, FileInfo fileInfo) {
		cacheFileDatas.remove(fileName);
		cacheFileDatas.put(fileName, fileInfo);
	}
	
	public void checkMaxSize() {
		if(cacheMaxSize < nowSize) {
			LinkedList<Entry<String, FileInfo>> deleteDatas = findDeleteDatas();
			for(Entry<String, FileInfo> entry : deleteDatas) {
				delete(entry.getKey());
			}
		}
	}
	
	public LinkedList<Entry<String, FileInfo>> findDeleteDatas() {
		int deleteSize = 0;
		LinkedList<Entry<String, FileInfo>> deleteDatas = new LinkedList<Entry<String, FileInfo>>();
		for(Entry<String, FileInfo> entry : cacheFileDatas.entrySet()) {
			deleteDatas.add(entry);
			deleteSize += entry.getValue().file.length();
			if (nowSize - deleteSize < cacheMaxSize) {
				break;
			}
		}

		return deleteDatas;
	}
	
	public File checkCacheDirectory(Context context, String dirName) {
		File cacheDir = getDiskCacheDir(context, dirName);
		
		if(!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		
		return cacheDir;
	}
	
  public File getDiskCacheDir(Context context, String uniqueName) {
    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
    // otherwise use internal cache dir
    final String cachePath =
            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                    !isExternalStorageRemovable() ? getExternalCacheDir(context).getPath() :
                            context.getCacheDir().getPath();

    return new File(cachePath + File.separator + uniqueName);
}

  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public boolean isExternalStorageRemovable() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        return Environment.isExternalStorageRemovable();
    }
    return true;
  }

  public File getExternalCacheDir(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
        return context.getExternalCacheDir();
    }

    // Before Froyo we need to construct the external cache dir ourselves
    final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
    return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
  }
  
  public class FileInfo {
  	File file;
  	int size;
  	
  	public FileInfo(File file) {
  		this.file = file;
  		this.size = (int)file.length();
  	}
  }
  
	public void copyFile(String defaultPath, String path) {
		InputStream in = null;
		OutputStream out = null;

    try {
      //create output directory if it doesn't exist      
      
    	String newPath = cacheDir + "/" + path;  		    	
    	
      in = new FileInputStream(defaultPath);        
      out = new FileOutputStream(newPath);

      byte[] buffer = new byte[1024];
      int read;
      while ((read = in.read(buffer)) != -1) {
          out.write(buffer, 0, read);
      }
      in.close();
      in = null;

          // write the output file
      out.flush();
      out.close();
      out = null;

      // delete the original file        
    } 
    catch (FileNotFoundException fnfe1) {
    	Log.e("tag", fnfe1.getMessage());
    }
    catch (Exception e) {
    	Log.e("tag", e.getMessage());
    }		       		
    
    new File(defaultPath).delete();
	}	  
}
