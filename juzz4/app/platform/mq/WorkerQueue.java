package platform.mq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import play.Logger;

public abstract class WorkerQueue<T> { 
	
	private MessageQueue<byte[]> mMQ;
	private String queueName;

	public WorkerQueue(String queueName) {
		this.queueName = queueName;
		Map<String, Object> properties;
		properties = new HashMap<String, Object>();
		properties.put("client", getClass().getSimpleName());
		properties.put("queue", queueName);
		mMQ = MQConnection.createDefaultMessageQueue(properties);
	}

	public void open() throws IOException {
		mMQ.open();
		createQueue();
	}
	
	public boolean isOpen()
	{
		return mMQ.isOpen();
	}
	
	public String getQueue()
	{
		return queueName;
	}

	public void createQueue() throws IOException {
		mMQ.createQueue(queueName);
	}

	public int queueMessages() {
		return mMQ.queueMessages(queueName);
	}

	public void deleteQueue(boolean force) throws IOException {
		mMQ.deleteQueue(queueName, force);
	}
	
	public void publish(T message) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(message);
		oos.flush();
		
		publish(baos.toByteArray());
		oos.reset();
		oos.close();
	}
	
	protected void publish(byte[] message) throws IOException
	{
		mMQ.publish(queueName, message);
	}
	
	protected MessageQueue<byte[]> getMessageQueue()
	{
		return mMQ;
	}

	public void close() {
		mMQ.close();
	}
	
	protected QueueWorkerFactory<T> getFactory()
	{
		return (QueueWorkerFactory<T>) mMQ;
	}
	
	public abstract Runnable newQueueService(int prefetch) throws IOException;
	
	public static abstract class WorkerQueueService implements MessageProcessor<byte[]>, QueueService
	{
		private Worker worker;
		private int prefetch;
		private boolean autoAck = true;
		private String queueName;
		private QueueWorkerFactory factory;
		
		public WorkerQueueService(String queueName, QueueWorkerFactory factory) throws IOException
		{
			this.queueName = queueName;
			this.factory = factory;
		}
		
		@Override
		public void setPrefetch(int prefetch)
		{
			this.prefetch = prefetch;
		}
		
		@Override
		public void setAutoAck(boolean autoAck)
		{
			this.autoAck = autoAck;
		}
		
		@Override
		public void run() {
			try {
				worker = factory.createConsumer(queueName, this, autoAck, prefetch);
				worker.loop();
			} catch (Exception e) {
				Logger.error(e, "error running MQ worker");
			}
		}
		
		public void close()
		{
			if (worker != null)
				worker.close();
		}
	}
}
