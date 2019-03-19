package com.kaisquare.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import models.MongoDevice;
import models.NodeCommand;
import models.PendingDeletedQueue;
import platform.DeviceManager;
import platform.Environment;
import platform.mq.CommandMessageQueue;
import platform.mq.CommandPublisher;
import platform.mq.MQConnection;
import play.Logger;
import play.modules.morphia.Model.MorphiaQuery;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.kaisquare.kaisync.platform.Command;
import com.kaisquare.kaisync.rabbitmq.QueueInfo;
import com.kaisquare.kaisync.rabbitmq.RabbitMQAPI;
import com.kaisquare.util.StringCollectionUtil;
import com.rabbitmq.client.ConnectionFactory;

public final class TaskManager {
	
	private static TaskManager mInstance = null;

	private static DefaultNodeCommandStateListener defaultStateListener = new DefaultNodeCommandStateListener();
	private static CommandMessageQueue MQ;
	private static Object staticLock = new Object(); 
	
	private Map<CommandType, List<ITask>> observers = new ConcurrentHashMap<CommandType, List<ITask>>();
	private TaskLooper taskLooper = new TaskLooper();
	private List<ICommandStateListener> commandStateListener = Collections.synchronizedList(new ArrayList<ICommandStateListener>());
	
	private TaskManager() {
		addCommandStateListener(defaultStateListener);
	}
	
	/**
	 * Check whether there're any pending commands to be sent
	 */
	public synchronized void checkPendingCommands() {
		MorphiaQuery query = NodeCommand.q();
		query.field("state").notIn(Arrays.asList(
				NodeCommandState.Success, NodeCommandState.Failed, NodeCommandState.Cancel));
		Iterable<NodeCommand> commands = query.fetch();
		for (NodeCommand command : commands)
		{
			CommandPublisher publisher = new CommandPublisher(getCommandQueue(command.getNodeId(), command.getMacAddress()));
			int messages = publisher.queueMessages();
			boolean queueExist = messages >= 0;
			if (queueExist) //the queue is there, the node version is v4.3 above, if not then we keep it for v4.2 command mechanism
			{
				boolean sent = false;
				NodeCommandState state = command.getState();
				if (state == NodeCommandState.Pending || state == NodeCommandState.Responding)
				{
					Command cmd = new Command(command.getIdAsStr(), command.getCommand().toString(), command.getSourceId());
					cmd.getParameters().addAll(command.getParameters());
					sent = publisher.publish(cmd);
				}
				if (sent)
				{
					switch (state)
					{
					case Pending:
						changeCommandState(NodeCommandState.Sending, command);
						break;
					case Sending:
						break;
					default:
						command.delete();
						break;
					}
				}
			}
			publisher.close();

			MongoDevice device = MongoDevice.getByPlatformId(command.getNodeId());
			if (device == null)
			{
				command.delete();
				continue;
			}
		}
	}

	public List<ITask> getTasks(NodeCommand command)
	{
		return observers.get(command.getCommand());
	}
	
	public List<Future<Object>> notifyTask(NodeCommand command)
	{
		List<Future<Object>> futures = new ArrayList<Future<Object>>();
		List<ITask> tasks = getTasks(command);
		if (tasks != null)
		{
			for (ITask t : tasks)
				futures.add(newTask(command, t));
		}
		else
		{
			Logger.warn("No tasks for '%s'", command.getCommand());
			futures.add(new NoneTaskResult(command, false));
			command.respond(false);
			command.save();
//			throw new NoneTaskException("No Task for command '" + command.getCommand().toString() + "'");
		}
		
		return futures;
	}
	
	public Future<Object> newTask(NodeCommand command, ITask task)
	{
		return taskLooper.newTask(command, task);
	}
	
	/**
	 * Add a listener for handling command state
	 * it can add many different listeners, but all the listener will be triggered from each state changed commands
	 * so don't need to add listener for each commands, just use one listener to handle all of sent commands
	 * @param listener {@link ICommandStateListener} instance
	 */
	public void addCommandStateListener(ICommandStateListener listener)
	{
		if (listener != null && !commandStateListener.contains(listener))
			commandStateListener.add(listener);
	}
	
	/**
	 * Remove a listener from {@link TaskManager}
	 * @param listener listener to remove
	 */
	public void removeCommandStateListener(ICommandStateListener listener)
	{
		if (listener != null && commandStateListener.contains(listener))
			commandStateListener.remove(listener);
	}
	
	private void triggerStateChanged(NodeCommand command, NodeCommandState state)
	{
		for (ICommandStateListener listener : commandStateListener)
		{
			listener.onCommandStateChanged(command, state);
		}
	}

