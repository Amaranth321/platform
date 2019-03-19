package com.kaisquare.sync;

import com.kaisquare.kaisync.file.FileOptions;
import com.kaisquare.kaisync.file.IFileTransferHandler;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import com.mongodb.gridfs.GridFSInputFile;

import platform.config.readers.ConfigsServers;
import play.Logger;
import play.modules.morphia.Model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class FileTransferHandler implements IFileTransferHandler {
	public static String FILE_BUCKET = "fileserver";
	
	private static long ACCESS_TIMEOUT = 300 * 1000;
	private static int LIMIT_FILE_AMOUNT = 800; 
	
	private Map<String, FileHolder> mOpenedFiles = new ConcurrentHashMap<String, FileHolder>();
	private Timer timer;
	
	public FileTransferHandler()
	{
		ConfigsServers config = ConfigsServers.getInstance();
		FILE_BUCKET = config.kaisyncFileServerCfg().getAsString("bucket", FILE_BUCKET);
        ACCESS_TIMEOUT = config.kaisyncFileServerCfg().getAsLong("timeout", ACCESS_TIMEOUT);
        LIMIT_FILE_AMOUNT = config.kaisyncFileServerCfg().getAsInt("openlimits", LIMIT_FILE_AMOUNT);
		timer = new Timer();
		timer.schedule(new FileTimoutCheckTask(), ACCESS_TIMEOUT, ACCESS_TIMEOUT);
		
		DBCollection col = Model.db().getCollection(FILE_BUCKET + ".files");
		DBObject indexes = new BasicDBObject();
		indexes.put("metadata.category", 1);
		indexes.put("uploadDate", 1);
		col.ensureIndex(indexes);
	}

	@Override
	public String openFile(String identifier, FileOptions option, long position, Map<String, String> metadata) {
		if (mOpenedFiles.size() >= LIMIT_FILE_AMOUNT)
		{
			Logger.error("reached limit of opened files for file server (%d)", LIMIT_FILE_AMOUNT);
			return "";
		}
		
		String id = "";
		GridFS fs = new GridFS(Model.db(), FILE_BUCKET);
		GridFSFile gfs = fs.findOne(identifier);
		
		synchronized (mOpenedFiles)
		{
			switch (option)
			{
			case READ:
				if (gfs != null)
				{
					id = UUID.randomUUID().toString();
					mOpenedFiles.put(id, new FileHolder(identifier, gfs, position));
				}
				else
					Logger.warn("file '%s' doesn't exist", identifier);
				
				break;
			case WRITE:
				if (gfs != null)
					fs.remove(identifier);
				
				gfs = fs.createFile(identifier);
				if (metadata != null)
				{
					Logger.debug("[FileTransferHandler] stored file %s", metadata);
					DBObject fileMetadata = new BasicDBObject();
					fileMetadata.putAll(metadata);
					gfs.setMetaData(fileMetadata);
				}
				id = UUID.randomUUID().toString();
				mOpenedFiles.put(id, new FileHolder(identifier, gfs, 0));
			}
		}
		
		return id;
	}

	@Override
	public int readFile(String identifier, byte[] b, int offset, int length) {
		int read = -1;
		FileHolder holder = mOpenedFiles.get(identifier);
		if (holder != null && holder.getInputStream() != null)
		{
			holder.setAccessTime();
			InputStream in = holder.getInputStream();
			try {
				read = in.read(b, offset, length);
				if (read == -1)
					closeFile(identifier);
				
			} catch (IOException e) {
				Logger.error(e, "readFile");
				closeFile(identifier);
			}
		}
		
		return read;
	}

	@Override
	public void writeFile(String identifier, byte[] src, int offset, int length) {
		FileHolder holder = mOpenedFiles.get(identifier);
		if (holder != null && holder.getOutputStream() != null)
		{
			holder.setAccessTime();
			OutputStream out = holder.getOutputStream();
			try {
				out.write(src, offset, length);
			} catch (IOException e) {
				Logger.error(e, "writeFile");
				closeFile(identifier);
			}
		}
	}

	@Override
	public boolean closeFile(String identifier) {
		boolean ret = false;
		
		synchronized (mOpenedFiles)
		{
			ret = closeFileInternal(identifier);
		}
		mOpenedFiles.remove(identifier);
		
		return ret;
	}
	
	private boolean closeFileInternal(String identifier) {
		boolean ret = false;
		
		FileHolder holder = mOpenedFiles.get(identifier);
		if (holder != null)
		{
			if (holder.getInputStream() != null)
			{
				try {
					holder.getInputStream().close();
					ret = true;
				} catch (IOException e) {
				}
			}
			
			if (holder.getOutputStream() != null)
			{
				OutputStream out = holder.getOutputStream();
				try {
					out.flush();
					out.close();
					ret = true;
				} catch (IOException e) {
				}
			}
		}
	
		return ret;
	}

	@Override
	public boolean deleteFile(String identifier) {
		GridFS fs = new GridFS(Model.db(), FILE_BUCKET);
		try {
			fs.remove(identifier);
		} catch (Exception e) {
			Logger.error(e, "failed to delete file: '%s'", identifier);
			return false;
		}
		
		return true;
	}

	@Override
	public InputStream getInputStream(String identifier) {
		return mOpenedFiles.get(identifier).getInputStream();
	}

	@Override
	public OutputStream getOutputStream(String identifier) {
		return mOpenedFiles.get(identifier).getOutputStream();
	}

	@Override
	public long getFileLength(String identifier) {
		FileHolder holder = mOpenedFiles.get(identifier);
		if (holder != null)
			return holder.gfs.getLength();
		
		return 0;
	}

	class FileHolder
	{
		private String name;
		private GridFSFile gfs;
		private InputStream input;
		private OutputStream output;
		private long skipBytes;
		private volatile long accessTime;
		
		public FileHolder(String name, GridFSFile gfs, long skipBytes)
		{
			accessTime = System.currentTimeMillis();
			this.name = name;
			this.gfs = gfs;
			this.skipBytes = skipBytes;
		}
		
		public GridFSFile getGfs()
		{
			return gfs;
		}
		
		public String getName()
		{
			return name;
		}
		
		public void setAccessTime()
		{
			accessTime = System.currentTimeMillis();
		}
		
		public long getAccessTime()
		{
			return accessTime;
		}
		
		public InputStream getInputStream()
		{
			if (input == null)
			{
				input = gfs instanceof GridFSDBFile ? ((GridFSDBFile)gfs).getInputStream() : null;
				
				if (skipBytes > 0 && input != null)
				{
					try {
						long skipped = input.skip(skipBytes);
						Logger.warn("'%s' expected skipping bytes %d, actual skipped bytes %d", name, skipBytes, skipped);
					} catch (IOException e) {
						Logger.error(e, "'%s' failed to skip to position %d", name, skipBytes);
					}
				}
			}
				
			return input;
		}
		
		public OutputStream getOutputStream()
		{
			if (output == null)
				output = gfs instanceof GridFSInputFile ? ((GridFSInputFile)gfs).getOutputStream() : null;
				
			return output;
		}
	}
	
	class FileTimoutCheckTask extends TimerTask
	{

		@Override
		public void run() {
			synchronized (mOpenedFiles)
			{
				long now = System.currentTimeMillis();
				Iterator<Entry<String, FileHolder>> iterator = mOpenedFiles.entrySet().iterator();
				while (iterator.hasNext())
				{
					Entry<String, FileHolder> entry = iterator.next();
					String identifier = entry.getKey();
					FileHolder holder = entry.getValue();
					long idle = now - holder.getAccessTime();
					if (idle >= ACCESS_TIMEOUT)
					{
						Logger.warn("File '%s' (%s) idle for %d, close file (files: %d)", identifier, holder.getName(), idle, mOpenedFiles.size());
						closeFileInternal(identifier);
						iterator.remove();
					}
				}
			}
		}
		
	}

	@Override
	public void close() {
		synchronized (mOpenedFiles)
		{
			Iterator<Entry<String, FileHolder>> iterator = mOpenedFiles.entrySet().iterator();
			while (iterator.hasNext())
			{
				closeFileInternal(iterator.next().getKey());
				iterator.remove();
			}
		}
	}
}
