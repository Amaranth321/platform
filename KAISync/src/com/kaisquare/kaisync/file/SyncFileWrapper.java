package com.kaisquare.kaisync.file;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;

import com.kaisquare.kaisync.ISyncFile;
import com.kaisquare.kaisync.ISyncReadFile;
import com.kaisquare.kaisync.ISyncWriteFile;
import com.kaisquare.kaisync.thrift.FileAction;
import com.kaisquare.kaisync.thrift.SyncFile;

public class SyncFileWrapper implements ISyncFile, ISyncReadFile, ISyncWriteFile
{
	private SyncFile mFile;
	private FileAction mAction;
	private String mKeyStore;
	private String mKeypass;
	private HashMap<String, String> metadata = new HashMap<String, String>();
    private int readTimeout;
    private int writeTimeout;
	
	public SyncFileWrapper(SyncFile file, FileAction action)
	{
		this(file, action, null, null);
	}
	
	public SyncFileWrapper(SyncFile file, FileAction action, String keystore, String keypass)
	{
		mFile = file;
		mAction = action;
		setKeystore(keystore, keypass);
	}
	
	public void setKeystore(String keystore, String keypass) {
		mKeyStore = keystore;
		mKeypass = keypass;
	}

	@Override
	public void setMetadata(String key, String value)
	{
		metadata.put(key, value);
	}
	
	@Override
	public String getID() {
		return mFile.getIdentifier();
	}

    @Override
    public void setReadTimeout(int seconds) {
        readTimeout = seconds;
    }

    @Override
    public void setWriteTimeout(int seconds) {
        writeTimeout = seconds;
    }

	@Override
	public InputStream getInputStream() throws IOException {
		return getInputStream(0);
	}

	@Override
	public InputStream getInputStream(long position) throws IOException {
		if (mAction == FileAction.WRITE)
			throw new IllegalStateException("This file is for write only");
		
		FileSyncInputStream stream = new FileSyncInputStream(getID(), mFile.getHost(), mFile.getPort(), position, mKeyStore, mKeypass, readTimeout);
		mFile.setLength(stream.getTransport().getLength());
		
		return new BufferedInputStream(stream);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if (mAction == FileAction.READ)
			throw new IllegalStateException("This file is for read only");
		
//		return new FileSyncBufferedOutputStream(getID(), mFile.getHost(), mFile.getPort(), 0);
//		FileSyncPipedOutputStream stream = new FileSyncPipedOutputStream(
		TransportOutputStream stream = new TransportOutputStream(
				getID(), mFile.getHost(), mFile.getPort(), 0, mKeyStore, mKeypass, metadata, writeTimeout);
		mFile.setLength(stream.getTransport().getLength());
		
		return stream;
	}

	@Override
	public long getSize() {
		return mFile.getLength();
	}

	@Override
	public Date getCreatedDate() {
		return new Date(mFile.getCreatedTime());
	}

	@Override
	public String getHash() {
		return mFile.getHash();
	}

	public ISyncReadFile toReadFile()
	{
		return this;
	}

	public ISyncWriteFile toWriteFile()
	{
		return this;
	}
	
}
