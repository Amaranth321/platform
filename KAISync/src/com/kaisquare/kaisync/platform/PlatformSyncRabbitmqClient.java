package com.kaisquare.kaisync.platform;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ShutdownChannelGroupException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kaisquare.kaisync.ISyncReadFile;
import com.kaisquare.kaisync.ISyncWriteFile;
import com.kaisquare.kaisync.KAISync;
import com.kaisquare.kaisync.file.SyncFileWrapper;
import com.kaisquare.kaisync.platform.MessagePacket.PacketDataHelper;
import com.kaisquare.kaisync.thrift.FileAction;
import com.kaisquare.kaisync.thrift.SyncFile;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.Utils;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

class PlatformSyncRabbitmqClient implements IPlatformSync, AutoCloseable {
	
	private static final String[] PROTOCOLS = new String[] { "TLSv1.1", "TLSv1.2", "TLSv1" };
	private static final String QUEUE_COMMAND = "kaiup_rpc_queue";
	private static final String QUEUE_EVENT = "event_queue";
	private static final String MQ_EVENT_PUSH = "event_push";
	private static final Logger logger = LogManager.getLogger(PlatformSyncRabbitmqClient.class);
	private ConnectionHolder conn;
	private String host;
	private int port;
	private String keystore;
	private String keypass;
	private String corrId;
	private String username;
	private String password;
	private String protocol;
	private int timeout;
	
	public PlatformSyncRabbitmqClient(String host, int port) throws IOException {
		this(host, port, null, null);
	}
	
	public PlatformSyncRabbitmqClient(String host, int port, String keystore, String keypass) throws IOException {
		this(host, port, keystore, keypass, null, null);
	}
	
	public PlatformSyncRabbitmqClient(String host, int port, String keystore, String keypass, String username, String password) throws IOException
	{
		this(host, port, keystore, keypass, username, password, 60000);
	}
	
	public PlatformSyncRabbitmqClient(String host, int port, String keystore, String keypass, String username, String password, int timeout) throws IOException {
		this.host = host;
		this.port = port;
		this.keystore = keystore;
		this.keypass = keypass;
		this.timeout = timeout;
		setCredentials(username, password);
		if (!ensureConnection())
			throw new IOException("unable to connect to server " + host + ":" + port);
		
		corrId = Utils.getMacAddress(null);
		if (Utils.isStringEmpty(corrId))
			corrId = UUID.randomUUID().toString();
	}

	private synchronized boolean ensureConnection() {
		boolean ret = false;
		try {
			if (conn == null || !conn.isOpen())
			{
				if (conn != null) conn.close();
				if (!Utils.isStringEmpty(protocol))
				{
					try {
						conn = ConnectionHolder.newConnection(protocol, host, port, keystore, keypass, username, password);
						return true;
					} catch (NoSuchAlgorithmException | IOException e) {
					}
				}
				for (int i = 0; i < PROTOCOLS.length; i++)
				{
					try {
						conn = ConnectionHolder.newConnection(PROTOCOLS[i], host, port, keystore, keypass, username, password);
						protocol = PROTOCOLS[i];
						break;
					} catch (NoSuchAlgorithmException | IOException e) {
						AppLogger.e(this, "failed to connect to %s:%d %s (keystore=%s, username=%s): %s",
								host,
								port,
								PROTOCOLS[i],
								keystore,
								username,
								e.getMessage());
						
						if (i == PROTOCOLS.length - 1)
							return false;
						else
							Thread.sleep(1000);
					}
				}
			}
			ret = true;
		} catch (Exception e) {
			AppLogger.e(this, e, "");
		}
		
		return ret;
	}
	
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

	@Override
	public InetSocketAddress getSoftwareUpdateHost() {
		if (!ensureConnection())
			return null;
		
		InetSocketAddress address = null; 
		MessagePacket packet = newPacket(FUNC_SOFTWARE_HOST);
		MQResponse resp = sendAndGetResponse(packet);
		if (resp.isOk())
		{
			Map<String, PacketDataHelper> map = resp.getPacket().toMap();
			address = new InetSocketAddress(map.get("host").readAsString(), map.get("port").readAsInt());
		}
		
		return address;
	}

