package com.kaisquare.kaisync.platform;

import java.util.concurrent.TimeoutException;

public interface CommandClient {
	
	/**
	 * Start command receiving
	 */
	void start();
	
	/**
	 * wait for client is ready to connect with server
	 * @param timeout timeout in milliseconds, pass 0 as infinite waiting
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 */
	void awaitReady(long timeout) throws InterruptedException, TimeoutException;
	
	/**
	 * Stop and close connection for command receiving
	 */
	void close();

}
