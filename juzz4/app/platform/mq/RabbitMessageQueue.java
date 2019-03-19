package platform.mq;

import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import play.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RabbitMQ implementation
 */
public class RabbitMessageQueue implements MessageQueue<byte[]>, QueueWorkerFactory<byte[]> {

	private RabbitMQConnection conn;
	private Channel channel;
	private boolean confirmMode;
	private AtomicBoolean open = new AtomicBoolean(false);
	
	public RabbitMessageQueue(RabbitMQConnection conn)
	{
		this.conn = conn; 
	}
	
	/**
	 * Create work queue of RabbitMQ
	 * @param host message queue server
	 * @param port message queue server port
	 * @return a connection holder {@link ConnectionHolder} which keeps the connection and channel of RabbitMQ
	 * @throws IOException
	 */
	protected Channel createQueueChannel() throws IOException
	{
		Channel channel = conn.createChannel();
		channel.confirmSelect();
		confirmMode = true;
		
		return channel;
	}
	
	private void ensureConnection()
	{
		if (!isOpen())
			throw new RuntimeException("message queue connection is not open.");
	}
	
	@Override
	public void open() throws IOException
	{
		if (!isOpen())
		{
			channel = createQueueChannel();
			open.set(true);
		}
	}
	
	@Override
	public void createQueue(String queueName) throws IOException {
		ensureConnection();
		channel.queueDeclare(queueName, true, false, false, null);
	}
	
	@Override
	public boolean isOpen()
	{
		return open.get() && conn != null && channel != null;
	}
	
	@Override
	public int queueMessages(String queueName)
	{
		ensureConnection();
		int messages = -1;
		try {
			messages = channel.queueDeclarePassive(queueName).getMessageCount();
		} catch (IOException e) {
			Logger.debug("command queue '%s' doesn't exist", queueName);
			messages = -1;
		}
		
		return messages;
	}

	@Override
	public int consumers(String queueName) {
		ensureConnection();
		int consumers = -1;
		try {
			consumers = channel.queueDeclarePassive(queueName).getConsumerCount();
		} catch (IOException e) {
			Logger.debug("command queue '%s' doesn't exist", queueName);
			consumers = -1;
		}
		
		return consumers;
	}
	
	public boolean isInUse(String queueName)
	{
		ensureConnection();
		try {
			return channel.queueDeclarePassive(queueName).getConsumerCount() > 0;
		} catch (IOException e) {
			Logger.debug("command queue '%s' doesn't exist ", queueName);
		}
		
		return false;
	}
	
	public Channel getChannel()
	{
		return channel;
	}
	
	public void enableConfirmMode() throws IOException
	{
		ensureConnection();
		if (isOpen() && !confirmMode)
		{
			channel.confirmSelect();
			confirmMode = true;
		}
	}

	@Override
	public boolean deleteQueue(final String queueName, boolean force) throws IOException {
		boolean ret = false;
		Channel channel = null;
		try {
			channel = createQueueChannel();
			DeclareOk r = channel.queueDeclarePassive(queueName);
			int messages = r.getMessageCount();
			if (force || messages == 0)
			{
				channel.queueDelete(queueName);
				Logger.info("deleted queue '%s'", queueName);
				ret = true;
			}
		} catch (IOException e) {
			if (e.getCause() instanceof ShutdownSignalException)
			{
				if (((ShutdownSignalException)e.getCause()).getReason().toString().contains("NOT_FOUND"))
					ret = true;
			}
		} catch (Exception e) {
			Logger.error(e, "error deleting queue %s", queueName);
		} finally {
			try {
				if (channel != null)
					channel.close();
			} catch (Exception e) {}
		}
		
		return ret;
	}
	
	@Override
	public Worker createConsumer(String queueName, MessageProcessor<byte[]> processor, boolean autoAck, int prefetch) throws IOException
	{
		return new MQWorker(createQueueChannel(), queueName, processor, autoAck, prefetch);
	}
	
	public void publish(String queueName, byte[] message) throws IOException
	{
		ensureConnection();
		channel.basicPublish("", queueName, MessageProperties.PERSISTENT_BASIC, message);
		if (confirmMode)
		{
			try {
				channel.waitForConfirmsOrDie();
			} catch (InterruptedException e) {
				throw new IOException("wait for confirms interrupted", e);
			}
		}
	}
	
