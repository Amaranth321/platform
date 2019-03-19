package com.kaisquare.kaisync.file;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class FileSyncPipedOutputStream extends PipedOutputStream {
	
	private volatile IFileTransport mTransport;
	private PipedInputStream mPipedInput;
	private ExecutorService mSinglePool;
	private Future<Throwable> mFuture;
	
	public FileSyncPipedOutputStream(String identifier, String host, int port, long position, String keystore, String keypass, Map<String, String> metadata) throws IOException
	{
	    this(identifier, host, port, position, keystore, keypass, metadata, 0);
	}
	
	public FileSyncPipedOutputStream(String identifier, String host, int port, long position, String keystore, String keypass, Map<String, String> metadata, int timeout) throws IOException
	{
		super();
		
		mTransport = FileServer.createTransport(host, port, keystore, keypass);
		mTransport.setWriteTimeout(timeout);
		mTransport.openFile(identifier, FileOptions.WRITE, position, metadata);
		
		mPipedInput = new PipedInputStream(this, 8192);
		
		mSinglePool = Executors.newSingleThreadExecutor();
		mFuture = (Future<Throwable>) mSinglePool.submit(new WriteTask());
	}
	
	IFileTransport getTransport()
	{
		return mTransport;
	}

	private class WriteTask implements Callable<Throwable>
	{
		@Override
		public Throwable call()
		{
			try {
				mTransport.writeFile(mPipedInput);
			} catch (IOException e) {
				return e; 
			}
			
			return null;
		}
	}

	@Override
	public void close() throws IOException {
		
		try {
			super.close();
		} finally {
			try {
				Throwable t = mFuture.get(3000, TimeUnit.MILLISECONDS);
				mPipedInput.close();
				
				if (t != null)
					throw t;
			} catch (Throwable e) {
				throw new IOException(e);
			} finally {
				mTransport.close();
				mTransport = null;
				try {
					mFuture.get();
				} catch (InterruptedException | ExecutionException e) {}
				mSinglePool.shutdown();
			}
		}
	}
}
