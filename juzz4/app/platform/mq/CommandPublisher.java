package platform.mq;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import play.Logger;

import com.google.gson.Gson;
import com.kaisquare.kaisync.platform.Command;
import com.kaisquare.kaisync.platform.MessagePacket;
import com.rabbitmq.client.ConnectionFactory;

public class CommandPublisher {

	private static final Object MUTEX = new Object();

	private static RabbitMQConnection conn;
	
	private MessageQueue<byte[]> mq;
	private String queueName;
	
	public CommandPublisher(String queueName)
	{
		this.queueName = queueName;
		ensureConnection();
	}
	
	public boolean publish(Command command)
	{
		boolean ret = false;
		try {
			ensureConnection();
			
			MessagePacket packet = new MessagePacket();
			packet.put("commandcount", 1);
			packet.put("cmds", new Gson().toJson(Arrays.asList(command)));
			mq.publish(queueName, packet.toBytes());
			ret = true;
		} catch (Exception e) {
			Logger.error(e, "");
		}
		
		return ret;
	}
	
	public int queueMessages()
	{
		ensureConnection();
		return mq.queueMessages(queueName);
	}

	public int consumers() {
		ensureConnection();
		return mq.consumers(queueName);
	}
	
	public boolean deleteQueue(boolean force)
	{
		try {
			ensureConnection();
			return mq.deleteQueue(queueName, force);
		} catch (IOException e) {
			Logger.error(e, "failed to delete queue '%s'", queueName);
		}
		
		return false;
	}
	
	public void close()
	{
		mq.close();
	}
	
	private synchronized void ensureConnection() {
		if (mq == null || !mq.isOpen())
		{
			try {
				if (mq != null)
					mq.close();
				else
				{
					synchronized (MUTEX) {
						if (conn == null)
						{
							ConnectionFactory factory = MQConnection.getMQFactory();
							conn = new RabbitMQConnection(factory);

							HashMap<String, Object> properties = new HashMap<String, Object>();
							properties.put("client", "CommandPublisher");
							conn.setClientProperties(properties);
						}
					}
					mq = new RabbitMessageQueue(conn);
				}
				mq.open();
				Logger.debug("created command queue connection");
			} catch (Exception e) {
				Logger.error(e, "");
			}
		}
	}
	
	public static MessagePacket newCommandPacket(Command command)
	{
		MessagePacket packet = new MessagePacket();
		packet.put("commandcount", 1);
		packet.put("cmds", new Gson().toJson(Arrays.asList(command)));
		return packet;
	}

}
