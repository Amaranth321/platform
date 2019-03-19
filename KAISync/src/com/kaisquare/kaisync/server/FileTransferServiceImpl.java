package com.kaisquare.kaisync.server;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.thrift.TException;

import com.kaisquare.kaisync.file.FileOptions;
import com.kaisquare.kaisync.file.IFileTransferHandler;
import com.kaisquare.kaisync.file.IServerSyncFile;
import com.kaisquare.kaisync.thrift.FileAction;
import com.kaisquare.kaisync.thrift.FileChunk;
import com.kaisquare.kaisync.thrift.SyncFile;
import com.kaisquare.kaisync.thrift.FileTransferService.Iface;

/*package*/class FileTransferServiceImpl implements Iface {
	
	private IFileTransferHandler mHandler;
	
	public FileTransferServiceImpl(IFileTransferHandler handler)
	{
		mHandler = handler;
	}

	@Override
	public String openFile(String identifier, FileAction action, long position) throws TException {
		return mHandler.openFile(identifier, action == FileAction.WRITE ? FileOptions.WRITE : FileOptions.READ, position, null);
	}

	@Override
	public FileChunk readFile(String id, int chunkedSize) throws TException {
		byte[] b = new byte[chunkedSize];
		int read = mHandler.readFile(id, b, 0, b.length);
		
		FileChunk chunk = new FileChunk();
		chunk.setSize(read);
		if (read > 0)
			chunk.setData(ByteBuffer.wrap(b, 0, read));
		else
			chunk.setData(new byte[0]);
		
		return chunk;
	}

	@Override
	public void writeFile(String id, ByteBuffer data, int length) throws TException {
		byte[] b = new byte[length];
		data.get(b);
		mHandler.writeFile(id, b, 0, length);
	}

	@Override
	public boolean closeFile(String id) throws TException {
		return mHandler.closeFile(id);
	}

	@Override
	public boolean deleteFile(String identifier) throws TException {
		return mHandler.deleteFile(identifier);
	}

	static SyncFile setSyncFile(SyncFile syncFile, IServerSyncFile file)
    {
    	Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    	c.setTime(file.getCreatedDate());
		syncFile.setIdentifier(file.getID());
		syncFile.setHost(file.getHost());
		syncFile.setPort(file.getPort());
		syncFile.setLength(file.getSize());
		syncFile.setCreatedTime(c.getTimeInMillis());
		syncFile.setHash(file.getHash());
		
		return syncFile;
    }
}
