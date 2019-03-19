package com.kaisquare.sync;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import lib.util.JsonReader;
import lib.util.exceptions.ApiException;
import models.SoftwareUpdateFile;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import platform.CloudSyncManager;
import platform.config.readers.ConfigsServers;
import platform.nodesoftware.SoftwareManager;
import play.Logger;

import com.google.gson.Gson;
import com.kaisquare.kaisync.ISyncWriteFile;
import com.kaisquare.kaisync.file.FileOptions;
import com.kaisquare.kaisync.file.IFileClient;
import com.kaisquare.kaisync.platform.DeviceType;
import com.kaisquare.util.Hash;

public class DataSync {
	
	public static final File FILE_PATH = new File("public/files/tmp/softwareuploads/");

	public static void addSoftwareUpdateFile(File file) throws Exception
	{
		if (!FILE_PATH.exists())
			FILE_PATH.mkdirs();
		
		File tmpFile = new File(FILE_PATH, UUID.randomUUID().toString());
		File updateFile = null;
		if (file.renameTo(tmpFile))
			updateFile = tmpFile;
		else
			updateFile = file;
		
		try {
			NodeUpdateFile kainodeFile = new NodeUpdateFile(updateFile);
			if (!kainodeFile.isValidUpdateFile())
            {
                throw new ApiException("Failed to save the update file. It may be corrupted");
            }

            String version = kainodeFile.getKainodeVersion();
            long modelId = kainodeFile.getUpdatefileType() == DeviceType.NodeOne ?
            		kainodeFile.getModelId() == null ? SoftwareManager.NODE_ONE_AMEGIA_MODEL_ID : Long.parseLong(kainodeFile.getModelId()) :
            		SoftwareManager.NODE_UBUNTU_MODEL_ID;
            SoftwareManager.getInstance().verifyUploadedFileVersion(modelId, version);

			boolean ret = false;

            JsonReader fileSvrCfg = ConfigsServers.getInstance().kaisyncFileServerCfg();
            String serverHost = fileSvrCfg.getAsString("host", null);
            int port = fileSvrCfg.getAsInt("port", 0);
			IFileClient client = CloudSyncManager.getInstance().newFileClient();
			
			String fileServerId = Hash.getFileChecksum(updateFile);
			ISyncWriteFile remoteFile = (ISyncWriteFile) client.openFile(fileServerId, FileOptions.WRITE);
			remoteFile.setWriteTimeout(60);
			remoteFile.setMetadata("category", "updatefile");
			remoteFile.setMetadata("sha1", fileServerId);
			remoteFile.setMetadata("contentType", "application/octet-stream");
			remoteFile.setMetadata("version", version);
			remoteFile.setMetadata("modelId", String.valueOf(modelId));
			
			InputStream in = null;
			OutputStream out = null;
			try {
				in = new BufferedInputStream(new FileInputStream(updateFile));
				out = remoteFile.getOutputStream();
				
				byte[] buf = new byte[8192];
				int read = 0;
				
				while ((read = in.read(buf)) > 0)
				{
					out.write(buf, 0, read);
				}
				
				ret = true;
			} catch (IOException e) {
				Logger.error(e, "failed to save update file");
				client.deleteFile(fileServerId);
			} finally {
				if (out != null)
				{
					try {
						out.flush();
						out.close();
					} catch (IOException e) {}
					
				}
				
				if (in != null)
				{
					try {
						in.close();
					} catch (IOException e) {}
				}
			}
			
			if (ret)
            {
                SoftwareUpdateFile dbFile = new SoftwareUpdateFile(fileServerId,
                                                                   modelId,
                                                                   version,
                                                                   serverHost,
                                                                   port,
                                                                   updateFile.length(),
                                                                   DateTime.now(DateTimeZone.UTC).getMillis());
                dbFile.save();
                Logger.info("Update file uploaded, modelId=%s, version=%s, fileServerId=%s, server=%s:%d",
                            modelId, version, fileServerId, serverHost, port);
            }
        } finally {
			if (tmpFile.exists())
				tmpFile.delete();
		}
	}
	
	public static void removeSoftwareUpdate(SoftwareUpdateFile updateFile)
	{
		IFileClient client = CloudSyncManager.getInstance().newFileClient();
		if (client.deleteFile(updateFile.getFileServerId()))
            updateFile.delete();
	}

	static class LockHolder
	{
		private Object mLock = new Object();
		private boolean mIsLocked = false;
		private long mLockedId = 0;
		
		public void lock()
		{
			if (Thread.currentThread().getId() == mLockedId)
				return;
			
			synchronized (mLock)
			{
				while (mIsLocked)
				{
					try {
						mLock.wait();
					} catch (InterruptedException e) {
					}
				}
		
				mLockedId = Thread.currentThread().getId();
				mIsLocked = true;
			}
		}
		
		public void unlock()
		{
			if (Thread.currentThread().getId() != mLockedId)
				return;
			
			synchronized (mLock)
			{
				mLockedId = 0;
				mIsLocked = false;
				mLock.notify();
			}
		}
		
		public boolean isLocked()
		{
			return mIsLocked;
		}
	}
}
