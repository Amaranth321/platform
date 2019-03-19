package com.kaisquare.kaisync.server;

import com.kaisquare.kaisync.file.IFileTransferHandler;

public class CombinedFileTransferServer implements ISyncServer {
	
	private ISyncServer mThriftServer;
	private ISyncServer mFileAccessServer;
	
	public CombinedFileTransferServer(int port, IFileTransferHandler handler)
	{
		mThriftServer = new FileDirectAccessServer(port, handler);//new FileTransferServer(port, handler);
		mFileAccessServer = new FileDirectAccessServer(port + 10, handler);
	}
	
	@Override
	public void setReadTimeout(int timeout)
	{
		mThriftServer.setReadTimeout(timeout);
		mFileAccessServer.setReadTimeout(timeout);
	}
	
	@Override
	public void setWriteTimeout(int timeout)
	{
		mThriftServer.setWriteTimeout(timeout);
		mFileAccessServer.setWriteTimeout(timeout);
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getPort() {
		return mThriftServer.getPort();
	}
	
	public int getFileAccessPort()
	{
		return getPort() + 10;
	}

	@Override
	public void start() {
		mThriftServer.start();
		mFileAccessServer.start();
	}

	@Override
	public void stop() {
		mThriftServer.stop();
		mFileAccessServer.stop();
	}

	@Override
	public void setKeystore(String keystore, String keypass) {
		mThriftServer.setKeystore(keystore, keypass);
		mFileAccessServer.setKeystore(keystore, keypass);
	}

}
