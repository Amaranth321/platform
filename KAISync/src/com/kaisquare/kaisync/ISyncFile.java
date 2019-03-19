package com.kaisquare.kaisync;


/**
 * The file information on the server
 */
public interface ISyncFile {
	
	/**
	 * Encrypt data during file transfer
	 * @param keystore path to keystore
	 * @param keypass password of keystore
	 */
	void setKeystore(String keystore, String keypass);
	
	/**
	 * Get the identifier of the file
	 * @return identifier of the file
	 */
	String getID();
}
