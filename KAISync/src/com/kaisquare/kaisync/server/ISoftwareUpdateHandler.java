package com.kaisquare.kaisync.server;

import com.kaisquare.kaisync.file.IServerSyncFile;
import com.kaisquare.kaisync.platform.DeviceType;

/**
 * Server side software update handler
 */
public interface ISoftwareUpdateHandler {
	
	/**
	 * Get the latest version
	 * @param deviceType the device type
	 * @param version speicfy the version of client
	 * @return version
	 */
	String getLatestVersion(DeviceType deviceType, double version);
	
	/**
	 * Get the latest update file
	 * @param deviceType the device type
	 * @param version speicfy the version of client
	 * @return update file description
	 */
	IServerSyncFile getLatestUpdateFile(DeviceType deviceType, double version);
	
	
}
