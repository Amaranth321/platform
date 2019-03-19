package com.kaisquare.kaisync.platform;

import java.io.IOException;


public final class PlatformSync {
	
	private PlatformSync() {}
	
	public static IPlatformSync newPlatformClient(String host, int port, String keystore, String keypass, String username, String password) throws IOException
	{
		return new PlatformSyncRabbitmqClient(host, port, keystore, keypass, username, password);
	}

	public static void releasePlatformSync(IPlatformSync sync)
	{
	    if (sync instanceof PlatformSyncRabbitmqClient)
	        ((PlatformSyncRabbitmqClient)sync).release();
	}
}
