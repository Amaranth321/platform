package com.kaisquare.kaisync.server;

import com.kaisquare.kaisync.file.IFileTransferHandler;
import com.kaisquare.kaisync.platform.IKaiOneProxyHandlerFactory;
import com.kaisquare.kaisync.platform.IPlatformSyncHandler;

public final class ServerBinder {
	
	private ServerBinder() {}
	
	public static ISyncServer bindPlatformSyncServer(int port, IPlatformSyncHandler handler)
	{
		return new PlatformSyncThriftServer(port, handler);
	}
	
	public static ISyncServer bindFileTransferServer(int port, IFileTransferHandler handler)
	{
		return new CombinedFileTransferServer(port, handler);
	}
	
	public static ISyncServer bindSoftwareUpdateServer(int port, ISoftwareUpdateHandler handler)
	{
		return new SoftwareUpdateServer(port, handler);
	}

	public static ISyncServer bindKaiOneProxyServer(int port, IKaiOneProxyHandlerFactory factory)
	{
		return new PlatformCommunicationServer(port, factory);
	}
}
