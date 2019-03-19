package com.kaisquare.sync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import models.SoftwareUpdateFile;

import com.kaisquare.kaisync.file.IServerSyncFile;

public class ServerSyncFile implements IServerSyncFile {
	
	private String mID;
	private String mHost;
	private int mPort;
	private long mSize;
	private Date mDate;
	private String mHash;
	
	public ServerSyncFile(SoftwareUpdateFile su)
	{
		this(su.getFileServerId(), su.getHost(), su.getPort(), su.getFileSize(), new Date(su.getUploadedTime()), su.getFileServerId());
	}
	
	public ServerSyncFile(String id, String host, int port, long size, Date date, String hash)
	{
		mID = id;
		mHost = host;
		mPort = port;
		mSize = size;
		mDate = date;
		mHash = hash;
	}

	@Override
	public String getID() {
		return mID;
	}

	@Override
	public String getHost() {
		return mHost;
	}

	@Override
	public int getPort() {
		return mPort;
	}

	@Override
	public long getSize() {
		return mSize;
	}

	@Override
	public Date getCreatedDate() {
		return mDate;
	}

	@Override
	public String getHash() {
		return mHash;
	}

}
