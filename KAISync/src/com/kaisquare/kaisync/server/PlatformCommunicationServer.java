package com.kaisquare.kaisync.server;

import java.io.InputStream;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;

import com.kaisquare.kaisync.codec.PlatformCommand;
import com.kaisquare.kaisync.file.ChunkedData;
import com.kaisquare.kaisync.platform.IKaiOneProxyHandler;
import com.kaisquare.kaisync.platform.IKaiOneProxyHandlerFactory;
import com.kaisquare.kaisync.transport.ITransportHandlerFactory;
import com.kaisquare.kaisync.transport.ITransportListener;
import com.kaisquare.kaisync.transport.StringSyncPipelineFactory;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.Utils;

public class PlatformCommunicationServer extends NettyServer implements ITransportHandlerFactory {

	private IKaiOneProxyHandlerFactory handlerFactory;
	private String keystore;
	private String keypass;
	
	PlatformCommunicationServer(int port, IKaiOneProxyHandlerFactory factory) {
		super(port);
		
		this.handlerFactory = factory;
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void setKeystore(String keystore, String keypass) {
		this.keystore = keystore;
		this.keypass = keypass;
	}

	@Override
	protected ChannelPipelineFactory createPipelineFactory() {
		StringSyncPipelineFactory factory = new StringSyncPipelineFactory(this, PlatformCommand.class);
		factory.setReadTimeout(mReadTimeout);
		factory.setWriteTimeout(mWriteTimeout);
		if (!Utils.isStringEmpty(keystore))
		    factory.setSslContext(SSLContextProvider.getTrustSslContext(keystore, keypass).createSSLEngine());
		return factory;
	}

	@Override
	public ITransportListener getTransportHandler() {
		return new TransportHandler(handlerFactory.getHandler());
	}
	
	@Override
	public void stop()
	{
		super.stop();
	}
	
	private class TransportHandler implements ITransportListener
	{
		private IKaiOneProxyHandler handler;
		
		public TransportHandler(IKaiOneProxyHandler handler)
		{
			this.handler = handler;
		}

		@Override
		public void onTransportConnected(Channel channel) throws Exception {
			handler.onChannelConnected(channel);
		}
	
		@Override
		public void onTransportReceived(final Channel channel, Object request)
				throws Exception {

			if (request instanceof PlatformCommand)
			{
				PlatformCommand cmd = (PlatformCommand) request;
				handler.onCommandReceived(channel, cmd);
			}
			else if (request instanceof ChunkedData || request instanceof InputStream)
				handler.onDataReceived(channel, request);
			else
			{
				AppLogger.e(this, "unknown request from client " + request);
				channel.close();
			}
		}
	
		@Override
		public void onTransportClosed(Channel channel) throws Exception {
			handler.onChannelClosed(channel);
		}

		@Override
		public void onTransportException(Channel channel, Throwable cause) {
		    handler.onChannelException(channel, cause);
		}
	}
	
}
