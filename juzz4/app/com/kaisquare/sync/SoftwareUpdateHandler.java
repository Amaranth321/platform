package com.kaisquare.sync;

import platform.nodesoftware.SoftwareManager;
import models.MongoDevice;
import models.SoftwareUpdateFile;
import models.node.NodeObject;

import com.kaisquare.kaisync.file.IServerSyncFile;
import com.kaisquare.kaisync.platform.DeviceType;
import com.kaisquare.kaisync.platform.IServerUpdateFileInfo;
import com.kaisquare.kaisync.server.ISoftwareUpdateHandler;
import com.kaisquare.util.StringCollectionUtil;
import play.Logger;

public class SoftwareUpdateHandler implements ISoftwareUpdateHandler {
	
	private SoftwareUpdateHandler()
	{
	}

	@Override
	public String getLatestVersion(DeviceType type, double version) 
	{
		SoftwareUpdateFile su = getLatestSoftwareUpdateByType(type, version);
		
		if (su != null)
			return su.getVersion();
		else
			return "";
	}

	private SoftwareUpdateFile getLatestSoftwareUpdateByType(DeviceType type, double version) {
		SoftwareUpdateFile su = null;
		if (type != null)
		{
			switch (type)
			{
			case NodeFour:
			case NodeTen:
			case Nodes:
				su = SoftwareUpdateFile.findEligibleUpdate(SoftwareManager.NODE_UBUNTU_MODEL_ID, version);
				break;
			case NodeOne:
				su = SoftwareUpdateFile.findEligibleUpdate(SoftwareManager.NODE_ONE_AMEGIA_MODEL_ID, version);
				break;
			}
		}
		return su;
	}

	@Override
	public IServerSyncFile getLatestUpdateFile(DeviceType type, double version) 
	{
        Logger.debug("[DeviceType=%s, %s] checking latest update file.", type, version);
		SoftwareUpdateFile su = getLatestSoftwareUpdateByType(type, version);
        if (su == null)
        {
            return null;
        }

        Logger.debug("[DeviceType=%s] latest update file returned. (%s).", type, su.getVersion());
        return new ServerSyncFile(su);
	}

	public IServerUpdateFileInfo getLatestUpdateFile(String identifier, String model) 
	{
		String modelId = model;
		NodeObject node = NodeObject.findByPlatformId(identifier);
        if (node == null)
        {
            return null;
        }
        
		if (StringCollectionUtil.isEmpty(model))
		{
			MongoDevice device = node.getDbDevice();
			if (device != null)
				modelId = device.getModelId();
		}

        Logger.debug("[%s:%s:%s] checking latest update file.", node.getName(), modelId, node.getReleaseNumber());
        SoftwareUpdateFile su = SoftwareUpdateFile.findEligibleUpdate(Long.valueOf(modelId), node.getReleaseNumber());
        if (su == null)
        {
            return null;
        }

        Logger.debug("[%s] latest update file returned. (%s).", node.getName(), su.getVersion());
        return new UpdateFileInfo(
                su.getVersion(),
                String.valueOf(su.getModelId()),
                new ServerSyncFile(su));
    }
	
	public static SoftwareUpdateHandler getInstance()
	{
		return Holder.INSTANCE;
	}

	static class Holder
	{
		public static final SoftwareUpdateHandler INSTANCE = new SoftwareUpdateHandler();
	}
	
	static class UpdateFileInfo implements IServerUpdateFileInfo
	{
		private String version;
		private String model;
		private IServerSyncFile file;

		public UpdateFileInfo(String version, String model, IServerSyncFile file)
		{
			this.version = version;
			this.model = model;
			this.file = file;
		}

		@Override
		public String getVersion() {
			return version;
		}

		@Override
		public String getModel() {
			return model;
		}

		@Override
		public IServerSyncFile getFile() {
			return file;
		}
		
	}
}
