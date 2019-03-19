package com.kaisquare.kaisync;

import java.net.InetSocketAddress;

/**
 * A IClientChannel is used to hold client's socket channel
 */
public interface IClientChannel {
	
	/**
	 * Get client remote address
	 * @return
	 */
	InetSocketAddress getAddress();

}
