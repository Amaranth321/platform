package com.kaisquare.kaisync.transport;

import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;

public class StringSyncCodec implements ChannelUpstreamHandler, ChannelDownstreamHandler {
	
	private StringSyncEncoder encoder = StringSyncEncoder.getEncoder();
	private StringSyncDecoder decoder;
	
	public StringSyncCodec(Class<? extends StringKeyValueHeader> clazz)
	{
		decoder = new StringSyncDecoder(clazz);
	}

	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		encoder.handleDownstream(ctx, e);
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		decoder.handleUpstream(ctx, e);
	}
	
	public StringSyncDecoder getDecoder()
	{
		return decoder;
	}

}