	/**
	 * Send command to target 'nodeId', the command will be pending until network is available
	 * @param nodeId target to send
	 * @param macAddress the MAC address of node
	 * @param cmd which command will send to the target
	 * @param params parameters of command
	 * @return a future object {@link NodeCommandFuture}
	 */
	public NodeCommandFuture sendCommand(String nodeId, String macAddress, CommandType cmd, String...params)
	{
		NodeCommand command = new NodeCommand(nodeId, macAddress, cmd);
		int length = params.length;
		if (length == 0)
			command.getParameters().add("");
		for (int i = 0; i < length; i++)
			command.getParameters().add(params[i]);
		
		return sendCommand((NodeCommand)command.save());
	}
	
	/**
	 * Send command to target 'nodeId', the command will be pending until network is available
	 * @param command {@link NodeCommand} will be sent
	 * @return
	 */
	protected NodeCommandFuture sendCommand(NodeCommand command)
	{
		NodeCommandFuture f = new NodeCommandFuture(command.getIdAsStr());
		//on cloud, we have to push the message to the queue that Node is binding
		if (Environment.getInstance().onCloud())
		{
			if (command.getCommand().getInterceptor() != null && command.getCommand().getInterceptor().intercept(command))
			{
				//command is sent via the different way
			}
			else
			{
				Command cmd = new Command(command.getIdAsStr(), command.getCommand().toString(), command.getSourceId());
				cmd.getParameters().addAll(command.getParameters());
				sendCommand(command.getNodeId(), command.getMacAddress(), cmd);
			}
		}
		return f;
	}
	
	boolean sendCommand(String nodeId, String macAddress, Command command)
	{
		boolean ret = false;
		if (Environment.getInstance().onCloud())
		{
			String queueName = getCommandQueue(nodeId, macAddress);
			CommandPublisher publisher = new CommandPublisher(queueName);
			try {				
				if (publisher.queueMessages() >= 0)
				{
					if (publisher.publish(command))
					{
						ret = true;
						NodeCommand cmd = NodeCommand.find("_id", command.getId()).get();
						if (cmd != null)
							changeCommandState(NodeCommandState.Sending, cmd);
					}
				}
				else
					Logger.debug("command queue '%s' not exists", queueName);
				
			} catch (Exception e) {
				Logger.error(e, "failed to publish message for node %s, %s, %s", nodeId, macAddress, command.getCommand());
			}
			publisher.close();
			command.getParameters().clear();
		}
		
		return ret;
	}
	
