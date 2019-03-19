package platform.services;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.NodeCommand;
import models.node.NodeInfo;
import platform.CloudSyncManager;
import platform.Environment;
import platform.config.readers.ConfigsServers;
import platform.node.NodeManager;
import play.Logger;
import play.Play;
import play.services.Service;

import com.kaisquare.kaisync.platform.Command;
import com.kaisquare.kaisync.platform.CommandClient;
import com.kaisquare.kaisync.platform.ICommandReceivedListener;
import com.kaisquare.kaisync.platform.IPlatformSync;
import com.kaisquare.sync.NodeCommandState;
import com.kaisquare.sync.TaskManager;
import com.kaisquare.sync.Utils;
import com.kaisquare.util.SysInfoUtil;

public class CloudSyncService extends Service implements ICommandReceivedListener {
	
	private static final Object PROCESS_LOCK = new Object();
	private static final int MAX_READ_COMMAND = 10;
	private Object lock = new Object();
	private List<Command> pendingCommands = new ArrayList<Command>();
	
	private String macAddress;
    
    private IPlatformSync client = null;
	private NodeInfo node = null;
	
	private String lastCommandId;
	private String lastCommandType;
	private long lastCommandSent;
	
	public CloudSyncService()
	{
        macAddress = SysInfoUtil.getMacAddress(true);
	}

	@Override
	public boolean isMutex() {
		return false;
	}

	@Override
	protected boolean isJPARequired() {
		return true;
	}

	@Override
	protected void startService() {
		if (Environment.getInstance().onKaiNode())
		{
			CommandClient cmdClient = null;
			while (Play.started)
			{
				try {
					if (client == null)
						client = CloudSyncManager.getInstance().getPlatformSync();
					
					if (node == null)
					{
						node = NodeManager.getInstance().getNodeInfo();
		                if (node == null || Utils.isStringEmpty(node.getCloudPlatformDeviceId()))
                        {
                            //node is not registered yet. wait and recheck
                            synchronized (lock) {
                                try {
                                    lock.wait(15000);
                                } catch (InterruptedException e1) {
                                }
                                continue;
                            }
                        }
		                
		                //we have to send this 'validate()' to cloud, if node does exist on the cloud,
		                //then cloud will create a queue to this node for commands 
		                if (!client.validate(node.getCloudPlatformDeviceId(), macAddress))
		                {
		                	node = null;
		                	throw new CloudSyncException("node is invalid on cloud " + CloudSyncManager.getInstance().getCloudSyncServerHost());
		                }
					}
	                
					if (cmdClient == null)
					{
		                cmdClient = client.bindCommands(node.getCloudPlatformDeviceId(), macAddress, this);
		                cmdClient.start();
					}
					cmdClient.awaitReady(0);
	
	                //send pending commands
	                if (!pendingCommands.isEmpty() && !client.sendCommands(
                            node.getCloudPlatformDeviceId(), macAddress, pendingCommands))
	                    throw new CloudSyncException("unable to send commands");
	                else
	                    pendingCommands.clear();
	
	                //send finish tasks
	                responseCommands(client, node.getCloudPlatformDeviceId());
	
	                //send command to cloud
	                sendCommands(client, node.getCloudPlatformDeviceId());
	                synchronized (lock) {
	                	lock.wait(1000);
	                }
				} catch (CloudSyncException e) {
					Logger.warn("CloudSyncException: %s", e.getMessage());
					synchronized (lock) {
						try {
							lock.wait(10000);
						} catch (InterruptedException e1) {
							break;
						}
					}
				} catch (Exception e) {
					if (client != null)
					{
						client.close();
						client = null;
					}
					Logger.error(e, "error during command sync: %s", e.getMessage());
					synchronized (lock) {
						try {
							lock.wait(5000);
						} catch (InterruptedException e1) {
							break;
						}
					}
				}
			}
			if (cmdClient != null)
				cmdClient.close();
			if (client != null)
				client.close();
		}
		else
			Logger.debug("the platform is not running as Node, CloudSyncService exited.");
	}

