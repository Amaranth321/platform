package com.kaisquare.kaisync.server;

import java.net.InetSocketAddress;

import com.kaisquare.kaisync.IClientChannel;

class ClientChannel implements IClientChannel {
	
	private InetSocketAddress mAddress;

	public void setAddress(InetSocketAddress address)
	{
		mAddress = address;
	}

	@Override
	public InetSocketAddress getAddress() {
		return mAddress;
	}

}
