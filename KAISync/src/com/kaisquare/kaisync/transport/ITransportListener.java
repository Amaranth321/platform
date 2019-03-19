package com.kaisquare.kaisync.transport;

import org.jboss.netty.channel.Channel;

public interface ITransportListener {
	
	void onTransportConnected(Channel channel) throws Exception;
	
	void onTransportReceived(Channel channel, Object response) throws Exception;
	
	void onTransportClosed(Channel channel) throws Exception;
	
	void onTransportException(Channel channel, Throwable cause);

}