	@Override
	public List<Command> getCommands(String identifier, String macAddress) {
		if (!ensureConnection())
			return new LinkedList<Command>();
		
		List<Command> commands = new ArrayList<Command>();
		try {
			String queueName = getCommandQueueName(identifier, macAddress);
			conn.getChannel().queueDeclare(queueName, true, false, false, null);
			
			GetResponse resp = null;
			do {
			    Channel channel = conn.getChannel();
				resp = channel.basicGet(queueName, false);
				if (resp != null)
				{
					MessagePacket packet = new MessagePacket(resp.getBody());
					List<Command> cmds = readCommandsFromPacket(packet, channel, resp.getEnvelope().getDeliveryTag());
					commands.addAll(cmds);
					cmds.clear();
					cmds = null;
				}
			} while (resp != null);
		} catch (IOException e) {
			AppLogger.e(this, e, "error during getting commands");
		}
		
//		MessagePacket packet = newPacket(FUNC_GET_COMMAND);
//		packet.put(MessagePacket.FIELD_IDENTIFIER, identifier);
//		packet.put(MessagePacket.FIELD_MACADDRESS, macAddress);
//		
//		MQResponse resp = sendAndGetResponse(packet);
//		if (resp.isOk())
//			commands = readCommandsFromPacket(resp.getPacket());
		
		return commands;
	}
	
	private static String getCommandQueueName(String identifier, String macAddress) {
		return String.format("command-%s-%s", identifier, macAddress.toLowerCase());
	}

	@Override
	public CommandClient bindCommands(String identifier, String macAddress, ICommandReceivedListener listener) throws IOException {
		ConnectionFactory factory = null;
		try {
			factory = ConnectionHolder.getFactory(protocol, host, port, keystore, keypass, username, password);
			return new MQCommandClient(identifier, macAddress, factory, listener);
		} catch (NoSuchAlgorithmException e) {
			for (int i = 0; i < PROTOCOLS.length; i++)
			{
				try {
					factory = ConnectionHolder.getFactory(PROTOCOLS[i], host, port, keystore, keypass, username, password);
					protocol = PROTOCOLS[i];
					return new MQCommandClient(identifier, macAddress, factory, listener);
				} catch (NoSuchAlgorithmException e1) {
				}
			}
			throw new IOException("unable to find proper SSL protocol");
		}
	}

	@Override
	public boolean sendCommands(String identifier, String macAddress, List<Command> commands) {
		if (!ensureConnection())
			return false;
		
		MessagePacket packet = newPacket(FUNC_SEND_COMMAND);
		packet.put(MessagePacket.FIELD_IDENTIFIER, identifier);
		packet.put(MessagePacket.FIELD_MACADDRESS, macAddress);
		packet.put("commandcount", commands.size());
		
		Gson gson = new Gson();
		packet.put("cmds", gson.toJson(commands));
		
		MQResponse resp = sendAndGetResponse(packet);
		return resp.isOk();
	}

	@Override
	public boolean sendClientCommands(String identifier, String macAddress, List<Command> commands) {
		String queueName = getCommandQueueName(identifier, macAddress);
		MessagePacket packet = new MessagePacket();
		packet.put("commandcount", commands.size());
		packet.put("cmds", new Gson().toJson(commands));
		try {
			conn.getChannel().queueDeclarePassive(queueName);
			conn.getChannel().basicPublish("", queueName, MessageProperties.PERSISTENT_BASIC, packet.toBytes());
			conn.awaitForConfirms(timeout);
			return true;
		} catch (Exception e) {
			AppLogger.e(this, e, "");
		}
		return false;
	}

