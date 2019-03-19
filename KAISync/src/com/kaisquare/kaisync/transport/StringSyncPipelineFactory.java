package com.kaisquare.kaisync.transport;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;


public class StringSyncPipelineFactory implements ChannelPipelineFactory {
	
	private ITransportHandlerFactory mTransportFactory;
	private Class<? extends StringKeyValueHeader> mClass;
	private String mKeystore;
	private String mKeypass;
	private SSLEngine mSslEngine;
	private int mReadTimeout = 0;
	private int mWriteTimeout = 0;
	
	public StringSyncPipelineFactory(ITransportHandlerFactory factory, Class<? extends StringKeyValueHeader> clazz)
	{
		this(factory, clazz, null, null);
	}
	
	public StringSyncPipelineFactory(ITransportHandlerFactory factory, Class<? extends StringKeyValueHeader> clazz,
			String keystore, String keypass)
	{
		mTransportFactory = factory;
		mClass = clazz;
		mKeystore = keystore;
		mKeypass = keypass;
	}
	
	/**
	 * @param timeout timeout in seconds
	 */
	public void setReadTimeout(int timeout)
	{
		if (timeout > 0)
			mReadTimeout = timeout;
	}

	public void setWriteTimeout(int timeout) {
		if (timeout > 0)
			mWriteTimeout = timeout;
	}
	
	public void setSslContext(SSLEngine sslContext)
	{
		mSslEngine = sslContext;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		
		ChannelPipeline pipeline = Channels.pipeline();
		if (mSslEngine != null)
			pipeline.addLast("ssl", new SslHandler(mSslEngine));
		pipeline.addLast("codec", new StringSyncCodec(mClass));
		pipeline.addLast("streamer", new ChunkedWriteHandler());
//		pipeline.addLast("aggregator", new StreamChunkAggregator());
		if (mReadTimeout > 0)
			pipeline.addLast("readtimeout", new ReadTimeoutHandler(SharedTimer.INSTANCE, mReadTimeout));
		if (mWriteTimeout > 0)
			pipeline.addLast("writetimeout", new WriteTimeoutHandler(SharedTimer.INSTANCE, mWriteTimeout));
		pipeline.addLast("handler", new ExternalTransportHandler(mTransportFactory.getTransportHandler(), mKeystore, mKeypass));
		
		return pipeline;
	}
	
	private static class SharedTimer
	{
		private static final HashedWheelTimer INSTANCE = new HashedWheelTimer();
	}
}