	/**
	 * Cancel a pending command
	 * @param commandId {@link NodeCommand} id
	 * @return
	 */
	public boolean cancelCommand(String commandId)
	{
		NodeCommand command = NodeCommand.find("_id", commandId).first();
		if (command != null && command.getState() == NodeCommandState.Pending)
		{
			command.delete();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Get pending commands
	 * @param nodeId
	 * @param macAddress the MAC address of the node
	 * @return
	 */
	public Iterable<NodeCommand> getCommands(String nodeId, String macAddress)
	{
		return getCommands(nodeId, macAddress, NodeCommandState.Pending);
	}
	
	public Iterable<NodeCommand> getFinishedCommands(String nodeId, String macAddress)
	{
		return getCommands(nodeId, macAddress, NodeCommandState.Responding);
	}
	
	private Iterable<NodeCommand> getCommands(String nodeId, String macAddress, NodeCommandState state)
	{
		if ("".equals(macAddress))
			return NodeCommand.find("nodeId state", nodeId, state.toString()).order("createdTime").fetch();
		else
			return NodeCommand.find("nodeId macAddress state", nodeId, macAddress.toLowerCase(), state.toString()).order("createdTime").fetch();
	}
	
	public void changeCommandState(NodeCommandState state, String commandId)
	{
		changeCommandState(state, (NodeCommand)NodeCommand.find("_id", commandId).first());
	}
	
	public void changeCommandState(NodeCommandState state, NodeCommand command)
	{
		if (command != null)
		{
			command.setState(state);
			command = command.save();
			triggerStateChanged(command, state);
		}
	}
	
	public void register(CommandType command, ITask task)
	{
		if (task == null)
			throw new NullPointerException("task");
		
		List<ITask> list = observers.get(command);
		if (list == null)
		{
			list = new ArrayList<ITask>();
			observers.put(command, list);
		}
		
		list.add(task);
//		Logger.info("register task '%s' for command '%s'", task.getClass().getName(), command);
	}
	
	public void unregister(CommandType command, ITask task)
	{
		List<ITask> list = observers.get(command);
		if (list != null)
			list.remove(task);
		
		Logger.info("unregister task '%s' from command '%s'", task.getClass().getName(), command);
	}
	
	public NodeCommandFuture getNodeCommandFuture(String commandId)
	{
		if (commandId == null)
			throw new NullPointerException("an emtpy commandId");
		return new NodeCommandFuture(commandId);
	}
	
	public boolean processCommand(String identifier, String macAddress, List<Command> commands)
	{
		return processCommand(identifier, macAddress, commands, null);
	}
	
	/**
	 * process commands from remote client
	 * @param identifier the id of client
	 * @param macAddress the MAC address of client
	 * @param commands commands from client
	 * @param refCommand this function will add processed commands in this list parameter
	 * @return
	 */
	public boolean processCommand(String identifier, String macAddress, List<Command> commands, List<Command> refCommand)
	{
		boolean ret = false;
		if (commands != null)
		{
			TaskManager manager = getInstance();
			for (Command c : commands)
			{
				if (Logger.isDebugEnabled())
				{
					int maxLength = 500;
					StringBuilder sb = new StringBuilder();
					sb.append("[");
					for (String param : c.getParameters())
					{
						if (sb.length() > 1)
							sb.append(",");
						if (param.length() > maxLength)
							sb.append(param.substring(0, maxLength));
						else
							sb.append(param);
					}
					sb.append("]");
					Logger.debug("Received command: %s %s", c.getCommand(), sb.toString());
				}

				NodeCommand cmd;
				try {
					if (!StringCollectionUtil.isEmpty(c.getOriginalId())) //if it's existing command, update state
					{
						cmd = NodeCommand.find("_id", c.getOriginalId()).first();
						if (cmd != null)
						{
							NodeCommandState state = NodeCommandState.valueOf(c.getParameters().get(0));
							manager.changeCommandState(state, cmd);
						}
						ret = true;
					}
					else { //new command
						CommandType cmdType = CommandType.parse(c.getCommand());
						if (cmdType == null)
						{
							Logger.error("Unrecognized command type (%s, nodeId=%s)", c.getCommand());
							ret = false;
						}
						else
						{
							cmd = new NodeCommand(identifier, macAddress, cmdType, true);
							cmd.getParameters().addAll(c.getParameters());
							cmd.setSourceId(c.getId());
							try {
								openMQ(); //try to open command MQ, then publish to queue
								MQ.publish(cmd);
								ret = true;
							} catch (IOException e) {
								Logger.error(e, "error publishing message to queue, process command directly");
								closeMQ();
							}
						}
					}
				} catch (Exception e) {
					Logger.error(e, "failed to process command '%s:%s' ('%s'), source: '%s'", 
							c.getId(), c.getCommand(), c.getParameters(), c.getOriginalId());
					if (refCommand != null)
					{
						Command resp = new Command("", c.getCommand(), c.getId());
						resp.getParameters().add(NodeCommandState.Failed.toString());
						refCommand.add(resp);
					}
				}
				c.getParameters().clear();
			}
		}
		return ret;
	}
	
	static void openMQ() throws IOException
	{
		if (MQ == null || !MQ.isOpen())
			openMQLocked();
	}
	
	static void openMQLocked() throws IOException
	{
		synchronized (staticLock)
		{
			if (MQ != null)
				MQ.close();
			else
				MQ = new CommandMessageQueue();
			
			try {
				MQ.open();
			} catch (IOException e) {
				try { MQ.close(); } catch (Exception e1) {}
				
				throw e;
			}
		}
	}
	
	static void closeMQ() {
		synchronized (staticLock)
		{
			if (MQ != null)
			{
				try { MQ.close(); } catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Remove all commnads of the node
	 * @param nodeId
	 */
	public void removeCommandsByNodeId(String nodeId, String macAddress)
	{
		Datastore ds = NodeCommand.ds();
		Query<NodeCommand> query = ds.createQuery(NodeCommand.class);
		query.field("nodeId").equal(nodeId);
		ds.delete(query);
		
		removeCommandQueue(nodeId, macAddress, true);
	}
	
	public void removeCommandQueue(String nodeId, String macAddress, boolean force)
	{
		String queueName = getCommandQueue(nodeId, macAddress);
		PendingDeletedQueue pending = PendingDeletedQueue.find("queueName", queueName).get();
		if (pending == null)
		{
			pending = new PendingDeletedQueue();
			pending.setQueueName(queueName);
			pending.setForce(force);
			pending.save();
		}
	}
	
	private static String getCommandQueue(String nodeId, String macAddress) {
		return String.format("command-%s-%s", nodeId, macAddress.toLowerCase());
	}

	/**
	 * Remove all related command type of the node
	 * @param nodeId node id
	 * @param type a given {@link CommandType} to be deleted
	 */
	public void removeCommandsByType(String nodeId, CommandType command)
	{
		Datastore ds = NodeCommand.ds();
		Query<NodeCommand> query = ds.createQuery(NodeCommand.class);
		query.and(
				query.criteria("nodeId").equal(nodeId),
				query.criteria("command").equal(command));
	
		ds.delete(query);
	}

	/**
	 * Clear those commands that have been already responded by remote host
	 */
	public void removeRespondedCommands()
	{
		Datastore ds = NodeCommand.ds();
		Query<NodeCommand> query = ds.createQuery(NodeCommand.class);
		query.or(query.criteria("state").equal(NodeCommandState.Success),
                 query.criteria("state").equal(NodeCommandState.Failed),
				 query.criteria("state").equal(NodeCommandState.Cancel));
		
		ds.delete(query);
	}
	
	public void removeUnusedQueues()
	{
		if (!Environment.getInstance().onCloud())
			return;
		
		Iterable<PendingDeletedQueue> pendingQueues = PendingDeletedQueue.find().fetch();
		for (PendingDeletedQueue queueInfo : pendingQueues)
		{
			CommandPublisher publisher = new CommandPublisher(queueInfo.getQueueName());
			if (publisher.deleteQueue(queueInfo.isForce()))
				queueInfo.delete();
			publisher.close();
		}
		
		ConnectionFactory factory = MQConnection.getMQFactory();
		RabbitMQAPI api = new RabbitMQAPI(factory.getHost(), 15672, factory.getUsername(), factory.getPassword());
		try {
			DeviceManager dm = DeviceManager.getInstance();
			List<QueueInfo> queues = api.listQueues();
			HashMap<String, Long> duplicateQueues = new HashMap<String, Long>();
			for (QueueInfo q : queues)
			{
				if (q.getConsumers() > 0)
					continue;
				
				boolean needToDelete = false;
				if (q.getName().startsWith("command") && !q.getName().equalsIgnoreCase("command_queue"))
				{
					String[] info = q.getName().split("\\-");
					if (!Pattern.matches("^command\\-\\d+\\-\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}$", q.getName()))
						needToDelete = true;
					else
					{
						Long nodeId = Long.parseLong(info[1]);
						String macAddress = info[2].toLowerCase().trim();
						try {
							MongoDevice device = MongoDevice.getByDeviceKey(macAddress);

							//if it has no messages in the queue, then we can consider deleting the queue if there's no specific device id in db
							if (q.getMessages() == 0)
							{
								needToDelete = device == null
										|| Long.parseLong(device.getDeviceId()) == 0
										|| Long.parseLong(device.getDeviceId()) != nodeId.longValue()
										|| !device.getDeviceKey().equals(macAddress);
							}
							//consider a situation that device was removed from db, but the node hasn't got the CLOUD_DELETE_LICENSE yet
							//so only delete the queue when the node id/mac-address is different from the db
							else if (device != null && (Long.parseLong(device.getDeviceId()) > 0 && (Long.parseLong(device.getDeviceId()) != nodeId.longValue())
									|| !device.getDeviceKey().equals(macAddress)))
							{
								needToDelete = true;
							}
							else
							{
								Long previousId = duplicateQueues.get(macAddress);
								if (previousId != null)
								{
									if (nodeId.longValue() > previousId.longValue())
										api.deleteQueue("/", String.format("command-%d-%s", previousId.longValue(), macAddress));
									else
									{
										needToDelete = true;
										nodeId = previousId;
									}
								}
								duplicateQueues.put(macAddress, nodeId);
							}
						} catch (Exception e) {
							Logger.error("unable to check queue '%s' for device key '%s', nodeId '%s': %s", 
									q.getName(), macAddress, nodeId, e.getMessage());
						}
					}
					
					if (needToDelete)
						Logger.info("delete unused queue '%s': %s", q.getName(), api.deleteQueue("/", q.getName()));
				}
			}
			duplicateQueues.clear();
		} catch (Exception e) {
			Logger.warn(e, "rabbitmq management plugin is unavailable, host=%s, user=%s", 
					factory.getHost(),
					factory.getUsername());
		}
	}
	
	public static synchronized DefaultNodeCommandStateListener getDefaultListener()
	{
		return defaultStateListener;
	}
	
	public static synchronized TaskManager getInstance()
	{
		if (mInstance == null)
			mInstance = new TaskManager();
		
		return mInstance;
	}

	private class NoneTaskResult implements Future<Object>, ITaskResult
	{
		private NodeCommand command;
		private boolean result;
		
		private NoneTaskResult(NodeCommand command, boolean result)
		{
			this.command = command;
			this.result = result;
		}

		@Override
		public NodeCommand getCommand() {
			return command;
		}

		@Override
		public Object getResult() {
			return result;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return true;
		}

		@Override
		public boolean isCancelled() {
			return true;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return this;
		}

		@Override
		public Object get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			return this;
		}
		
	}
	
	class NoneTaskException extends RuntimeException
	{

		public NoneTaskException() {
			super();
		}

		public NoneTaskException(String message, Throwable cause,
				boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public NoneTaskException(String message, Throwable cause) {
			super(message, cause);
		}

		public NoneTaskException(String message) {
			super(message);
		}

		public NoneTaskException(Throwable cause) {
			super(cause);
		}
		
	}
}