	@Override
	public ISyncWriteFile syncEventVideoFile(String eventId, String nodeId, String fileName) {
		if (!ensureConnection())
			return null;
		
		ISyncWriteFile syncFile = null;
		MessagePacket packet = newPacket(FUNC_SYNC_VIDEO);
		packet.put("eventid", eventId);
		packet.put("nodeid", nodeId);
		packet.put("filename", fileName);
		
		MQResponse resp = sendAndGetResponse(packet);
		if (resp.isOk())
		{
			Map<String, PacketDataHelper> map = resp.getPacket().toMap();
			syncFile = new SyncFileWrapper(readFileMetadata(map), FileAction.WRITE).toWriteFile();
		}
		
		return syncFile;
	}

	@Override
	public boolean addLogFile(String nodeId, String fileID) {
		if (!ensureConnection())
			return false;
		
		MessagePacket packet = newPacket(FUNC_ADD_LOG);
		packet.put("nodeid", nodeId);
		packet.put("fileid", fileID);
		
		MQResponse resp = sendAndGetResponse(packet);
		return resp.isOk();
	}

	@Override
	public ISyncWriteFile uploadFile(String fileName) {		
		if (!ensureConnection())
			return null;
		
		ISyncWriteFile syncFile = null;
		MessagePacket packet = newPacket(FUNC_UPLOAD_FILE);
		packet.put("filename", fileName);
		
		MQResponse resp = sendAndGetResponse(packet);
		if (resp.isOk())
		{
			Map<String, PacketDataHelper> map = resp.getPacket().toMap();
			syncFile = new SyncFileWrapper(readFileMetadata(map), FileAction.WRITE).toWriteFile();
		}
		
		return syncFile;
	}

	@Override
	public String getLatestVersion(DeviceType type) {
		if (!ensureConnection())
			return "";
		
		String version = "";
		MessagePacket packet = newPacket(FUNC_CHECK_LATEST_VERSION);
		packet.put("devicetype", type.toString());
		packet.put("version", "4.3");
		
		MQResponse resp = sendAndGetResponse(packet);
		if (resp.isOk())
		{
			Map<String, PacketDataHelper> map = resp.getPacket().toMap();
			version = map.get("latestversion").readAsString();
		}
		return version;
	}

	@Override
	public ISyncReadFile getLatestUpdateFile(DeviceType type) {
		if (!ensureConnection())
			return null;
		
		ISyncReadFile syncFile = null;
		MessagePacket packet = newPacket(FUNC_GET_UPDATE_FILE);
		packet.put("devicetype", type.toString());
		packet.put("version", "4.3");
		
		MQResponse resp = sendAndGetResponse(packet);
		if (resp.isOk())
		{
			Map<String, PacketDataHelper> map = resp.getPacket().toMap();
			syncFile = new SyncFileWrapper(readFileMetadata(map), FileAction.READ).toReadFile();
		}
		
		return syncFile;
	}

	@Override
	public UpdateFileInfo getLatestUpdateFile(String identifier, String modelId) {
		if (!ensureConnection())
			return null;
		
		UpdateFileInfo info = null;
		ISyncReadFile syncFile = null;
		MessagePacket packet = newPacket(FUNC_GET_LATEST_UPDATE_FILE);
		packet.put(MessagePacket.FIELD_IDENTIFIER, identifier);
		packet.put("model", modelId);
		
		MQResponse resp = sendAndGetResponse(packet);
		if (resp.isOk())
		{
			Map<String, PacketDataHelper> map = resp.getPacket().toMap();
			syncFile = new SyncFileWrapper(readFileMetadata(map), FileAction.READ).toReadFile();
			info = new UpdateFileInfoWrapper(map.get("version").readAsString(), map.get("model").readAsString(), syncFile);
		}
		
		return info;
	}

	@Override
	public boolean pushEvent(byte[] message) {
		return pushEvent(MQ_EVENT_PUSH, QUEUE_EVENT, message);
	}
	
	@Override
	public boolean pushEvent(String target, String key, byte[] message) {
		if (!ensureConnection())
			return false;
		
		boolean ret = false;
		try {
			try {
				conn.getChannel().exchangeDeclarePassive(target);
			} catch (IOException | ShutdownChannelGroupException e) {
				conn.getChannel().exchangeDeclare(target, "fanout", true, false, null);
			}
			
			conn.getChannel().basicPublish(target, key, MessageProperties.PERSISTENT_BASIC, message);
			conn.awaitForConfirms(timeout);
			ret = true;
		} catch (IOException | InterruptedException | TimeoutException e) {
			AppLogger.e(this, e, "error pushing event");
		};
		return ret;
	}

