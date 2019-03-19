package com.kaisquare.kaisync.platform;

import com.kaisquare.kaisync.file.IServerSyncFile;

public interface IServerUpdateFileInfo {
	
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
	public IServerSyncFile getFile();

}
