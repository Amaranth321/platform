package com.kaisquare.kaisync.file;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import com.kaisquare.kaisync.thrift.FileTransferService;
import com.kaisquare.kaisync.thrift.FileTransferService.Iface;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.ThriftUtil;
import com.kaisquare.kaisync.utils.ThriftUtil.Client;
import com.kaisquare.kaisync.utils.Utils;

/*package*/class FileTransferThriftClient extends AbstractFileClient {

	public FileTransferThriftClient(String host, int port, String truststore,
			String keypass) {
		super(host, port, truststore, keypass);
	}

	public FileTransferThriftClient(String host, int port) {
		super(host, port);
	}

	@Override
	public boolean deleteFile(String identifier) {
		boolean ret = false;
		Client<Iface> client = null;
		try {
			if (!Utils.isStringEmpty(getTrustStore()))
			{
				client = ThriftUtil.newServiceSSLClient(
						FileTransferService.Iface.class,
						FileTransferService.Client.class,
						getHost(),
						getPort(),
						10000, 3, 3000,
						getTrustStore(),
						getKeypass());
			}
			else
			{
				client = ThriftUtil.newServiceClient(
						FileTransferService.Iface.class,
						FileTransferService.Client.class,
						getHost(),
						getPort(),
						10000, 3, 3000);
			}
			ret = client.getIface().deleteFile(identifier);
		} catch (TTransportException e) {
			AppLogger.e(this, e, "TTransportException");
		} catch (TException e) {
			AppLogger.e(this, e, "TException");
		} finally {
			if (client != null)
				client.close();
		}
		
		return ret;
	}

}
