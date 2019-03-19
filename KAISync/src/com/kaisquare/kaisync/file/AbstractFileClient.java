package com.kaisquare.kaisync.file;

import java.util.TimeZone;

import com.kaisquare.kaisync.ISyncFile;
import com.kaisquare.kaisync.thrift.FileAction;
import com.kaisquare.kaisync.thrift.SyncFile;

public abstract class AbstractFileClient implements IFileClient {

	private String mHost;
	private int mPort;
	private String mTruststore;
	private String mKeypass;
	
	public AbstractFileClient(String host, int port)
	{
		mHost = host;
		mPort = port;
	}
	
	public AbstractFileClient(String host, int port, String truststore, String keypass)
	{
		mHost = host;
		mPort = port;
		mTruststore = truststore;
		mKeypass = keypass;
	}
	
	public String getHost()
	{
		return mHost;
	}
	
	public int getPort()
	{
		return mPort;
	}
	
	public String getTrustStore()
	{
		return mTruststore;
	}
	
	public String getKeypass()
	{
		return mKeypass;
	}
	
	@Override
	public ISyncFile openFile(String identifier, FileOptions options) {
		return openFile(identifier, options, mTruststore, mKeypass);
	}

	@Override
	public ISyncFile openFile(String identifier, FileOptions options, String keystore, String keypass) {
		ISyncFile syncFile = null;
		long now = System.currentTimeMillis();
		TimeZone tz = TimeZone.getDefault();
		int offset = tz.getOffset(now);
		long utc = now - offset;
		
		SyncFile file = new SyncFile();
		file.setIdentifier(identifier);
		file.setHost(mHost);
		file.setPort(mPort);
		file.setCreatedTime(utc);
		
		switch (options)
		{
		case READ:
			syncFile = new SyncFileWrapper(file, FileAction.READ, keystore, keypass);
			break;
		case WRITE:
			syncFile = new SyncFileWrapper(file, FileAction.WRITE, keystore, keypass);
			break;
		default:
			throw new IllegalArgumentException("Unknown file option");
		}
		
		return syncFile;
	}

}
