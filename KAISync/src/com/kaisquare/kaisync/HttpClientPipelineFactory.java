package com.kaisquare.kaisync;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

public class HttpClientPipelineFactory implements ChannelPipelineFactory {
	
	private ChannelHandler handler;
	
	public HttpClientPipelineFactory(ChannelHandler handler)
	{
		this.handler = handler;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = pipeline();
		pipeline.addLast("codec", new HttpClientCodec());
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
        pipeline.addLast("handler", handler);
        return pipeline;
	}

}