	@Override
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	@Override
	public boolean validate(String identifier, String macAddress) throws IOException {
		if (!ensureConnection())
			throw new IOException("Unable to connect server");
		
		boolean ret = false;
		MessagePacket packet = newPacket(FUNC_VALIDATE_CLIENT);
		packet.put(MessagePacket.FIELD_IDENTIFIER, identifier);
		packet.put(MessagePacket.FIELD_MACADDRESS, macAddress);
		logger.info("FIELD_MACADDRESS:"+macAddress);
		logger.info("FIELD_IDENTIFIER:"+identifier);
		MQResponse resp = sendAndGetResponse(packet);
		logger.info(resp.status);
		if (resp.isOk())
		{
			Map<String, PacketDataHelper> map = resp.getPacket().toMap();
			ret = map.get("valid").readAsInt() == 1;
		}
		else
			throw new IOException("Unable to get response from cloud, status=" + resp.getStatus());
			
		return ret;
	}

	@Override
	public boolean sendMessage(String target, String type, MessagePacket message) {
	    if (!ensureConnection())
	        return false;
	    
	    try {
	        conn.getChannel().basicPublish(target, type, MessageProperties.BASIC, message.toBytes());
	        conn.awaitForConfirms();
		
		return true;
	    } catch (Exception e) {
	        AppLogger.e(this, e, "");
	    }
	    
	    return false;
	}

	@Override
	public void close() {
		if (conn != null)
			conn.close();
	}
	
	public void release()
	{
	    if (conn != null)
	        conn.release();
	}

	private SyncFile readFileMetadata(Map<String, PacketDataHelper> map) {
		return new SyncFile(
				map.get("fileid").readAsString(), 
				map.get("length").readAsLong(),
				map.get("host").readAsString(),
				map.get("port").readAsInt(),
				map.get("createdtime").readAsLong(),
				map.get("hash").readAsString());
	}
	
	private MessagePacket newPacket(int func)
	{
		MessagePacket packet = new MessagePacket();
		packet.put(MessagePacket.FIELD_FUNC, func);
		
		return packet;
	}
	
	private MQResponse sendAndGetResponse(MessagePacket packet)
	{
		return sendAndGetResponse(packet, 60000);
	}
	
	private MQResponse sendAndGetResponse(MessagePacket packet, int timeout)
	{
		if (!ensureConnection())
			return new MQResponse(null);
		
		MessagePacket replyPacket = null;
		MQResponse result = null;
		try {
			byte[] message = packet.toBytes();
			
			String replyQueueName = conn.getChannel().queueDeclare().getQueue();			
			BasicProperties props = new BasicProperties()
										.builder()
										.correlationId(corrId)
										.replyTo(replyQueueName)
										.build();
			conn.getChannel().basicPublish("", QUEUE_COMMAND, props, message);
			conn.awaitForConfirms(timeout);
			
			conn.getChannel().basicQos(1);
			QueueingConsumer consumer = new QueueingConsumer(conn.getChannel());
			conn.getChannel().basicConsume(replyQueueName, false, consumer);
			QueueingConsumer.Delivery delivery = null;
			if (timeout > 0)
				delivery = consumer.nextDelivery(timeout);
			else
				delivery = consumer.nextDelivery();
			
			if (delivery != null && delivery.getProperties() != null
					&& delivery.getProperties().getCorrelationId().equals(corrId))
			{
				conn.getChannel().basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				replyPacket = new MessagePacket(delivery.getBody());
			}
			else if (delivery != null && delivery.getProperties() != null)
				AppLogger.e(this, "CorrelationId is not equal: it should be '%s', but '%s'", 
						corrId, delivery.getProperties().getCorrelationId());
			else
				AppLogger.d(this, "unable to get message from cloud, request timeout");
			
			conn.getChannel().basicCancel(consumer.getConsumerTag());
		} catch (InterruptedException e) {
		} catch (TimeoutException e) {
			AppLogger.e(this, "timeout: waiting response for queue");
		} catch (ShutdownSignalException e) {
		} catch (Exception e) {
			AppLogger.e(this, e, "error");
		} finally {
			result = new MQResponse(replyPacket);
		}
		
		return result;
	}
	