	@Override
	public void close()
	{
		open.set(false);
		if (channel != null)
		{
			try {
				channel.close();
				channel = null;
			} catch (Exception e) {}
		}
	}
	
	/**
	 * A worker of consuming queue message
	 */
	public static class MQWorker implements Worker
	{
		private QueueingConsumer consumer;
		private MessageProcessor<byte[]> processor;
		private Channel channel;
		private volatile boolean quitted = false;
		private boolean autoAck;
		
		public MQWorker(Channel channel, String queueName, MessageProcessor<byte[]> processor, boolean autoAck, int prefetch) throws IOException
		{
			this.channel = channel;
			this.processor = processor;
			this.autoAck = autoAck;
			
			channel.queueDeclare(queueName, true, false, false, null);
			consumer = new QueueingConsumer(channel);
//			channel.basicQos(prefetch);
			channel.basicConsume(queueName, autoAck, consumer);
			this.processor = processor;
		}
		
		@Override
		public void loop() throws Exception
		{
			Queue<RabbitMQResult> ackQueue = new LinkedList<RabbitMQResult>(); 
			while (!quitted)
			{
				if (processor == null)
					throw new NullPointerException("No message processor");
				
				try {
					QueueingConsumer.Delivery delivery = consumer.nextDelivery(5000);
					acknowledgeMessage(ackQueue, channel);
					if (delivery == null)
						continue;
					else if (delivery.getBody() == null)
					{
						channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
						continue;
					}
					
					byte[] message = delivery.getBody();
					try {
						processor.processMessage(message, 
								new RabbitMQMessageAck(delivery.getEnvelope().getDeliveryTag(), autoAck ? null : ackQueue));
					} catch (Exception e) {
						channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
						throw e;
					}
				} catch (ShutdownSignalException e) {
					Logger.error("MQWorker ShutdownSignalException: %s", e.getMessage());
					break;
				} catch (InterruptedException e) {
					Logger.error("MQWorker InterruptedException: %s", e.getMessage());
					break;
				}
			}
		}
		
		private void acknowledgeMessage(Queue<RabbitMQResult> ackQueue, Channel channel) throws IOException {
			if (ackQueue.size() > 0)
			{
				synchronized (ackQueue) {
					RabbitMQResult r;
					while ((r = ackQueue.poll()) != null)
					{
//						Logger.info("finished processing message: %d (%d)", r.getDeliveryTag(), System.nanoTime());
						if (r.isOk())
							channel.basicAck(r.getDeliveryTag(), false);
						else
							channel.basicNack(r.getDeliveryTag(), false, true);
					}
				}
			}
		}

		@Override
		public void close()
		{
			quitted = true;
			try {
				channel.close();
			} catch (Exception e) {}
			processor = null;
		}
	}
	
	static class RabbitMQResult
	{		
		private long deliveryTag;
		private boolean ok;
		
		public RabbitMQResult(long deliveryTag, boolean ok)
		{
			this.deliveryTag = deliveryTag;
			this.ok = ok;
		}
		
		public long getDeliveryTag()
		{
			return deliveryTag;
		}
		
		public boolean isOk()
		{
			return ok;
		}
	}
	
	static class RabbitMQMessageAck implements MessageAck
	{
		private Queue<RabbitMQResult> resultQueue;
		private long deliveryTag;
		
		public RabbitMQMessageAck(long deliveryTag, Queue<RabbitMQResult> resultQueue)
		{
			this.deliveryTag = deliveryTag;
			this.resultQueue = resultQueue;
		}

		@Override
		public String getMessageId() {
			return String.valueOf(deliveryTag);
		}

		@Override
		public synchronized void ack() {
			try {
				if (resultQueue != null)
				{
					synchronized (resultQueue) {
						resultQueue.add(new RabbitMQResult(deliveryTag, true));
					}
				}
			} finally {
				resultQueue = null;
			}
		}

		@Override
		public synchronized void notAck() {
			try {
				if (resultQueue != null)
				{
					synchronized (resultQueue) {
						resultQueue.add(new RabbitMQResult(deliveryTag, false));
					}
				}
			} finally {
				resultQueue = null;
			}
		}
		
	}
}
