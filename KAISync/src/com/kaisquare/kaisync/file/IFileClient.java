package com.kaisquare.kaisync.file;

import com.kaisquare.kaisync.ISyncFile;

/**
 * This interface defines methods for accessing files on client side 
 */
public interface IFileClient {
	
	/**
	 * Open file on file server
	 * @param identifier the unique id for writing or reading
	 * @param options the file action, refer to {@link FileOptions}
	 * @return the id for opened file
	 */
	ISyncFile openFile(String identifier, FileOptions options);
	
	/**
	 * Open file on file server
	 * @param identifier the unique id for writing or reading
	 * @param options the file action, refer to {@link FileOptions}
	 * @param keystore path to keystore
	 * @param keypass password of keystore
	 * 
	 * @return the id for opened file
	 */
	ISyncFile openFile(String identifier, FileOptions options, String keystore, String keypass);
	
	/**
	 * Delete an existing file
	 * @param identifier the id of existing file
	 * @return
	 */
	boolean deleteFile(String identifier);
}
