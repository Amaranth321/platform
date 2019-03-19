package com.kaisquare.kaisync.file;

import java.io.IOException;

import com.kaisquare.kaisync.utils.AppLogger;


/*package*/class FileTransferTcpClient extends AbstractFileClient {

	public FileTransferTcpClient(String host, int port, String truststore,
			String keypass) {
		super(host, port, truststore, keypass);
	}

	public FileTransferTcpClient(String host, int port) {
		super(host, port);
	}

	@Override
	public boolean deleteFile(String identifier) {
		boolean ret = false; 
		TcpFileTransport transport = null;
		try {
			transport = new TcpFileTransport(getHost(), getPort(), getTrustStore(), getKeypass());
			transport.connect();
			ret = transport.deleteFile(identifier);
		} catch (IOException e) {
			AppLogger.e(this, e, "error during deleting file '%s'", identifier);
		} finally {
			if (transport != null)
				transport.close();
		}
		
		return ret;
	}

}
