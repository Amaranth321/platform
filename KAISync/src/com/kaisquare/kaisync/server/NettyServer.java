package com.kaisquare.kaisync.server;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioServerSocketChannelFactory;

public abstract class NettyServer implements ISyncServer {
	
	protected int mPort;
	protected ServerBootstrap mBootstrap;
	protected Channel mChannel;
	private boolean mBlockingMode = false;
	private ExecutorService mBossExecutor;
	private ExecutorService mWorkerExecutor;
	protected int mReadTimeout = 0;
	protected int mWriteTimeout = 0;
	
	NettyServer(int port)
	{
		mPort = port;
	}
	
	protected void setBlockingChannelMode(boolean blocking)
	{
		mBlockingMode = blocking;
	}
	
	@Override
	public void setReadTimeout(int timeout)
	{
		mReadTimeout = timeout;
	}
	
	@Override
	public void setWriteTimeout(int timeout)
	{
		mWriteTimeout = timeout;
	}

	@Override
	public int getPort() {
		return mPort;
	}

	@Override
	public void start() {
		ChannelFactory channelFactory;
		mBossExecutor = Executors.newCachedThreadPool();
		mWorkerExecutor = Executors.newCachedThreadPool();
		if (mBlockingMode)
		{
			channelFactory = new OioServerSocketChannelFactory(
					mBossExecutor, mWorkerExecutor);
		}
		else
		{
			channelFactory = new NioServerSocketChannelFactory(
					mBossExecutor, mWorkerExecutor);
		}
		mBootstrap = new ServerBootstrap(channelFactory);
		
		mBootstrap.setPipelineFactory(createPipelineFactory());
		mBootstrap.setOption("child.tcpNoDelay", true);
		mChannel = mBootstrap.bind(new InetSocketAddress(mPort));
	}

	@Override
	public void stop() {
		mBossExecutor.shutdownNow();
		mWorkerExecutor.shutdownNow();
		mChannel.close();
		mBootstrap.releaseExternalResources();
	}

	/**
	 * Create netty pipeline factory for netty server
	 * @return netty's {@link ChannelPipelineFactory}
	 */
	protected abstract ChannelPipelineFactory createPipelineFactory();
}
