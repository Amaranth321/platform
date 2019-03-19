package com.kaisquare.kaisync.server;

import org.apache.thrift.TException;

import com.kaisquare.kaisync.file.IServerSyncFile;
import com.kaisquare.kaisync.platform.DeviceType;
import com.kaisquare.kaisync.thrift.SoftwareUpdateService.Iface;
import com.kaisquare.kaisync.thrift.SyncFile;

/*package*/class SoftwareUpdateServiceImpl implements Iface {
	
	private ISoftwareUpdateHandler mHandler;
	
	public SoftwareUpdateServiceImpl(ISoftwareUpdateHandler handler)
	{
		mHandler = handler;
	}

	@Override
	public String getLatestVersion() throws TException {
		return mHandler.getLatestVersion(null, 4.2);
	}

	@Override
	public SyncFile getLatestUpdateFile() throws TException {
		IServerSyncFile file = mHandler.getLatestUpdateFile(null, 4.2);
		SyncFile syncFile = new SyncFile();
		if (file != null)
			FileTransferServiceImpl.setSyncFile(syncFile, file);
		
		return syncFile;
	}

	@Override
	public String getLatestVersion0(String type) throws TException {
		return mHandler.getLatestVersion(type == null ? null : DeviceType.valueOf(type), 4.2);
	}

	@Override
	public SyncFile getLatestUpdateFile0(String type) throws TException {
		IServerSyncFile file = mHandler.getLatestUpdateFile(type == null ? null : DeviceType.valueOf(type), 4.2);
		SyncFile syncFile = new SyncFile();
		
		if (file != null)
			FileTransferServiceImpl.setSyncFile(syncFile, file);
		
		return syncFile;
	}
}
