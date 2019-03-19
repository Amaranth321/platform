package com.kaisquare.kaisync.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.TException;

import com.kaisquare.kaisync.IClientChannel;
import com.kaisquare.kaisync.file.IServerSyncFile;
import com.kaisquare.kaisync.platform.IPlatformSyncHandler;
import com.kaisquare.kaisync.thrift.Command;
import com.kaisquare.kaisync.thrift.NetAddress;
import com.kaisquare.kaisync.thrift.PlatformSynchronizationService.Iface;
import com.kaisquare.kaisync.thrift.SyncFile;

/*package*/class PlatformSynchronizationServiceImpl implements Iface, IClientChannelListener {
	
	private IPlatformSyncHandler mHandler;
	
	public PlatformSynchronizationServiceImpl(IPlatformSyncHandler handler)
	{
		mHandler = handler;
	}

	@Override
	public SyncFile getDbSyncFile(String identifier) throws TException {
		return null;
	}

	@Override
	public NetAddress getSoftwareUpdateHost() throws TException {
		InetSocketAddress address = mHandler.getSoftwareUpdateHost();
		NetAddress addr = new NetAddress();
		addr.setHost(address.getHostName());
		addr.setPort(address.getPort());
		
		return addr;
	}

	@Override
	public List<Command> getCommands(String identifier) throws TException {
		return getCommands1(identifier, "");
	}

	@Override
	public boolean sendCommands(String identifier, List<Command> commands)
			throws TException {
		boolean ret = sendCommands1(identifier, "", commands);
		commands.clear();
		return ret;
	}

	@Override
	public List<Command> getCommands1(String identifier, String macAddress) throws TException {
		List<com.kaisquare.kaisync.platform.Command> commands = mHandler.getCommands(identifier, macAddress);
		List<Command> list = new ArrayList<Command>();
		if (commands != null)
		{
			for (com.kaisquare.kaisync.platform.Command c : commands)
			{
				Command cmd = new Command();
				cmd.setId(c.getId());
				cmd.setCommand(c.getCommand());
				cmd.setParameters(c.getParameters());
				cmd.setOriginalId(c.getOriginalId());
				list.add(cmd);
			}
		}
		
		return list; 
	}

	@Override
	public boolean sendCommands1(String identifier, String macAddress, List<Command> commands) throws TException {
		List<com.kaisquare.kaisync.platform.Command> list = new ArrayList<com.kaisquare.kaisync.platform.Command>();
		
		if (commands != null)
		{
			for (Command c : commands)
			{
				com.kaisquare.kaisync.platform.Command cmd = 
					new com.kaisquare.kaisync.platform.Command(c.getId(), c.getCommand(), c.getOriginalId());
				cmd.getParameters().addAll(c.getParameters());
				list.add(cmd);
			}
		}
		
		mHandler.sendCommands(identifier, macAddress, list);
		list.clear();
		
		return true;
	}

    @Override
    public SyncFile syncEventVideoFile(String eventId, String nodeId, String fileName) throws TException {
		IServerSyncFile file = mHandler.syncEventVideoFile(eventId, nodeId, fileName);
		SyncFile syncFile = new SyncFile();
		
		if (file != null)
			FileTransferServiceImpl.setSyncFile(syncFile, file);
		
		return syncFile;
    }

	@Override
	public boolean channelRequest(IClientChannel channel) {
		return mHandler.beforeProcess(channel);
	}

	@Override
	public boolean addLogFile(String nodeId, String fileID) throws TException {
		return mHandler.addLogFile(nodeId, fileID);
	}

	@Override
	public SyncFile uploadFile(String fileName) throws TException {
		IServerSyncFile file = mHandler.uploadFile(fileName);
		SyncFile syncFile = new SyncFile();
		if (file != null)
			FileTransferServiceImpl.setSyncFile(syncFile, file);
		
		return syncFile;
	}
}
