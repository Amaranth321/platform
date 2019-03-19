package com.kaisquare.kaisync.platform;

import com.kaisquare.kaisync.ISyncReadFile;

/**
 * Software update check for client side
 */
public interface ISoftwareUpdateSync {
	
	/**
	 * Get the latest version
	 * @return version
	 */
	String getLatestVersion();

	/**
	 * Get the latest update file
	 * @return update file
	 */
	ISyncReadFile getLatestUpdateFile();
	
	/**
	 * Close connection 
	 */
	void close();
}
