package com.kaisquare.kaisync.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.thrift.TException;

import com.kaisquare.kaisync.thrift.FileAction;
import com.kaisquare.kaisync.thrift.FileChunk;
import com.kaisquare.kaisync.thrift.FileTransferService;
import com.kaisquare.kaisync.thrift.FileTransferService.Iface;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.ThriftUtil;
import com.kaisquare.kaisync.utils.Utils;
import com.kaisquare.kaisync.utils.ThriftUtil.Client;

@Deprecated
class ThriftFileTransport implements IFileTransport {
	
	private String mID;
	private Client<Iface> mClient; 
	
	public ThriftFileTransport(String host, int port) throws IOException
	{
		AppLogger.v(this, "Using Thrift transport");
		try {
			mClient = ThriftUtil.newServiceClient(
					FileTransferService.Iface.class,
					FileTransferService.Client.class,
					host,
					port,
					30000, 5, 5000);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void openFile(String identifier, FileOptions option, long position, Map<String, String> metadata)
			throws IOException {
		
		try {
			FileAction action = FileOptions.READ == option ? FileAction.READ : FileAction.WRITE;
			mID = mClient.getIface().openFile(identifier, action, position);
		} catch (TException e) {
			throw new IOException(e);
		}
		
		if (Utils.isStringEmpty(mID))
			throw new IOException("failed to open file '" + identifier + "'");
	}

	@Override
	public ChunkedData readFile(int chunkedSize) throws IOException {
		ChunkedData data = null;
		
		try {
			FileChunk chunkedData = mClient.getIface().readFile(mID, 8192);
			
			data = new ChunkedData(chunkedData.getSize(), chunkedData.getData());
		} catch (TException e) {
			throw new IOException(e);
		}
		
		return data;
	}

	@Override
	public void writeFile(ByteBuffer data, int length) throws IOException {
		try {
			mClient.getIface().writeFile(mID, data, length);
		} catch (TException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public void writeFile(InputStream in) throws IOException {
		byte[] buf = new byte[8192];
		int read = 0;
		
		while ((read = in.read(buf, 0, buf.length)) > 0)
		{
			try {
				mClient.getIface().writeFile(mID, ByteBuffer.wrap(buf, 0, read), read);
			} catch (TException e) {
				throw new IOException(e);
			}
		}
	}

	@Override
	public void close() {
		try {
			mClient.getIface().closeFile(mID);
		} catch (TException e) {
			AppLogger.e(this, e, "close");
		} finally {
			mClient.close();
		}
	}

	@Override
	public long getLength() {
		//TODO: should define a thrift interface for mClient.getIface().getFileLength(mID);
		return 0;
	}

    @Override
    public void setReadTimeout(int seconds) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setWriteTimeout(int seconds) {
        // TODO Auto-generated method stub
        
    }

}
