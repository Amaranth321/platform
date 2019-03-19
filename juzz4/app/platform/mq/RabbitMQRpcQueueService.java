package platform.mq;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.kaisquare.kaisync.file.IServerSyncFile;
import com.kaisquare.kaisync.platform.Command;
import com.kaisquare.kaisync.platform.*;
import com.kaisquare.kaisync.platform.MessagePacket.PacketDataHelper;
import com.kaisquare.sync.ITaskResult;
import com.kaisquare.sync.NodeCommandState;
import com.kaisquare.sync.TaskManager;
import com.kaisquare.sync.interceptor.RemoteShellInterceptor;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.*;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.NodeCommand;
import models.RemoteShellState;
import platform.DeviceManager;
import platform.mq.ResultCounting.ResultCheckDelegate;
import platform.services.MessageAckFuture;
import platform.services.ResultStatistics;
import play.Logger;
import play.jobs.Job;
import play.utils.PThreadFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class RabbitMQRpcQueueService implements QueueService, ResultCheckDelegate, ResultStatistics {
	
	public static final String QUEUE_NAME = "kaiup_rpc_queue";
	private ResultCounting result;
	private Connection conn = null;
	private Channel channel = null;
	private IPlatformSyncHandler handler;
	private volatile boolean quit = false;

	public RabbitMQRpcQueueService(IPlatformSyncHandler handler)
	{
		this.handler = handler;
	}

	@Override
	public void run() {
		result = new ResultCounting(this);
		result.start();
		
		ConnectionFactory factory = MQConnection.getMQFactory();
		int cores = Runtime.getRuntime().availableProcessors() + 1;
		ExecutorService mqEs = Executors.newFixedThreadPool(cores);
		ExecutorService worker = Executors.newFixedThreadPool(cores, new PThreadFactory("MQ-RPC"));
		final Semaphore lock = new Semaphore(cores + 1);
		
		HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put("client", "MQRpc");
		properties.put("queue", QUEUE_NAME);
		factory.setClientProperties(properties);
		try {
			conn = factory.newConnection(mqEs);
			channel = conn.createChannel();
			channel.queueDeclare(QUEUE_NAME, false, false, true, null);
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicQos(cores);
			channel.basicConsume(QUEUE_NAME, true, consumer);
			
			while (!quit)
			{
				final QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				
				lock.acquire();
				worker.execute(new Runnable() {

					@Override
					public void run() {
						try {
							final MessagePacket packet = new MessagePacket(delivery.getBody());
							final BasicProperties props = delivery.getProperties();
							final BasicProperties replyProps = new BasicProperties()
											.builder()
											.correlationId(props.getCorrelationId())
											.build();
							final Map<String, PacketDataHelper> map = packet.toMap();
							final int funcID = map.get(MessagePacket.FIELD_FUNC).readAsInt();
							final MessagePacket replyPacket = new MessagePacket();
							
							try {
								replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_OK);
								replyPacket.put(MessagePacket.FIELD_REASON, "");
								
								processMessage(funcID, map, replyPacket);
							} catch (Exception e) {
								Logger.error(e, "error processing message: funcID=%d", funcID);
								replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_ERROR);
								replyPacket.put(MessagePacket.FIELD_REASON, e.getMessage());
							}
							try {
								channel.basicPublish("", props.getReplyTo(), replyProps, replyPacket.toBytes());
							} catch (Exception e) {
								Logger.error("error processing message: %s", e.getMessage());
							}		
						} catch (Exception e) {
							Logger.error(e, "error running RPC request");
						} finally {
							lock.release();
						}
					}
					
				});
			}
		} catch (InterruptedException e) {
		} catch (ShutdownSignalException e) {
			//rabbitmq received shutdown
			Logger.error("RabbitMQ ShutdownSignalException: %s", e.getMessage());
		} catch (ConnectException e) {
			Logger.error("RabbitMQ ConnectException: %s", e.getMessage());
		} catch (Exception e) {
			Logger.error(e, "error processing queue message");
		} finally {
			if (channel != null)
			{
				try {
					channel.close();
				} catch (Exception e) {}
				try {
					conn.close();
				} catch (Exception e) {}
				channel = null;
				conn = null;
			}
		}
		result.quit();
		mqEs.shutdownNow();
		worker.shutdownNow();
	}
	
	private void processMessage(int funcID, Map<String, PacketDataHelper> map, MessagePacket replyPacket) throws InterruptedException
	{
		List<Command> commands;
		IServerSyncFile syncFile;
		String identifier;
		String macAddress;
		String sourceVersion = map.get("version").readAsString();
		double clientVersion = 4.3;
		if (!"".equals(sourceVersion))
			clientVersion = Double.parseDouble(sourceVersion);
		
		Datastore ds;
		switch (funcID)
		{
		case IPlatformSync.FUNC_SOFTWARE_HOST:
			InetSocketAddress addr = handler.getSoftwareUpdateHost();
			replyPacket.put("host", addr.getHostString());
			replyPacket.put("port", addr.getPort());
			break;
		case IPlatformSync.FUNC_ADD_LOG:
			if (!handler.addLogFile(map.get("nodeid").readAsString(), map.get("fileid").readAsString()))
				replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_FAILED);
			break;
		case IPlatformSync.FUNC_GET_COMMAND:
			commands = handler.getCommands(map.get("identifier").readAsString(), map.get("macaddress").readAsString());
			replyPacket.put("commandcount", commands.size());
			replyPacket.put("cmds", new Gson().toJson(commands));
			break;
		case IPlatformSync.FUNC_SEND_COMMAND:
			identifier = map.get(MessagePacket.FIELD_IDENTIFIER).readAsString();
			macAddress = map.get(MessagePacket.FIELD_MACADDRESS).readAsString().toLowerCase();
			int count = map.get("commandcount").readAsInt();

			List<Command> cmds;
			if (map.containsKey("cmds"))
			{
				Logger.debug("client '%s' uses json format commands", identifier);
				Gson gson = new Gson();
				cmds = gson.fromJson(map.get("cmds").readAsString(), new TypeToken<ArrayList<Command>>(){}.getType());
			}
			else
			{
				cmds = new ArrayList<Command>();
				for (int i = 0; i < count; i++)
				{
					try {
						cmds.add((Command)MessagePacket.readObject(map.get("c" + i).getRaw()));
					} catch (Exception e) {
						Logger.error(e, "");
					}
				}
			}
			
			if (!TaskManager.getInstance().processCommand(identifier, macAddress, cmds))
				replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_FAILED);
			
