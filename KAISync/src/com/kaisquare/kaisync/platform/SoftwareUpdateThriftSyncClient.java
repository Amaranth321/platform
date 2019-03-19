package com.kaisquare.kaisync.platform;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import com.kaisquare.kaisync.ISyncReadFile;
import com.kaisquare.kaisync.file.SyncFileWrapper;
import com.kaisquare.kaisync.thrift.FileAction;
import com.kaisquare.kaisync.thrift.SoftwareUpdateService;
import com.kaisquare.kaisync.thrift.SyncFile;
import com.kaisquare.kaisync.thrift.SoftwareUpdateService.Iface;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.ThriftUtil;
import com.kaisquare.kaisync.utils.ThriftUtil.Client;

/*package*/class SoftwareUpdateThriftSyncClient implements ISoftwareUpdateSync {
	
	private static final int SOCKET_TIMEOUT = 1500;
	private static final int NUM_TRIES = 5;
	private static final int RETRY_DELAY = 3000;
	private Client<Iface> mClient;
	private DeviceType deviceType;
	
	public SoftwareUpdateThriftSyncClient(String host, int port, DeviceType type) throws TTransportException
	{
		deviceType = type;
		mClient = ThriftUtil.newServiceClient(
				SoftwareUpdateService.Iface.class,
				SoftwareUpdateService.Client.class, 
				host, 
				port, 
				SOCKET_TIMEOUT, NUM_TRIES, RETRY_DELAY);
	}

	@Override
	public String getLatestVersion() {
		try {
			return mClient.getIface().getLatestVersion0(deviceType.toString());
		} catch (TException e) {
			AppLogger.e(this, e, "");
			return "";
		}
	}

	@Override
	public ISyncReadFile getLatestUpdateFile() {
		try {
			SyncFile file = mClient.getIface().getLatestUpdateFile0(deviceType.toString());
			return new SyncFileWrapper(file, FileAction.READ).toReadFile();
		} catch (TException e) {
			AppLogger.e(this, e, "");
		}
		
		return null;
	}

	@Override
	public void close() {
		mClient.close();
		mClient = null;
	}
}
