package com.kaisquare.kaisync.platform;

import java.io.Closeable;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.kaisquare.kaisync.KAISync;
import com.kaisquare.kaisync.transport.PThreadFactory;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.Utils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

class ConnectionHolder implements Closeable
{
    private static final int CONNECTION_IDLE_TIMEOUT = 60000;
	private static final AtomicInteger Count = new AtomicInteger(0);
	private static final Object lock = new Object();
	
	private static HashMap<String, Object> properties;
	private static ConnectionFactory factory;
	private static Connection conn;
	private static ClosingHandler handler = new ClosingHandler();
	private static AtomicBoolean closed = new AtomicBoolean(true);
	private static long lastClosedChannelTimestamp;
	private static ShutdownListener shutdownListener = new ShutdownListener() {
		
		@Override
		public void shutdownCompleted(ShutdownSignalException cause) {
			closed.set(true);
		}
	};
	private Channel channel;
	
	protected ConnectionHolder()
	{
	}
	
	protected ConnectionHolder(Channel channel)
	{
		setConnection(channel);
	}
	
	protected void setConnection(Channel channel)
	{
		handler.cancelClosing();
		this.channel = channel;
	}
	
	public boolean isOpen()
	{
		return !closed.get() && conn != null && channel != null && conn.isOpen() && channel.isOpen();
	}
	
	public void awaitForConfirms() throws IOException, InterruptedException, TimeoutException
	{
		awaitForConfirms(60000);
	}
	
	public void awaitForConfirms(int timeout) throws IOException, InterruptedException, TimeoutException
	{
		channel.waitForConfirmsOrDie(timeout);
	}
	
	public Channel getChannel()
	{
		return channel;
	}
	
	public synchronized void close()
	{
		try {
			if (channel != null)
			{
				channel.close();
				synchronized (lock) {
					if (Count.decrementAndGet() == 0)
					{
	                    lastClosedChannelTimestamp = System.currentTimeMillis();
						handler.prepareClosing();
					}
				}
			}
		} catch (Exception e) {}
		channel = null;
	}

    public void release()
    {
        synchronized (lock) {
            try {
                channel.close();
            } catch (Exception e) {
            }
            try {
                conn.close();
            } catch (Exception e) {
            }
            try {
                if (handler != null)
                    handler.quit();
            } catch (Throwable t) {
                AppLogger.d(this, "error occurred during releasing connections: %s", t.getMessage());
            } finally {
                handler = null;
                Count.set(0);
            }
        }
    }
	
	public static ConnectionHolder newConnection(String protocol, String host, int port, String keystore, String keypass, String username, String password) throws IOException, NoSuchAlgorithmException
	{
		Channel channel = null;
		synchronized (lock) {
			if (properties == null)
			{
				properties = new HashMap<String, Object>();
				properties.put("client", "kaisync-" + KAISync.getVersion());
			}
			if (handler == null)
			    handler = new ClosingHandler();
			
			for (int i = 0; i < 2; i++) {
				try {
					if (closed.get() || conn == null || !conn.isOpen())
					{
						if (conn != null)
						{
							try {
							    conn.close();
							}
							catch (Exception e) {}
						}
						
						factory = getFactory(protocol, host, port, keystore, keypass, username, password);
						factory.setClientProperties(properties);
						factory.setConnectionTimeout(CONNECTION_IDLE_TIMEOUT);
						factory.setRequestedHeartbeat(60);
						factory.setShutdownTimeout(20000);
						conn = factory.newConnection();
						conn.addShutdownListener(shutdownListener);
						Count.set(0);
						closed.set(false);
						AppLogger.i("ConnectionHolder", "connected to platform sync service %s:%d", host, port);
					}
				} catch (TimeoutException e) {
					throw new IOException("connection timeout");
				}
				try {
					channel = conn.createChannel();
					channel.confirmSelect();
					Count.incrementAndGet();
					lastClosedChannelTimestamp = 0;
					break;
				} catch (IOException e) {
					AppLogger.e("ConnectionHolder", "unable to create channel: %s", e.getMessage());
					//if the channel cannot be created, maybe it's connection problem
					//so let it try to recreate the connection again
					if (i == 1)
						throw e;
				}
			}
		}
		return new ConnectionHolder(channel);
	}

	public static ConnectionFactory getFactory(String protocol, String host, int port, String keystore, String keypass, String username, String password) throws IOException, NoSuchAlgorithmException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		factory.setPort(port);
		if (!Utils.isStringEmpty(username))
		{
			factory.setUsername(username);
			factory.setPassword(password);
		}
		if (!Utils.isStringEmpty(keystore))
			factory.useSslProtocol(SSLContextFactory.create(protocol, keystore, keypass));
		
		return factory;
	}
	
	static class ClosingHandler
	{
		private ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1, new PThreadFactory("Closing-pool"));
		private Runnable runnable = new ClosingRunnable();
		private AtomicBoolean isClosing = new AtomicBoolean(false);
		
		public ClosingHandler()
		{
		}

        public boolean isClosing()
		{
			return isClosing.get();
		}
		
		public void prepareClosing()
		{
			if (!isClosing.getAndSet(true))
			{
				pool.schedule(runnable, CONNECTION_IDLE_TIMEOUT, TimeUnit.MILLISECONDS);
			}
		}
		
		public void cancelClosing()
		{
			pool.remove(runnable);
			isClosing.set(false);
		}
        
        public void quit()
        {
            cancelClosing();
            pool.shutdownNow();
        }
	}
	
	static class ClosingRunnable implements Runnable
	{
        @Override
		public void run() {			
			synchronized (lock) {
				try {
					if (Count.get() > 0 ||
					   (lastClosedChannelTimestamp > 0 && (System.currentTimeMillis() - lastClosedChannelTimestamp) < CONNECTION_IDLE_TIMEOUT))
						return;
					
					conn.close();
					AppLogger.i("ConnectionHolder", "no more channels for %d ms, disconnected from platform sync service",
					                CONNECTION_IDLE_TIMEOUT);
				} catch (Exception e) {
					conn = null;
				}
			}
		}
		
	}
}