//			for (Command c : cmds)
//			{
//				NodeCommand cmd;
//				if (!Utils.isStringEmpty(c.getOriginalId()))
//					result.putFuture(new StateChangeJob(c).now());
//				else
//				{
//					cmd = new NodeCommand(identifier, macAddress, CommandType.valueOf(c.getCommand()), true);
//					cmd.getParameters().addAll(c.getParameters());
//					cmd.setSourceId(c.getId());
//					List<Future<Object>> futures = TaskManager.getInstance().notifyTask(cmd);
//					for (Future<Object> f : futures)
//						result.putFuture(f);
//				}
//			}
			break;
		case IPlatformSync.FUNC_SYNC_VIDEO:
			syncFile = handler.syncEventVideoFile(
					map.get("eventid").readAsString(),
					map.get("nodeid").readAsString(),
					map.get("filename").readAsString());
			if (syncFile != null)
				replyPacket.put(syncFile);
			else
				replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_FAILED);
			break;
		case IPlatformSync.FUNC_UPLOAD_FILE:
			syncFile = handler.uploadFile(map.get("filename").readAsString());
			if (syncFile != null)
				replyPacket.put(syncFile);
			else
				replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_FAILED);
			break;
		case IPlatformSync.FUNC_CHECK_LATEST_VERSION:			
			String version = handler.getLatestVersion(DeviceType.valueOf(map.get("devicetype").readAsString()), clientVersion);
			replyPacket.put("latestversion", version);
			break;
		case IPlatformSync.FUNC_GET_UPDATE_FILE:
			syncFile = handler.getLatestUpdateFile(DeviceType.valueOf(map.get("devicetype").readAsString()), clientVersion);
			if (syncFile != null)
				replyPacket.put(syncFile);
			else
				replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_FAILED);
			break;
		case IPlatformSync.FUNC_GET_LATEST_UPDATE_FILE:
			final String deviceId= map.get(MessagePacket.FIELD_IDENTIFIER).readAsString();
			final String model = map.get("model").readAsString();
			IServerUpdateFileInfo info = null;
			try {
				info = handler.getLatestUpdateFile(deviceId, model);
				
				if (info != null)
				{
					replyPacket.put("version", info.getVersion());
					replyPacket.put("model", info.getModel());
					replyPacket.put(info.getFile());
				}
				else
					replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_FAILED);
			} catch (Exception e) {
				Logger.error(e,  "");
				replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_ERROR);
			}
			
			break;
		case IPlatformSync.FUNC_VALIDATE_CLIENT:
			identifier = map.get(MessagePacket.FIELD_IDENTIFIER).readAsString();
			macAddress = map.get(MessagePacket.FIELD_MACADDRESS).readAsString().toLowerCase();
			
			try {
				final String macAddressFinal = macAddress;
				MongoDevice device = new Job<MongoDevice>()
				{
					public MongoDevice doJobWithResult()
					{
						try
						{
							return MongoDevice.getByDeviceKey(macAddressFinal);
						}
						catch (Exception e)
						{
							return null;
						}
					}
				}.now().get();

				String queueName = String.format("command-%s-%s", identifier, macAddress);
				Channel queueCheckChannel = null;
				try {
					/*
					 * we check whether the command queue exists or not, if there're messages in the queue,
					 * then the queue may have the rest of commands not sent to the node, e.g CLOUD_DELETE_LICENSE
					 */
					queueCheckChannel = MQConnection.createNewChannel();
					DeclareOk declareOk = queueCheckChannel.queueDeclarePassive(queueName);
					if (declareOk.getMessageCount() > 0)
					{
						replyPacket.put("valid", 1);
						return;
					}
				} catch (IOException e) {
					//command queue doesn't exist
				} finally {
					if (queueCheckChannel != null)
					{
						try {
							queueCheckChannel.close();
						} catch (Exception e) {}
					}
				}

				if (device != null && device.getDeviceId().equals(identifier) && device.getDeviceKey().equalsIgnoreCase(macAddress))
				{
					try {
						channel.queueDeclare(queueName, true, false, false, null);
						replyPacket.put("valid", 1);
					} catch (Exception e) {
						Logger.error(e, "failed to create queue '%s'", queueName);
						replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_ERROR);
					}
				}
				else
					replyPacket.put("valid", 0);
				
			} catch (ExecutionException e) {
				replyPacket.put(MessagePacket.FIELD_STATUS, MessagePacket.STATUS_ERROR);
			}
			
			break;
		case IPlatformSync.FUNC_REGISTER_REMOTE_SHELL:
			identifier = map.get(MessagePacket.FIELD_IDENTIFIER).readAsString();
			macAddress = map.get(MessagePacket.FIELD_MACADDRESS).readAsString().toLowerCase();
			
			if (!"".equals(macAddress))
			{
				String rsQueue = String.format("remote-shell-%s", macAddress);
				try {
					channel.exchangeDeclare(RemoteShellInterceptor.EXCHANGE_NAME, "topic", true);
					channel.queueDeclare(rsQueue, true, false, false, null);
					channel.queueBind(rsQueue, RemoteShellInterceptor.EXCHANGE_NAME, macAddress);
					ds = RemoteShellState.ds();
					Query<RemoteShellState> q = ds.createQuery(RemoteShellState.class);
					q.field("macAddress").equal(macAddress);
					
					UpdateOperations<RemoteShellState> ops = ds.createUpdateOperations(RemoteShellState.class);
					if (!"".equals(identifier))
						ops.set("cloudPlatformDeviceId", identifier);
					ops.set("registered", true);
					ds.findAndModify(q, ops, true, true);
					
					replyPacket.put("available", 1);
					replyPacket.put("queue", rsQueue);
				} catch (IOException e) {
					Logger.error("failed to create queue %s for remote shell", rsQueue);
					replyPacket.put("available", 0);
				}
			}
			else
				replyPacket.put("available", 0);
			break;
		default:
			Logger.error("RPC process: unknown function ID %d", funcID);
		}
	}

	@Override
	public void setPrefetch(int prefetch) {
	}

	@Override
	public void setAutoAck(boolean autoAck) {
	}

	@Override
	public synchronized void close() {
		quit = true;
		if (channel != null)
		{
			try {
				channel.close();
			} catch (Exception e) {}
		}
		if (conn != null)
		{
			try {
				conn.close();
			} catch (Exception e) {}
		}
		channel = null;
		conn = null;
	}

	@Override
	public boolean checkResult(Object obj) {
		if (obj instanceof ITaskResult)
		{
			ITaskResult result = (ITaskResult) obj;
			return ((Boolean)result.getResult());
		}
		else if (obj instanceof Boolean)
			return (Boolean)obj;
		else
			return false;
	}
	
	@Override
	public void onException(Future f, Throwable e)
	{
		if (f instanceof MessageAckFuture)
			((MessageAckFuture)f).getMessageAck().notAck();
	}

	@Override
	public int getTotalCount() {
		return result.getTotalCount();
	}

	@Override
	public int getSuccessCount() {
		return result.getSuccessCount();
	}

	@Override
	public int getFailCount() {
		return result.getFailCount();
	}

	@Override
	public double getAverage() {
		return result.getAverage();
	}

	@Override
	public long getLastProcessTime() {
		return result.getLastProcessTime();
	}
	
	static class StateChangeJob extends Job
	{
		private Command cmd;
		
		public StateChangeJob(Command cmd)
		{
			this.cmd = cmd;
		}

		@Override
		public Object doJobWithResult() throws Exception {
			NodeCommand nodeCommand = NodeCommand.find("_id", cmd.getOriginalId()).first();
			if (nodeCommand != null)
			{
				NodeCommandState state = NodeCommandState.valueOf(cmd.getParameters().get(0));
				TaskManager.getInstance().changeCommandState(state, nodeCommand);
			}
			cmd = null;
			
			return Boolean.valueOf(true);
		}
	}
}
