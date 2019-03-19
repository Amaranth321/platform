package com.kaisquare.kaisync.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedStream;

import com.kaisquare.kaisync.codec.FileSyncInfo;
import com.kaisquare.kaisync.server.SSLContextProvider;
import com.kaisquare.kaisync.transport.ITransportHandlerFactory;
import com.kaisquare.kaisync.transport.ITransportListener;
import com.kaisquare.kaisync.transport.StringKeyValueHeader;
import com.kaisquare.kaisync.transport.StringSyncCodec;
import com.kaisquare.kaisync.transport.StringSyncPipelineFactory;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.Utils;

class TcpFileTransport implements IFileTransport, ITransportHandlerFactory, ITransportListener {
	
	protected static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final long DEFAULT_TIMEOUT = 60000;
	
	private ClientBootstrap mBootstrap;
	private volatile Channel mChannel;
	private String mHost;
	private int mPort;
	private InputStream in;
	private byte[] mInBuffer = new byte[DEFAULT_BUFFER_SIZE];
	private volatile boolean mClosed = true;
	
	private FileSyncInfo mFileInfo;
	private Object mWaitingLock = new Object();
	private String mKeystore;
	private String mKeypass;
	private Boolean mIsUsingSSL = null;
	private AtomicBoolean mWaitForResponse = new AtomicBoolean(false);
	private int readTimeout = 0;
	private int writeTimeout = 0;
    private Throwable mException;

	private volatile PipedInputStream mPipedIn;
	private volatile PipedOutputStream mPipedOut;
	
	public TcpFileTransport(String host, int port) throws IOException
	{
		this(host, port, null, null);
	}
	
	public TcpFileTransport(String host, int port, String keystore, String keypass) throws IOException
	{
		AppLogger.v(this, "Using TCP transport");
		mHost = host;
		mPort = port;
		setKeystore(keystore, keypass);
	}
	
	public void setKeystore(String keystore, String keypass)
	{
		mKeystore = keystore;
		mKeypass = keypass;
	}
	
