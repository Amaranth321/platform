package com.kaisquare.kaisync;

import java.io.IOException;

import com.kaisquare.kaisync.file.FileServer;
import com.kaisquare.kaisync.file.IFileClient;
import com.kaisquare.kaisync.platform.IPlatformSync;
import com.kaisquare.kaisync.platform.PlatformSync;

/**
 * v{@value #VERSION}
 * KAISync is a library for communicating between KAI UP (Cloud) and KAI Node, or other server/client services
 */
public final class KAISync {
	
	private static final String VERSION = "1.3.7";
	
	private KAISync() {}
	
	/**
	 * Get platform sync client
	 * @param host host of platform sync server
	 * @param port port of platform sync server
	 * @return an interface or proxy for accessing between client and server
	 * @throws IOException 
	 */
	public static IPlatformSync newPlatformClient(String host, int port, String truststore, String keypass) throws IOException
	{
		return newPlatformClient(host, port, truststore, keypass, null, null);
	}
	
	/**
	 * Get platform sync client
	 * @param host host of platform sync server
	 * @param port port of platform sync server
	 * @return an interface or proxy for accessing between client and server
	 * @throws IOException 
	 */
	public static IPlatformSync newPlatformClient(String host, int port, String truststore, String keypass, String username, String password) throws IOException
	{
		return PlatformSync.newPlatformClient(host, port, truststore, keypass, username, password);
	}
	
	/**
	 * Release platform sync client if it's no longer in use or there will be no client being created
	 * @param client
	 */
	public static void releasePlatformClient(IPlatformSync client)
	{
	    PlatformSync.releasePlatformSync(client);
	}
	
	/**
	 * Get a file client handler, it can directly access file on the server via the {@link IFileClient} interface
	 * @param host host of file server
	 * @param port port of file server
	 * @return an interface or proxy for accessing between client and server
	 */
	public static IFileClient newFileClient(String host, int port)
	{
		return FileServer.newFileClient(host, port, null, null);
	}
	
	/**
	 * Get a file client handler, it can directly access file on the server via the {@link IFileClient} interface
	 * @param host host of file server
	 * @param port port of file server
	 * @param truststore the client truststore
	 * @param keypass the password of truststore
	 * @return an interface or proxy for accessing between client and server
	 */
	public static IFileClient newFileClient(String host, int port, String truststore, String keypass)
	{
		return FileServer.newFileClient(host, port, truststore, keypass);
	}
	
	/**
	 * Get current version of {@link KAISync}
	 * @return version
	 */
	public static String getVersion()
	{
		return VERSION;
	}
}
