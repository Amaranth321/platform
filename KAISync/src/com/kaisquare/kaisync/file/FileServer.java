package com.kaisquare.kaisync.file;

import java.io.IOException;

public final class FileServer {

	public static IFileClient newFileClient(String host, int port, String keystore, String keypass)
	{
		return new FileTransferTcpClient(host, port, keystore, keypass);
	}

	static IFileTransport createTransport(String host, int port, String keystore, String keypass) throws IOException
	{
		return new TcpFileTransport(host, port, keystore, keypass);
	}
}
