package com.kaisquare.kaisync.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.stream.ChunkedStream;

import com.kaisquare.kaisync.codec.FileSyncInfo;
import com.kaisquare.kaisync.file.ChunkedData;
import com.kaisquare.kaisync.file.FileOptions;
import com.kaisquare.kaisync.file.IFileTransferHandler;
import com.kaisquare.kaisync.transport.ITransportHandlerFactory;
import com.kaisquare.kaisync.transport.ITransportListener;
import com.kaisquare.kaisync.transport.PThreadFactory;
import com.kaisquare.kaisync.transport.StringSyncPipelineFactory;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.Utils;

class FileDirectAccessServer extends NettyServer implements ITransportHandlerFactory, ITransportListener {

	private IFileTransferHandler mHandler;
	private String mKeystore;
	private String mKeypass;
	
	private Map<Channel, SessionHolder> mChannelSession = new HashMap<Channel, SessionHolder>();
	private ExecutorService mThreadPool;
	
	public FileDirectAccessServer(int port, IFileTransferHandler handler)
	{
		super(port);
		
		mHandler = handler;
		mThreadPool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 4,
				new PThreadFactory(FileDirectAccessServer.class.getSimpleName()));
	}

	@Override
	public void setKeystore(String keystore, String keypass) {
		mKeystore = keystore;
		mKeypass = keypass;
	}

	@Override
	public String getName() {
		return "FileDirectAccessServer";
	}

	@Override
	public void stop() {
		super.stop();
		
		mHandler.close();
		mThreadPool.shutdown();
	}

	@Override
	protected ChannelPipelineFactory createPipelineFactory() {
		StringSyncPipelineFactory factory = new StringSyncPipelineFactory(this, FileSyncInfo.class, mKeystore, mKeypass);
		factory.setReadTimeout(mReadTimeout);
		factory.setWriteTimeout(mWriteTimeout);
		return factory;
	}

	@Override
	public ITransportListener getTransportHandler() {
		return this;
	}
	
	@Override
	public void onTransportConnected(Channel channel) throws Exception {
		mChannelSession.put(channel, new SessionHolder(null, null, null));
	}

	@Override
	public void onTransportReceived(Channel channel, Object response) throws Exception {
		if (response instanceof FileSyncInfo)
		{
			FileSyncInfo info = (FileSyncInfo) response;
			Map<String, String> map = new HashMap<String, String>();
			FileOptions option = info.getOption();
			
			SessionHolder session = mChannelSession.get(channel);
			session.option = option;
			AppLogger.i(this, "%s file (%s) from channel %d", option, info.getID(), channel.getId());
			
			if (option == FileOptions.DELETE)
			{
				boolean deleted = mHandler.deleteFile(info.getID());
				map.put(FileSyncInfo.FIELD_ID, info.getID());
				map.put(FileSyncInfo.FIELD_OPTION, option.toString());
				map.put(FileSyncInfo.FIELD_STATUS, deleted ? 
						Integer.toString(FileSyncInfo.STATUS_SUCCESS) : 
						Integer.toString(FileSyncInfo.STATUS_FAILED));
				channel.write(new FileSyncInfo(map));
				return;
			}
			
			Map<String, String> metadata = FileSyncInfo.fromMetadataValue(info.getMetadata());
			String identifier = mHandler.openFile(info.getID(), option, info.getPosition(), metadata);
			session.id = identifier;
			if (Utils.isStringEmpty(identifier))
			{
				channel.close();
				return;
			}

			map.put(FileSyncInfo.FIELD_ID, identifier);
			map.put(FileSyncInfo.FIELD_OPTION, option.toString());
			map.put(FileSyncInfo.FIELD_POSITION, Long.toString(info.getPosition()));
			map.put(FileSyncInfo.FIELD_LENGTH, Long.toString(mHandler.getFileLength(identifier)));
			
			AppLogger.v(this, "send file info: %s=%s, %s=%s, %s=%s, %s=%s",
					FileSyncInfo.FIELD_ID, identifier,
					FileSyncInfo.FIELD_OPTION, option.toString(),
					FileSyncInfo.FIELD_POSITION, info.getPosition(),
					FileSyncInfo.FIELD_LENGTH, mHandler.getFileLength(identifier));
			
			channel.write(new FileSyncInfo(map));
			synchronized (session) {
				if (session == null || session.closed)
				{
					mHandler.closeFile(identifier);
					channel.close();
					return;
				}
				
				switch (option)
				{
				case READ:
					ChannelFuture writeFuture = channel.write(new ChunkedStream(mHandler.getInputStream(identifier), 8192));
					writeFuture.addListener(ChannelFutureListener.CLOSE);
					
					break;
				case WRITE:
					OutputStream out = mHandler.getOutputStream(identifier);
					session.out = out;
									
					break;
				case DELETE:
					break;
				}
			}
			
		}
		else if (response instanceof ChunkedData)
		{
			ChunkedData chunkedData = (ChunkedData) response;
			mChannelSession.get(channel).out.write(chunkedData.getData());
		}
		else if (response instanceof InputStream)
		{
			final SessionHolder session = mChannelSession.get(channel);
			final InputStream is = (InputStream) response;
			
			mThreadPool.execute(new Runnable() {
				
				@Override
				public void run() {
					byte[] buff = new byte[8192];
					int read = 0, total = 0;
					try {
						OutputStream out = session.out;
						while ((read = is.read(buff)) > 0)
						{
							total += read;
							out.write(buff, 0, read);
						}
						out.flush();
						AppLogger.d(this, "total written: %d", total);
					} catch (Exception e){
						AppLogger.e(this, "error reading message: %s", e.getMessage());
					} finally {
						mHandler.closeFile(session.id);
					}
				}
			});
		}
		else
		{
			AppLogger.w(this, "unknown request from %s", channel.getRemoteAddress());
			channel.close();
		}
	}

	@Override
	public void onTransportClosed(Channel channel) throws Exception {
		releaseSession(channel);
	}

	@Override
	public void onTransportException(Channel channel, Throwable cause) {
		releaseSession(channel);
	}
	
	private void releaseSession(Channel channel)
	{
		if (mChannelSession.containsKey(channel))
		{
			SessionHolder session = mChannelSession.get(channel);
			synchronized (session) {
				AppLogger.i(this, "close channel %d for file (%s), mode: %s", channel.getId(), session.id, session.option);
				try {
					if (session.id != null)
						mHandler.closeFile(session.id);
				} finally {
					mChannelSession.remove(channel);
					session.out = null;
					session.closed = true;
				}
			}
		}
		else
			AppLogger.d(this, "channel (%d) not in session", channel.getId());
	}
	
	private class SessionHolder
	{
		public String id;
		public volatile OutputStream out;
		public volatile boolean closed = false;
		public FileOptions option;
		
		public SessionHolder(String id, OutputStream out, FileOptions option)
		{
			this.id = id;
			this.out = out;
			this.option = option;
		}
	}
}
