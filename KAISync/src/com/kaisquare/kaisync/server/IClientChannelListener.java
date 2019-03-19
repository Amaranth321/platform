package com.kaisquare.kaisync.server;

import com.kaisquare.kaisync.IClientChannel;

public interface IClientChannelListener {
	
	boolean channelRequest(IClientChannel channel);

}
