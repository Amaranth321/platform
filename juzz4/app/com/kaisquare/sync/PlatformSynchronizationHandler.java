package com.kaisquare.sync;

import com.kaisquare.kaisync.IClientChannel;
import com.kaisquare.kaisync.file.IServerSyncFile;
import com.kaisquare.kaisync.platform.Command;
import com.kaisquare.kaisync.platform.DeviceType;
import com.kaisquare.kaisync.platform.IPlatformSyncHandler;
import com.kaisquare.kaisync.platform.IServerUpdateFileInfo;
import com.kaisquare.playframework.PlayInvocationProxy;

import lib.util.JsonReader;
import models.NodeCommand;
import models.NodeLogFile;
import models.NodeLogFile.PullingStatus;
import models.SyncLog;
import models.node.NodeObject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import platform.CloudSyncManager;
import platform.DeviceManager;
import platform.config.readers.ConfigsServers;
import play.Logger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlatformSynchronizationHandler implements IPlatformSyncHandler {

	private static final int MAX_GET_COMMANDS = 10;
	
	private org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PlatformSynchronizationHandler.class);

	private static InetSocketAddress fsAddress;

	public PlatformSynchronizationHandler()
	{
	}

	@Override
	public InetSocketAddress getSoftwareUpdateHost() {
        JsonReader softwareUpdateCfg = ConfigsServers.getInstance().kaisyncSoftwareUpdateCfg();
        String host = softwareUpdateCfg.getAsString("host", null);
        int port = softwareUpdateCfg.getAsInt("port", 0);
        return new InetSocketAddress(host, port);
	}

	@Override
	public List<Command> getCommands(String identifier, String macAddress) {

		List<Command> commands = new ArrayList<Command>();
		TaskManager manager = TaskManager.getInstance();
		Iterable<NodeCommand> list = manager.getCommands(identifier, macAddress);
		
		try {
			int read = 0;

			if (list != null)
			{
				for (NodeCommand c : list)
				{
					Command cmd = new Command(c.getIdAsStr(), c.getCommand().toString(), c.getSourceId());
					cmd.getParameters().addAll(c.getParameters());
					manager.changeCommandState(NodeCommandState.Sending, c);
					commands.add(cmd);
					read++;
					
					if (read >= MAX_GET_COMMANDS)
						break;
				}
				
			}

			Iterable<NodeCommand> finishedCommands = manager.getFinishedCommands(identifier, macAddress);
			if (finishedCommands != null)
			{
				for (NodeCommand cmd : finishedCommands)
				{
					if (read >= MAX_GET_COMMANDS)
						break;
				
					if (cmd.getResult() == true)
						cmd.success();
					else
						cmd.failed();
					Command respCommand = new Command(cmd.getIdAsStr(), cmd.getCommand().toString(), cmd.getSourceId());
					respCommand.getParameters().add(cmd.getState().toString());
					commands.add(respCommand);
					cmd.delete();
					read++;
				}
			}
		}
		catch (Exception e) {
			Logger.error(e, "TaskManager:getCommands");
		}

		return commands;
	}

	@Override
	public void sendCommands(String identifier, String macAddress, List<Command> commands) {
		if (!TaskManager.getInstance().processCommand(identifier, macAddress, commands))
			throw new RuntimeException("failed to send command");
	}

	public IServerSyncFile syncEventVideoFile(String eventId, String nodeCoreId, String fileName) {
		try {
			InetSocketAddress address = getFileServerAddress();

//			Logger.info("sync event video request '%s', '%s', '%s'",
//					eventId,
//					nodeId,
//					fileName);
			
			NodeObject node = NodeObject.findByCoreId(nodeCoreId);
			int version = 99;
			try {
				version = Integer.parseInt(node.getNodeVersion().replace(".", "").substring(0, 2));
			} catch (Exception e) {}

			ServerSyncFile file = new ServerSyncFile(fileName,
					address.getHostString(),
					version < 43 ? address.getPort() - 10 : address.getPort(), //this is just for backward compatibility
					0,
					DateTime.now(DateTimeZone.UTC).toDate(),
					"");

			return file;
		} catch (Exception e) {
			Logger.warn("PlatformSynchronizationHandler.syncEventVideoFile: nodeId=%s : %s", nodeCoreId, e.getMessage());
			return null;
		}
	}

	@Override
	public boolean beforeProcess(IClientChannel channel) {
		InetSocketAddress address = channel.getAddress();
		if (address != null)
			logger.info(String.format("Node connected %s:%d", address.getHostString(), address.getPort()));

		return true;
	}

	@Override
	public boolean addLogFile(String nodeId, String fileID) {

		try {
			NodeLogFile record = NodeLogFile.find("nodeId", nodeId).first();
			if (record == null)
				record = new NodeLogFile(nodeId);
			
			record.status = PullingStatus.Standby;
			record.filename = fileID;
			record.uploadedDate = DateTime.now(DateTimeZone.UTC).toDate();
			record.save();

			return true;
		} catch (Exception e) {
			Logger.error(e, "");
		}

		return false;
	}

	@Override
	public IServerSyncFile uploadFile(String fileName) {
		try {
			InetSocketAddress address = getFileServerAddress();

			ServerSyncFile file = new ServerSyncFile(fileName,
					address.getHostString(),
					address.getPort(),
					0,
					DateTime.now(DateTimeZone.UTC).toDate(),
					"");

			return file;
		} catch (Exception e) {
			Logger.error(e, "uploadFile");
			return null;
		}
	}
	
	private static InetSocketAddress getFileServerAddress()
	{
		synchronized (PlatformSynchronizationHandler.class) {
			if (fsAddress == null)
			{
                JsonReader fileSvcCfg = ConfigsServers.getInstance().kaisyncFileServerCfg();
                String host = fileSvcCfg.getAsString("host", null);
                int port = fileSvcCfg.getAsInt("port", 0);
                fsAddress = new InetSocketAddress(host, port);
                if (fsAddress == null)
                {
                    Logger.error("file-server not set in config.json");
                    return null;
                }
            }
		}
		
		return fsAddress;
	}

	@Override
	public String getLatestVersion(DeviceType deviceType, double version) {
		return SoftwareUpdateHandler.getInstance().getLatestVersion(deviceType, version);
	}

	@Override
	public IServerSyncFile getLatestUpdateFile(DeviceType deviceType, double version) {
		return SoftwareUpdateHandler.getInstance().getLatestUpdateFile(deviceType, version);
	}

	@Override
	public IServerUpdateFileInfo getLatestUpdateFile(String identifier, String model) {
		return SoftwareUpdateHandler.getInstance().getLatestUpdateFile(identifier, model);
	}
	
	public static IPlatformSyncHandler getInstance()
	{
		return Holder.INSTANCE;
	}

	static class Holder
	{
		static final IPlatformSyncHandler INSTANCE = PlayInvocationProxy.newProxyInstance(
				IPlatformSyncHandler.class, PlatformSynchronizationHandler.class);
	}
}
