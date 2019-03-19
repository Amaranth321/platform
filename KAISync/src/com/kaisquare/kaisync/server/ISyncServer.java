package com.kaisquare.kaisync.server;

public interface ISyncServer {
	
	/**
	 * Name of the sync server
	 * @return
	 */
	String getName();
	
	/**
	 * Get the port which the server bound to
	 * @return
	 */
	int getPort();
	
	/**
	 * Set keystore and password for using ssl connection 
	 * @param keystore path to keystore file
	 * @param keypass the password of keystore
	 */
	void setKeystore(String keystore, String keypass);
	
	/**
	 * Set read timeout for connection
	 * @param timeout timeout in seconds
	 */
	void setReadTimeout(int timeout);
	
	/**
	 * Set write timeout for connection
	 * @param timeout timeout in seconds
	 */
	void setWriteTimeout(int timeout);
	
	/**
	 * Start server
	 */
	void start();
	
	/**
	 * Stop the server
	 */
	void stop();

}