	protected static List<Command> readCommandsFromPacket(MessagePacket packet)
	{
	    return readCommandsFromPacket(packet, null, 0);
	}
	
	protected static List<Command> readCommandsFromPacket(MessagePacket packet, Channel channel, long deliveryTag)
	{
		Map<String, PacketDataHelper> map = packet.toMap();
		int count = map.get("commandcount").readAsInt();
		String cmds = map.get("cmds").readAsString();
		List<Command> commands, mqCommands = new ArrayList<Command>();
		
		if (!Utils.isStringEmpty(cmds))
			commands = new Gson().fromJson(cmds, new TypeToken<ArrayList<Command>>(){}.getType());
		else
		{
			commands = new ArrayList<Command>(count);
			for (int i = 0; i < count; i++)
			{
				Object obj = MessagePacket.readObject(map.get("c" + i).getRaw());
				if (obj != null && obj instanceof Command)
					commands.add((Command)obj);
			}
		}
		
		if (commands != null)
		{
		    for (Command c : commands)
		    {
		        MQCommand mqCommand = new MQCommand(c.getId(), c.getCommand(), c.getOriginalId(), channel, deliveryTag);
		        mqCommand.getParameters().addAll(c.getParameters());
		        mqCommands.add(mqCommand);
		    }
		}
		
		return mqCommands;
	}
	
	static class MQCommand extends Command
	{
        private static final long serialVersionUID = 1L;
        private Channel channel;
        private long deliveryTag;
        
        public MQCommand(String id, String command, String originalId, Channel channel, long deliveryTag)
        {
            super(id, command, originalId);
            
            this.channel = channel;
            this.deliveryTag = deliveryTag;
        }
        
        @Override
        public synchronized void ack()
        {
            if (channel != null)
            {
                try {
                    channel.basicAck(deliveryTag, false);
                } catch (Exception e) {
                    AppLogger.w(this, "unable to ack command %s, %d", this, deliveryTag);
                }
            }
        }

        @Override
        public synchronized void nack()
        {
            if (channel != null)
            {
                try {
                    channel.basicNack(deliveryTag, false, true);
                } catch (Exception e) {
                    AppLogger.w(this, "unable to n-ack command %s, %d", this, deliveryTag);
                }
            }
        }
	    
	}
	
	static class MQResponse
	{	
		private MessagePacket packet;
		private int version = -1;
		private int status = -1;
		private String reason;
		
		public MQResponse(MessagePacket packet)
		{
			this.packet = packet;
			if (packet != null)
			{
				Iterator<Entry<String, PacketDataHelper>> iterator = packet.iterator();
				while (iterator.hasNext())
				{
					Entry<String, PacketDataHelper> entry = iterator.next();
					if (entry.getKey().equals(MessagePacket.FIELD_VERSION))
						version = entry.getValue().readAsInt();
					else if (entry.getKey().equals(MessagePacket.FIELD_STATUS))
						status = entry.getValue().readAsInt();
					else if (entry.getKey().equals(MessagePacket.FIELD_REASON))
						reason = entry.getValue().readAsString();
					
					if (version != -1 && status != -1)
					{
						if (status == 0)
							break;
						else if (reason != null)
							break;
					}
				}
			}
		}
		
		public MessagePacket getPacket()
		{
			return packet;
		}
		
		public int getVersion()
		{
			return version;
		}
		
		public int getStatus()
		{
			return status;
		}
		
		public boolean isOk()
		{
			return getStatus() == MessagePacket.STATUS_OK;
		}
		
		public String getReason()
		{
			return reason;
		}
		
		
	}
	
