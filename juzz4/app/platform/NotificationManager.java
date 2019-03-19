package platform;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeoutException;

import platform.mq.MQConnection;
import play.Logger;
import play.Play;
import play.libs.F.Promise;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;


public class NotificationManager {
	
	public static final String symbol = ",";
	public static final String APNS = "APNS";
	public static final String GCM = "GCM";
	
	private static final int DEFAULT_DELIVERY_TMEOUT = 20000;
	private static final String EXCHANGE_EVENTS = "events";
	private static NotificationManager instance;
    
    private NotificationTimeoutThread monitorThread;
	
	private NotificationManager () {
		monitorThread = new NotificationTimeoutThread();
		monitorThread.setDaemon(true);
		monitorThread.setName("NotificationTimeoutThread");
		monitorThread.start();
	}
	
	private Channel createMQChannel() throws IOException {
    	Channel channel = MQConnection.createNewChannel();
        channel.exchangeDeclare(EXCHANGE_EVENTS, "direct");
		
		return channel;
    }
	
	public void publishEventNotification(long userId, String data) throws IOException
	{
		Channel channel = createMQChannel();
		try {
			channel.confirmSelect();
			channel.basicPublish(EXCHANGE_EVENTS, String.valueOf(userId), null, data.getBytes());
			try {
				channel.waitForConfirmsOrDie();
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		} finally {
			try {
				channel.close();
			} catch (TimeoutException e) {}
		}
	}

	/**
	 * Register an user for retrieving on-screen notification
	 * @param userId
	 * @return
	 */
	public NotificationToken registerUser(String userId) throws IOException {
		
		Channel channel = createMQChannel();
		//subscribe for event
		Promise<String> future = new Promise<String>();
        String mqName = channel.queueDeclare().getQueue();
        channel.queueBind(mqName, EXCHANGE_EVENTS, userId);
        NotificationConsumer mqConsumer = new NotificationConsumer(future);
        channel.basicConsume(mqName, true, mqConsumer);
		
        NotificationToken token = new NotificationToken(channel, userId, mqName, future);
        try {
			monitorThread.addListener(token, mqConsumer, DEFAULT_DELIVERY_TMEOUT);
		} catch (NotAvailableException e) {
			unregisterUser(token);
			throw new IOException("Not available temporarily");
		}
        
		return token;
	}
	
	public void unregisterUser(NotificationToken token) throws IOException {
		token.close();
	}
	
	public static NotificationManager getManager()
	{
		synchronized (NotificationManager.class) {
			if (instance == null)
				instance = new NotificationManager();
		}
		
		return instance;
	}
	
	public static class NotificationToken
	{
		private String name;
		private String key;
		private Promise<String> promise;
		private Channel channel;
		
		public NotificationToken(Channel conn, String name, String key, Promise<String> promise)
		{
			this.channel = conn;
			this.name = name;
			this.key = key;
			this.promise = promise;
		}
		
		public String getTokenName()
		{
			return name;
		}
		
		public String getTokenKey()
		{
			return key;
		}
		
		public Promise<String> getPromise()
		{
			return promise;
		}
		
		public void close()
		{
			try {
				channel.queueUnbind(key, EXCHANGE_EVENTS, name);
				channel.queueDelete(key);
			} catch (Exception e) {
				Logger.error(e, "");
			} finally {
				try {
					channel.close();
				} catch (Exception e) {}
				channel = null;
			}
		}
	}
	
	public static class NotAvailableException extends Exception
	{

		public NotAvailableException() {
			super();
			// TODO Auto-generated constructor stub
		}

		public NotAvailableException(String message, Throwable cause,
				boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
			// TODO Auto-generated constructor stub
		}

		public NotAvailableException(String message, Throwable cause) {
			super(message, cause);
			// TODO Auto-generated constructor stub
		}

		public NotAvailableException(String message) {
			super(message);
			// TODO Auto-generated constructor stub
		}

		public NotAvailableException(Throwable cause) {
			super(cause);
			// TODO Auto-generated constructor stub
		}
		
	}
	
	private interface TimeoutListener
	{
		public void onReceiveTimeout();
	}
	
	private static class NotificationHolder
	{
		public long timestamp;
		public int timeout;
		public TimeoutListener listener;
		public NotificationToken token;
		
		public NotificationHolder(long timestamp, NotificationToken token, int timeout, TimeoutListener listener)
		{
			this.timestamp = timestamp;
			this.timeout = timeout;
			this.token = token;
			this.listener = listener;
		}
	}
	
	private static class NotificationTimeoutThread extends Thread
	{
		private ArrayBlockingQueue<NotificationHolder> listeners = 
				new ArrayBlockingQueue<NotificationManager.NotificationHolder>(3000);
		
		private Object locker = new Object();
		
		public NotificationTimeoutThread()
		{
		}
		
		public void addListener(NotificationToken token, TimeoutListener listener, int timeout) throws NotAvailableException
		{
			if (!listeners.offer(new NotificationHolder(System.currentTimeMillis(), token, timeout, listener)))
				throw new NotAvailableException("Notification not available temporarily");
		}
		
		@Override
		public void run() {
			NotificationManager nm = NotificationManager.getManager();
			while (Play.started)
			{
				try {
					NotificationHolder holder = listeners.take();
					long now = System.currentTimeMillis();
					long diff = (holder.timestamp + holder.timeout) - now;
					Logger.debug("check notification now: %d token: %s, ts %d, timeout: %d, diff: %d",
							now,
							holder.token.key,
							holder.timestamp,
							holder.timeout,
							diff);
					if (diff > 0)
					{
						synchronized (locker) {
							locker.wait(diff);
						}
					}
					holder.listener.onReceiveTimeout();
					
					nm.unregisterUser(holder.token);
					holder.listener = null;
					holder.token = null;
				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					Logger.error("NotificationTimeoutThread: %s", e.getMessage());
				}
			}
			
			NotificationHolder o;
			while ((o = listeners.poll()) != null)
			{
				o.listener.onReceiveTimeout();
				o.listener = null;
			}
		}
		
	}
	
	private static class NotificationConsumer implements Consumer, TimeoutListener
	{		
		private boolean isReceived = false;
		private Promise<String> promise;
		
		public NotificationConsumer(Promise<String> promise)
		{
			this.promise = promise;
		}

		@Override
		public void handleConsumeOk(String consumerTag) {
		}

		@Override
		public void handleCancelOk(String consumerTag) {
			if (!isReceived)
				invokePromise("");
		}

		@Override
		public void handleCancel(String consumerTag) throws IOException {
			if (!isReceived)
				invokePromise("");
		}

		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                byte[] body) throws IOException {
			isReceived = true;
			
			//handle event (or timeout)
	        String message = "";
	        if(body != null) {
		        message = new String(body);
	        }
	        
	        invokePromise(message);
		}

		@Override
		public void handleShutdownSignal(String consumerTag,
				ShutdownSignalException sig) {
			if (!isReceived)
				invokePromise("");
		}

		@Override
		public void handleRecoverOk(String consumerTag) {
		}
		
		private synchronized void invokePromise(String result)
		{
			if (promise != null)
			{
				promise.invoke(result);
				promise = null;
			}
		}

		@Override
		public void onReceiveTimeout() {
			invokePromise("");
		}
	}
}
