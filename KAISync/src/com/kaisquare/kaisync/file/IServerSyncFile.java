package com.kaisquare.kaisync.file;

import java.util.Date;

/**
 * The interface describes which server the file is located on, it should have basis information like file size, created date
 */
public interface IServerSyncFile {
	
	/**
	 * File ID on File Transfer Server
	 * @return
	 */
	String getID();
	
	/**
	 * File Transfer Server host
	 * @return
	 */
	String getHost();
	
	/**
	 * The port of File Transfer Server
	 * @return
	 */
	int getPort();
	
	/**
	 * File size
	 * @return
	 */
	long getSize();
	
	/**
	 * File created (uploaded) date (in UTC timezone)
	 * @return
	 */
	Date getCreatedDate(); 

	/**
	 * Get file hash string
	 * @return hash string
	 */
	String getHash();
}
