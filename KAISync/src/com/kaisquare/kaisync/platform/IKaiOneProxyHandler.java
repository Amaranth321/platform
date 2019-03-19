package com.kaisquare.kaisync.platform;

import org.jboss.netty.channel.Channel;

import com.kaisquare.kaisync.codec.PlatformCommand;

/**
 * KAI One communication handler
 */
public interface IKaiOneProxyHandler {
	
	/**
	 * Received commands from KAI One device
	 * @param command
	 * 
	 */
	void onCommandReceived(Channel channel, PlatformCommand command);

	/**
	 * Received content data after command received
	 * @param channel channel of the connection
	 * @param data received data object
	 */
	void onDataReceived(Channel channel, Object data);
	
	/**
	 * Callback when a channel connected
	 * @param channel
	 */
	void onChannelConnected(Channel channel);

	/**
	 * When connection is closed
	 * @param channel
	 */
	void onChannelClosed(Channel channel);
	
	/**
	 * Callback when an exception occurred on this channel
	 * @param channel
	 * @param cause
	 */
	void onChannelException(Channel channel, Throwable cause);
}
