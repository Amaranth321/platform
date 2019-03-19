package com.kaisquare.kaisync.platform;

import com.kaisquare.kaisync.ISyncFile;
import com.kaisquare.kaisync.ISyncReadFile;

public interface UpdateFileInfo {
	
	/**
	 * Get update file version
	 * @return the version number
	 */
	public String getVersion();
	
	/**
	 * Get model type of the update file
	 * @return
	 */
	public String getModel();
	
	/**
	 * Get update file
	 * @return {@link ISyncFile}
	 */
	public ISyncReadFile getFile();

}
