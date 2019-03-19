package platform.mq;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import jobs.cloud.HandleEventOnCloud;
import jobs.node.HandleEventOnNode;
import platform.Environment;
import platform.events.EventManager;
import platform.mq.ResultCounting.ResultCheckDelegate;
import platform.services.MessageAckFuture;
import platform.services.ResultStatistics;
import play.Logger;
import play.libs.F.Promise;

import com.kaisquare.events.thrift.EventDetails;
import com.kaisquare.kaisync.platform.MessagePacket;

public class EventMessageQueue extends WorkerQueue<EventDetails> {
	
	public static final String QUEUE_NAME = "event_queue";
	private static final String EXCHANGE_NAME = "event_push";

	public EventMessageQueue() {
		super(QUEUE_NAME);
	}
	
	@Override
	public void open() throws IOException {
		super.open();
		RabbitMessageQueue mq = (RabbitMessageQueue) getMessageQueue();
		try {
			mq.getChannel().exchangeDeclare(EXCHANGE_NAME, "fanout", true);
			mq.getChannel().queueBind(QUEUE_NAME, EXCHANGE_NAME, QUEUE_NAME);
		} catch (Exception e) {
			Logger.warn("event exchange declare: %s", e.getMessage());
		}
	}
	
	@Override
	public void publish(EventDetails message) throws IOException
	{
		MessagePacket packet = EventManager.getInstance().convertToMessagePacket(message);
		publish(packet.toBytes());
	}

	@Override
	public QueueService newQueueService(int prefetch) throws IOException {
		EventQueueService qs = new EventQueueService(getQueue(), getFactory());
		qs.setAutoAck(false);
		qs.setPrefetch(prefetch);
		return qs;
	}

	public static class EventQueueService extends WorkerQueueService implements ResultCheckDelegate, ResultStatistics
	{
		private int messageCount = 0;
		private String eventId;
		private ResultCounting result;
		private long lastTime;
		private MessageQueue<byte[]> mq;
		
		public EventQueueService(String queueName, QueueWorkerFactory factory) throws IOException {
			super(queueName, factory);
			result = new ResultCounting(this);
			result.start();
		}
		
		private EventDetails getEvent(byte[] message) throws IOException, ClassNotFoundException
		{			
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(message);
				ObjectInputStream ois = new ObjectInputStream(bais);
				
				return (EventDetails) ois.readObject();
			} catch (InvalidClassException e) {
				//this is due to EventDetail serialization problem,so remove this codes until there's no older version.
				//push those events to another temporary queue being consumed by temporary event process instance
				String queueName = "event_queue_old";
				if (mq == null || !mq.isOpen())
				{
					if (mq == null)
					{
						Map<String, Object> properties;
						properties = new HashMap<String, Object>();
						properties.put("client", "EventQueueService");
						properties.put("queue", queueName);
						mq = MQConnection.createDefaultMessageQueue(properties);
					}
					else
						mq.close();
					mq.open();
					mq.createQueue(queueName);
				}
				mq.publish(queueName, message);
				
				return null;
			} catch (Exception e) {
				MessagePacket packet = new MessagePacket(message);
				return EventManager.getInstance().convertToEventDetails(packet);
			}
		}

		@Override
		public void processMessage(byte[] message, MessageAck messageAck) {
			EventDetails event = null;
			try {
				lastTime = System.currentTimeMillis();
				messageCount++;
				event = getEvent(message);
				if (event == null)
				{
					messageAck.ack();
					return;
				}
				
				eventId = event.getId();
				Logger.debug("got event: %s [%s:%s]", event, event.getId(), event.getType());
				Logger.info("start processing message: %s (%d)", messageAck.getMessageId(), System.nanoTime());

                Promise future;
                if (Environment.getInstance().onKaiNode()) {
                    future = new HandleEventOnNode(event).now();
                }
                else {
                	Logger.info("on cloud processing");
                    future = new HandleEventOnCloud(event).now();
                }
				try {
					result.putFuture(new MessageAckFuture(future, messageAck));
				} catch (InterruptedException e) {}
			} catch (Exception e) {
				Logger.error(e, "error processing event %s", event);
				result.fail.incrementAndGet();
				messageAck.notAck();
			}
		}
		
		public String currentEventId()
		{
			return eventId;
		}
		
		@Override
		public int getTotalCount()
		{
			return messageCount;
		}
		
		@Override
		public int getSuccessCount()
		{
			return result.getSuccessCount();
		}
		
		@Override
		public int getFailCount()
		{
			return result.getFailCount();
		}
		
		@Override
		public double getAverage()
		{
			return result.getAverage();
		}
		
		public long getLastProcessTime()
		{
			return lastTime;
		}

		@Override
		public void close() {
			super.close();
			result.quit();
		}

		@Override
		public boolean checkResult(Object obj) {
			boolean ret = false;
			if (obj instanceof MessageAckFuture)
			{
				MessageAckFuture f = (MessageAckFuture) obj;
				MessageAck messageAck = f.getMessageAck();
				try {
					if (f.getResult() != null && f.getResult() instanceof Boolean && ((Boolean)f.getResult()).booleanValue())
						messageAck.ack();
					else
						messageAck.notAck();
					
					ret = true;
				} catch (Exception e) {
					Logger.error(e, "unable to know the result of event process");
				}
			}
			return ret;
		}
		
		@Override
		public void onException(Future f, Throwable e)
		{
			if (f instanceof MessageAckFuture)
				((MessageAckFuture)f).getMessageAck().notAck();
		}
	}
}
