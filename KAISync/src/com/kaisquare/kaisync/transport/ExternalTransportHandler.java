package com.kaisquare.kaisync.transport;

import java.nio.channels.ClosedChannelException;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.ssl.SslHandler;

import com.kaisquare.kaisync.server.SSLContextProvider;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.Utils;

public class ExternalTransportHandler extends SimpleChannelUpstreamHandler {

	private ITransportListener mTransportListener;
	private String mKeystore;
	private String mKeypass;
	
	public ExternalTransportHandler(ITransportListener listener) {
		this(listener, null, null);
	}

	public ExternalTransportHandler(ITransportListener listener, String keystore, String keypass) {
		mTransportListener = listener;
		setKeystore(keystore, keypass);
	}
	
	public void setKeystore(String keystore, String keypass)
	{
		mKeystore = keystore;
		mKeypass = keypass;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		
		if (e.getMessage() instanceof StringKeyValueHeader && ctx.getPipeline().get("ssl") == null)
		{
			StringKeyValueHeader header = (StringKeyValueHeader) e.getMessage();
			if ("1".equalsIgnoreCase(header.get("_ssl")))
			{
				if (!Utils.isStringEmpty(mKeystore))
				{
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("_ssl_ok", "1");
					e.getChannel().write(new StringKeyValueHeader(map)).addListener(new ChannelFutureListener() {
						
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							AppLogger.d(this, "using SSL");
							SSLContext sslContext = SSLContextProvider.getSslContext(mKeystore, mKeypass);
							SSLEngine engine = sslContext.createSSLEngine();
							engine.setUseClientMode(false);
							future.getChannel().getPipeline().addFirst("ssl", new SslHandler(engine));
							future.getChannel().getPipeline().get(StringSyncCodec.class).getDecoder().resetState();
						}
					});
				}
				else
				{
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("_ssl_ok", "0");
					e.getChannel().write(new StringKeyValueHeader(map));
				}
				
				return;
			}
		}
		
		fireTransportListener(e.getChannel(), e.getMessage());
	}

	protected void fireTransportListener(final Channel channel, final Object message) throws Exception {
		mTransportListener.onTransportReceived(channel, message);
	}
	
	protected ITransportListener getListener()
	{
		return mTransportListener;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		super.channelConnected(ctx, e);
		
		mTransportListener.onTransportConnected(e.getChannel());
	}
	
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		super.channelDisconnected(ctx, e);
		
		mTransportListener.onTransportClosed(e.getChannel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		
		if (e.getCause() != null && !(e.getCause() instanceof ClosedChannelException))
		{
			AppLogger.e(this, e.getCause(), "exception caught: %s: %s [%s]", e.getCause(), e.getCause().getMessage(), e.getChannel());
			e.getChannel().close();
		}
        mTransportListener.onTransportException(e.getChannel(), e.getCause());
	}

}
