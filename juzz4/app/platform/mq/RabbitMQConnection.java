package platform.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

public class RabbitMQConnection implements ShutdownListener {
	
	private static final Object MUTEX = new Object();

	private ConnectionFactory presetFactory;
	private Connection connection;
	private String host;
	private int port;
	private AtomicBoolean open = new AtomicBoolean(false);
	private String user;
	private String password;
	private Map<String, Object> clientProperties;
	
	public RabbitMQConnection(ConnectionFactory factory)
	{
		presetFactory = factory;
	}
	
	public RabbitMQConnection(String host, int port, String user, String password)
	{
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
	}

	public Channel createChannel() throws IOException {
		ensureConnection();
		return connection.createChannel();
	}
	
	public synchronized void setClientProperties(Map<String, Object> properties)
	{
		if (clientProperties == null)
			clientProperties = new HashMap<String, Object>();
		
		clientProperties.clear();
		clientProperties.putAll(properties);
	}
	
	public boolean isOpen()
	{
		return open.get() && connection != null && connection.isOpen();
	}

	@Override
	public void shutdownCompleted(ShutdownSignalException cause) {
		open.set(false);
		connection.removeShutdownListener(this);
	}

	private void ensureConnection() throws IOException {
		synchronized (MUTEX) {
			if (!isOpen())
			{
				ConnectionFactory factory = null;
				if (presetFactory == null)
				{
					factory = new ConnectionFactory();
					factory.setHost(host);
					factory.setPort(port);
					if (user != null && !"".equals(user))
					{
						factory.setUsername(user);
						factory.setPassword(password);
					}
					factory.setConnectionTimeout(15000);
				}
				else
					factory = presetFactory;
				
				if (clientProperties != null)
					factory.setClientProperties(clientProperties);
				
				try {
					connection = factory.newConnection();
					connection.addShutdownListener(this);
					open.set(true);
				} catch (TimeoutException e) {
					throw new IOException("connection timeout " + host + ":" + port);
				}
			}
		}
	}
}
