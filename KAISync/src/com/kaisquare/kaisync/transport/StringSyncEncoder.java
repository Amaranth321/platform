package com.kaisquare.kaisync.transport;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

public class StringSyncEncoder extends OneToOneEncoder {
	
	private static StringSyncEncoder mInstance;

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		
		if (msg instanceof StringKeyValueHeader)
			return ChannelBuffers.wrappedBuffer(((StringKeyValueHeader)msg).toString().getBytes());
		
		return msg;
	}
	
	public static synchronized StringSyncEncoder getEncoder()
	{
		if (mInstance == null)
			mInstance = new StringSyncEncoder();
		
		return mInstance;
	}

}
