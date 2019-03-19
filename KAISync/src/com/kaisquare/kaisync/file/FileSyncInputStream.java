package com.kaisquare.kaisync.file;

import java.io.IOException;
import java.io.InputStream;

/*package*/class FileSyncInputStream extends InputStream {
	
	private IFileTransport mTransport;
	private int readCount;
	
	public FileSyncInputStream(String identifier, String host, int port, long position, String keystore, String keypass, int timeout) throws IOException
	{
		mTransport = FileServer.createTransport(host, port, keystore, keypass);
		mTransport.setReadTimeout(timeout);
		mTransport.openFile(identifier, FileOptions.READ, position, null);
	}
	
	IFileTransport getTransport()
	{
		return mTransport;
	}
	
	private int readBuffer(byte[] b, int off, int len) throws IOException
	{
		int read = -1;
		ChunkedData chunkedData = mTransport.readFile(len);
		if (chunkedData != null)
		{
			read = chunkedData.getSize();
			if (chunkedData.getSize() > 0)
			{
				readCount += chunkedData.getSize();
				chunkedData.copy(b, off, len);
			}
			chunkedData = null;
		}
		
		return read;
	}

	@Override
	public int available() throws IOException {
		return (int) (mTransport.getLength() - readCount);
	}

	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		if (readBuffer(b, 0, 1) < 0)
			return -1;
		
		return b[0] & 0xff;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return readBuffer(b, off, len);
	}

	@Override
	public void close() throws IOException {
		mTransport.close();
	}

}