	public synchronized void connect() throws IOException
	{
		if (!mClosed)
			return;
		
		mException = null;
		mBootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
		
		StringSyncPipelineFactory pipeline = new StringSyncPipelineFactory(this, FileSyncInfo.class);
		pipeline.setReadTimeout(readTimeout);
		pipeline.setWriteTimeout(writeTimeout);
		mBootstrap.setOption("child.tcpNoDelay", true);
		mBootstrap.setOption("connectTimeoutMillis", DEFAULT_TIMEOUT);
		mBootstrap.setPipelineFactory(pipeline);
		AppLogger.d(this, "connect to '%s:%d'", mHost, mPort);
		ChannelFuture future = mBootstrap.connect(new InetSocketAddress(mHost, mPort));
		
		try {
			if (!future.await(DEFAULT_TIMEOUT))
			    throw new IOException("get response timeout");
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
		
		if (future.isSuccess())
		{
			mChannel = future.getChannel();
			mClosed = false;
		}
		else
			throw new IOException("unable to connect server (" + mHost + ":" + mPort + ")");
		
		Map<String, String> map = new HashMap<String, String>();
		if (!Utils.isStringEmpty(mKeystore))
		{
			mWaitForResponse.set(true);
			map.put("_ssl", "1");
			future = mChannel.write(new StringKeyValueHeader(map));
			try {
				if (!future.await(DEFAULT_TIMEOUT))
				    throw new IOException("get response timeout");
				long timeout = 0;
				while (isConnected() && mWaitForResponse.get()) {
					synchronized (mWaitingLock) {
						mWaitingLock.wait(1000);
						timeout += 1000;
					}
					if (timeout >= DEFAULT_TIMEOUT)
					    throw new IOException("get response timeout");
				}
				mWaitForResponse.set(false);
				if (!isConnected())
					throw new IOException("server has disconnected (" + mHost + ":" + mPort + ")");
				
				if (mIsUsingSSL != null && mIsUsingSSL.booleanValue())
				{
					AppLogger.d(this, "start using SSL");
					SSLContext sslContext = SSLContextProvider.getTrustSslContext(mKeystore, mKeypass);
					SSLEngine engine = sslContext.createSSLEngine();
					engine.setUseClientMode(true);
					mChannel.getPipeline().addFirst("ssl", new SslHandler(engine));
					mChannel.getPipeline().get(StringSyncCodec.class).getDecoder().resetState();
				}
				else
					throw new IOException("server SSL not supported");
				
			} catch (InterruptedException e) {
				throw new IOException("Process interrupted");
			}
		}
	}

	@Override
	public void openFile(String identifier, FileOptions option, long position, Map<String, String> metadata) throws IOException {
		connect();
		AppLogger.v(this, "open file in '%s' mode", option);
		switch (option)
		{
		case READ:
			mPipedIn = new PipedInputStream();
			break;
		default:
			break;
		}
		
		Map<String, String> map = new HashMap<String, String>();
		map.put(FileSyncInfo.FIELD_ID, identifier);
		map.put(FileSyncInfo.FIELD_OPTION, option.toString());
		map.put(FileSyncInfo.FIELD_POSITION, Long.toString(position));
		if (metadata != null)
			map.put(FileSyncInfo.FIELD_METADATA, FileSyncInfo.toMetadataValue(metadata));
			
		FileSyncInfo info = new FileSyncInfo(map);
		ChannelFuture future = mChannel.write(info);
		try {
			if (!future.await(DEFAULT_TIMEOUT))
			    throw new IOException("get response timeout");
			if (!future.isSuccess())
				throw new IOException("unable to get file '" + identifier + "' information");
			waitForResponse(DEFAULT_TIMEOUT);
		} catch (InterruptedException e) {
		} catch (IOException e) {
			throw e;
		} finally {
			if (!isConnected() || !future.isSuccess())
				throw new IOException("unable to connect server for file '" + identifier + "'");
		}
	}

    @Override
    public void setReadTimeout(int seconds) {
        readTimeout = seconds;
    }

    @Override
    public void setWriteTimeout(int seconds) {
        writeTimeout = seconds;
    }
	
	private void ensureConnection() throws IOException
	{
		if (!isConnected())
			throw new IOException("Connection closed");
	}
	
	public boolean isConnected()
	{
		return !mClosed && mChannel != null;// && mChannel.isConnected();
	}

	@Override
	public ChunkedData readFile(int chunkedSize) throws IOException {
		long timeout = 0;
		while (isConnected() && in == null && mPipedOut == null)
		{
			try {
				synchronized (mWaitingLock) {
					mWaitingLock.wait(1000);
					timeout += 1000;
				}
				if (timeout >= DEFAULT_TIMEOUT)
				    throw new IOException("get response timeout");
			} catch (InterruptedException e) {
				throw new IOException("Stream interrupted");
			}
		}
		if (in == null && mPipedOut == null)
			throw new IOException("Stream is closed");
		
		int read = 0;
		read = in != null ? in.read(mInBuffer, 0, mInBuffer.length) : mPipedIn.read(mInBuffer, 0, mInBuffer.length);
		if (mException != null)
		    throw new IOException(mException);
		ChunkedData chunkedData = new ChunkedData(read, mInBuffer);
		return chunkedData;
	}

	@Override
	public void writeFile(ByteBuffer data, int length) throws IOException {
		ensureConnection();
		
		ChannelBuffer buffer = ChannelBuffers.copiedBuffer(data);
		checkFuture(mChannel.write(buffer, mChannel.getRemoteAddress()));
		if (mException != null)
            throw new IOException(mException);
	}
	
	@Override
	public void writeFile(InputStream in) throws IOException
	{
		ensureConnection();		
		checkFuture(mChannel.write(new ChunkedStream(in, DEFAULT_BUFFER_SIZE)));
		if (mException != null)
            throw new IOException(mException);
	}

	@Override
	public long getLength() {
		long length = 0;
		
		if (mFileInfo != null)
			length = mFileInfo.getLength();
		
		return length;
	}
	
	private void checkFuture(ChannelFuture future) throws IOException
	{
		try {
			future.await();
		} catch (InterruptedException e) {
		} finally {
			if (!future.isSuccess())
				throw new IOException("unable to write stream");
		}
	}
	
	public boolean deleteFile(String identifier) throws IOException
	{
		ensureConnection();
		
		Map<String, String> map = new HashMap<String, String>();
		map.put(FileSyncInfo.FIELD_ID, identifier);
		map.put(FileSyncInfo.FIELD_OPTION, FileOptions.DELETE.toString());
		FileSyncInfo info = new FileSyncInfo(map);
		ChannelFuture future = mChannel.write(info);
		
		try {
			if (!future.await(DEFAULT_TIMEOUT))
			    throw new IOException("get response timeout");
			if (!future.isSuccess())
				throw new IOException("failed to delete file '" + identifier + "'");
			waitForResponse(DEFAULT_TIMEOUT);
			
			if (mFileInfo != null)
				return mFileInfo.getStatus() == FileSyncInfo.STATUS_SUCCESS;
		} catch (InterruptedException e) {
		} finally {
		}
		
		return false;
	}

	private void waitForResponse(long timeout) throws InterruptedException, IOException {
	    long elapsed = 0;
		while (isConnected() && mFileInfo == null)
		{
			synchronized (mWaitingLock) {
				mWaitingLock.wait(1000);
				elapsed += 1000;
			}
			if (elapsed >= DEFAULT_TIMEOUT)
			    throw new IOException("get response timeout");
		}
	}

    private void closeTransport() {
        mClosed = true;
        if (mPipedOut != null)
        {
            try {
                mPipedOut.flush();
                mPipedOut.close();
            } catch (Exception e) {}
        }
    }

	@Override
	public void close() {
		AppLogger.v(this, "close TCP transport");
		mClosed = true;
		mWaitForResponse.set(false);
		synchronized (mWaitingLock) {
			mWaitingLock.notifyAll();
		}
		if (mPipedOut != null)
		{
			try {
				mPipedOut.flush();
				mPipedOut.close();
			} catch (Exception e) {}
		}
		if (mPipedIn != null)
		{
			try {
				mPipedIn.close();
			} catch (Exception e) {}
		}
		mChannel.close();
		mBootstrap.releaseExternalResources();
	}

	@Override
	public ITransportListener getTransportHandler() {
		return this;
	}

	@Override
	public void onTransportConnected(Channel channel) throws Exception {
		
	}

	@Override
	public void onTransportReceived(Channel channel, Object response) throws Exception {
		if (mIsUsingSSL == null && response instanceof StringKeyValueHeader)
		{
			StringKeyValueHeader header = (StringKeyValueHeader) response;
			String sslOk = header.get("_ssl_ok");
			if (!Utils.isStringEmpty(sslOk))
			{
				if ("1".equals(sslOk))
					mIsUsingSSL = Boolean.valueOf(true);
				else
					mIsUsingSSL = Boolean.valueOf(false);
				
				mWaitForResponse.set(false);
				synchronized (mWaitingLock) {
					mWaitingLock.notifyAll();	
				}
				return;
			}
		}
		if (response instanceof FileSyncInfo)
		{
			FileSyncInfo info = (FileSyncInfo) response;
			Map<String, String> map = new HashMap<String, String>();
			map.put(FileSyncInfo.FIELD_ID, info.getID());
			map.put(FileSyncInfo.FIELD_OPTION, info.getOption().toString());
			map.put(FileSyncInfo.FIELD_POSITION, Long.toString(info.getPosition()));
			map.put(FileSyncInfo.FIELD_LENGTH, Long.toString(info.getLength()));
			map.put(FileSyncInfo.FIELD_STATUS, Integer.toString(info.getStatus()));
			mFileInfo = new FileSyncInfo(map);
			AppLogger.v(this, "received file info: %s=%s, %s=%s, %s=%d, %s=%d, %s=%d",
					FileSyncInfo.FIELD_ID, mFileInfo.getID(),
					FileSyncInfo.FIELD_OPTION, mFileInfo.getOption().toString(),
					FileSyncInfo.FIELD_POSITION, mFileInfo.getPosition(),
					FileSyncInfo.FIELD_LENGTH, mFileInfo.getLength(),
					FileSyncInfo.FIELD_STATUS, mFileInfo.getStatus());
			synchronized (mWaitingLock) {
				mWaitingLock.notifyAll();
			}
		}
		else if (response instanceof ChunkedData)
		{
			ChunkedData chunk = (ChunkedData) response;
			if (mPipedOut == null)
				mPipedOut = new PipedOutputStream(mPipedIn);
			synchronized (mWaitingLock) {
				mWaitingLock.notifyAll();
			}
			mPipedOut.write(chunk.getData());
			mPipedOut.flush();
		}
		else if (response instanceof InputStream)
		{
			in = (InputStream) response;
			synchronized (mWaitingLock) {
				mWaitingLock.notify();
			}
		}
	}

	@Override
	public void onTransportClosed(Channel channel) throws Exception {
		closeTransport();
	}

    @Override
	public void onTransportException(Channel channel, Throwable cause) {
        mException = cause;
	    closeTransport();
	}
}