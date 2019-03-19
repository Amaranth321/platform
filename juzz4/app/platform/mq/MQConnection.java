package platform.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lib.util.JsonReader;
import platform.config.readers.ConfigsServers;

public class MQConnection {
	
	private static final Object connMutex = new Object();
	private static final Object factoryMutex = new Object();
	private static ConnectionFactory mqFactory;
	private static RabbitMQConnection mqConn;
	
	private Connection connection;
	private Channel channel;
	
	public MQConnection(Connection conn, Channel ch)
	{
		connection = conn;
		channel = ch;
	}

	public boolean isOpen()
	{
		return channel.isOpen();
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public Channel getChannel()
	{
		return channel;
	}
	
	public void close()
	{
		try {
			if (channel != null)
				channel.close();
			channel = null;
		} catch (Exception e) {}
		try {
			if (connection != null)
				connection.close();
			connection = null;
		} catch (Exception e) {}
	}
	
	/**
	 * Create a message queue channel, it's NOT thread safe
	 * @return
	 */
	public static MessageQueue<byte[]> createDefaultMessageQueue()
	{
		return createDefaultMessageQueue(null);
	}
	
	/**
	 * Create a message queue channel, it's NOT thread safe
	 * @param properties extra properties for the connections
	 * @return
	 */
	public static MessageQueue<byte[]> createDefaultMessageQueue(Map<String, Object> properties)
	{
		ConnectionFactory factory = getMQFactory();
		RabbitMQConnection conn = new RabbitMQConnection(factory);
		if (properties != null)
			conn.setClientProperties(properties);
		return new RabbitMessageQueue(conn);
	}
	
	public static ConnectionFactory getMQFactory()
	{
		synchronized (factoryMutex) {
			if (mqFactory == null)
			{
				mqFactory = new ConnectionFactory();

                JsonReader msgSvrInfo = ConfigsServers.getInstance().kaisyncRabbitmqServerCfg();
				mqFactory.setHost(msgSvrInfo.getAsString("host", null));
				mqFactory.setPort(msgSvrInfo.getAsInt("port", 0));

		        String username = msgSvrInfo.getAsString("user", null);
                String password = msgSvrInfo.getAsString("password", null);
                if (username != null && !"".equals(username))
                {
                    mqFactory.setUsername(username);
                    mqFactory.setPassword(password);
                }
			}
		}
		
		return mqFactory;
	}
	
	public static Channel createNewChannel() throws IOException
	{
		synchronized (connMutex) {
			if (mqConn == null)
			{
				ConnectionFactory factory = getMQFactory();
				HashMap<String, Object> properties = new HashMap<String, Object>();
				properties.put("client", "platform");
				factory.setClientProperties(properties);
				mqConn = new RabbitMQConnection(factory);
			}
		}
		
		return mqConn.createChannel();
	}
}