	@Override
	public void stopService() {
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	private void responseCommands(IPlatformSync client, String identifier) {
        TaskManager manager = TaskManager.getInstance();
        pendingCommands.clear();
        Iterable<NodeCommand> finishedCommands = manager.getFinishedCommands(identifier, macAddress);
        if (finishedCommands != null) {
            for (NodeCommand cmd : finishedCommands) {
                if (cmd.getResult() == true)
                    cmd.success();
                else
                    cmd.failed();
                Command respCommand = new Command(cmd.getIdAsStr(), cmd.getCommand().toString(), cmd.getSourceId());
                respCommand.getParameters().add(cmd.getState().toString());
                pendingCommands.add(respCommand);
                cmd.delete();
            }
            if (!pendingCommands.isEmpty() && client.sendCommands(identifier, macAddress, pendingCommands))
                pendingCommands.clear();
        }
    }
	
	private void sendCommands(IPlatformSync client, String identifier) {
        TaskManager manager = TaskManager.getInstance();
        synchronized (PROCESS_LOCK) {
	        Iterable<NodeCommand> list = manager.getCommands(identifier, macAddress);
	        List<NodeCommand> loadedCommands = new ArrayList<NodeCommand>();
	        List<Command> commands = new ArrayList<Command>();
	
	        if (list != null) {
	        	int read = 0;
	            for (NodeCommand c : list) {
	                Command cmd = new Command(c.getIdAsStr(), c.getCommand().toString(), c.getSourceId());
	
	                if (c.getParameters() != null && c.getParameters().size() > 0)
	                    cmd.getParameters().addAll(c.getParameters());
	
	                loadedCommands.add(c);
	                commands.add(cmd);
	                read++;
	                
	                if (read >= MAX_READ_COMMAND)
	                	break;
	            }
	            if (!commands.isEmpty() && client.sendCommands(identifier, macAddress, commands))
	            {
	            	lastCommandId = commands.get(commands.size() - 1).getId();
	            	lastCommandType = commands.get(commands.size() - 1).getCommand();
	            	lastCommandSent = System.currentTimeMillis();
	            	for (NodeCommand c : loadedCommands)
	            	{
	            		manager.changeCommandState(NodeCommandState.Sending, c);
	            	}
	            }
	            loadedCommands.clear();
	            commands.clear();
	        }
        }
    }

	@Override
	public boolean onCommandReceived(String identifier, String macAddress, List<Command> commands) {
		boolean ret = false;
		synchronized (PROCESS_LOCK) {
			//process remote commands
	        List<Command> response = new ArrayList<Command>();
	        ret = TaskManager.getInstance().processCommand(identifier, macAddress, commands, response);
	        if (!response.isEmpty())
	        	pendingCommands.addAll(response);
//	            client.sendCommands(identifier, macAddress, response); //return current state of commands
		}
		
		return ret;
	}

	@Override
	public String dump() {
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		out.println("The last command");
		out.println("Id: " + lastCommandId);
		out.println("type: " + lastCommandType);
		out.println("sent at " + (lastCommandSent > 0 ? new Date(lastCommandSent) : "-"));
		
		return sw.toString();
	}
	
	static class CloudSyncException extends Exception
	{

		public CloudSyncException() {
			super();
		}

		public CloudSyncException(String message, Throwable cause,
				boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public CloudSyncException(String message, Throwable cause) {
			super(message, cause);
		}

		public CloudSyncException(String message) {
			super(message);
		}

		public CloudSyncException(Throwable cause) {
			super(cause);
		}
		
	}
	
	static class InvalidClientException extends Exception
	{

		public InvalidClientException() {
			super();
		}

		public InvalidClientException(String message, Throwable cause,
				boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public InvalidClientException(String message, Throwable cause) {
			super(message, cause);
		}

		public InvalidClientException(String message) {
			super(message);
		}

		public InvalidClientException(Throwable cause) {
			super(cause);
		}
		
	}
}