	static class MQCommandClient implements CommandClient, Runnable
	{
		private String identifier;
		private String macAddress;
		private ConnectionFactory factory;
		private Connection conn;
		private Channel channel;
		private Thread thread;
		private Object lock = new Object();
		private ICommandReceivedListener listener;
		private volatile boolean closed;
		private volatile boolean isReady;
		
		public MQCommandClient(String id, String macAddress, ConnectionFactory factory, ICommandReceivedListener listener)
		{
			this.identifier = id;
			this.macAddress = macAddress;
			this.factory = factory;
			this.listener = listener;
		}
		
		public void start()
		{
			synchronized (lock) { 
				if (thread == null)
				{
					closed = false;
					thread = new Thread(this, "MQCommandClient-" + identifier);
					thread.setDaemon(true);
					thread.start();
				}
			}
		}

		@Override
		public void awaitReady(long timeout) throws TimeoutException, InterruptedException {
			while (!isReady)
			{
				synchronized (lock) {
					lock.wait(timeout > 0 ? timeout : 2000);
				}
	
				if (closed)
					throw new InterruptedException();
				else if (!isReady && timeout > 0)
					throw new TimeoutException();
			}
		}
		
		@Override
		public void run()
		{
			String queueName = getCommandQueueName(identifier, macAddress);
			HashMap<String, Object> properties = new HashMap<String, Object>();
			properties.put("client", "bindCommand");
			properties.put("queue", queueName);
			properties.put("client", "kaisync-" + KAISync.getVersion());
			factory.setClientProperties(properties);
			factory.setConnectionTimeout(60000);
			factory.setRequestedHeartbeat(60);
			factory.setShutdownTimeout(20000);
			int delay = 3000;
			
			while (!closed)
			{
				try {
					conn = factory.newConnection();
					channel = conn.createChannel();
					channel.queueDeclare(queueName, true, false, false, null);
					
					int consumers = channel.queueDeclarePassive(queueName).getConsumerCount();
					if (consumers > 0)
					{
						AppLogger.w(this, "the queue '%s' already has %d consumer(s)", queueName, consumers);
						throw new ConsumerExistsException("consumer exists (count=" + consumers + ")");
					}
					
					channel.basicQos(1);
					QueueingConsumer consumer = new QueueingConsumer(channel);
					channel.basicConsume(queueName, false, consumer);
					
					isReady = true;
					synchronized (lock) {
						lock.notifyAll();
					}
					
					while (!closed)
					{
						try {
							QueueingConsumer.Delivery delivery = consumer.nextDelivery();
							if (delivery != null)
							{
								boolean success = false;
								MessagePacket packet = new MessagePacket(delivery.getBody());
								if (listener != null)
									success = listener.onCommandReceived(identifier, macAddress, readCommandsFromPacket(packet));
								
								if (success)
									channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
								else
									channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
							}
						} catch (ConsumerCancelledException | ShutdownSignalException | InterruptedException e) {
							AppLogger.d(this, e, "MQ connection has shut down");
							break;
						} catch (Exception e) {
							AppLogger.d(this, e, "unexpected error");
						}
					}
					delay = 3000;
				} catch (ConsumerExistsException e) {
					delay = 15000;
				} catch (Exception e) {
					AppLogger.e(this, e, "");
					delay = 3000;
				} finally {
					closeConn();
				}
				if (!closed)
				{
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e1) {
						break;
					}
				}
			}
		}

		private synchronized void closeConn() {
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
		}

		@Override
		public void close() {
			synchronized (lock) {
				closed = true;
				lock.notifyAll();
				if (thread != null)
				{
					closeConn();
					try {
						thread.join(30000);
						thread.interrupt();
					} catch (Exception e) {
					}
					thread = null;
				}
			}
		}
		
	}
	
	static class ConsumerExistsException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public ConsumerExistsException() {
			super();
		}

		public ConsumerExistsException(String message, Throwable cause,
				boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public ConsumerExistsException(String message, Throwable cause) {
			super(message, cause);
		}

		public ConsumerExistsException(String message) {
			super(message);
		}

		public ConsumerExistsException(Throwable cause) {
			super(cause);
		}
		
	}
}
