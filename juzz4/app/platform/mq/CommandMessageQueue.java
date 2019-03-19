package platform.mq;

import com.kaisquare.sync.ITaskResult;
import com.kaisquare.sync.PlatformSynchronizationHandler;
import com.kaisquare.sync.TaskManager;
import models.NodeCommand;
import platform.mq.ResultCounting.ResultCheckDelegate;
import platform.services.MessageAckFuture;
import platform.services.ResultStatistics;
import play.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.concurrent.Future;

public class CommandMessageQueue extends WorkerQueue<NodeCommand> {
	
	public static final String QUEUE_NAME = "command_queue";
	
	public CommandMessageQueue() {
		super(QUEUE_NAME);
	}
	
	public QueueService newQueueService(int prefetch) throws IOException
	{
		QueueService qs = new CommandQueueService(getQueue(), getFactory());
		qs.setAutoAck(false);
		qs.setPrefetch(prefetch);
		return new QueueServicePool(
				qs,
				new RabbitMQRpcQueueService(PlatformSynchronizationHandler.getInstance()));
	}
	
	public static class CommandQueueService extends WorkerQueueService implements ResultCheckDelegate, ResultStatistics
	{
		private int messageCount = 0;
		private String commandId;
		private ResultCounting result;
		private long lastTime;
		
		public CommandQueueService(String queueName, QueueWorkerFactory<NodeCommand> factory) throws IOException
		{
			super(queueName, factory);
			result = new ResultCounting(this);
			result.start();
		}

		private NodeCommand getCommand(byte[] message) throws IOException, ClassNotFoundException
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(message);
			ObjectInputStream ois = new ObjectInputStream(bais);
			
			return (NodeCommand) ois.readObject();
		}
		
		@Override
		public void processMessage(byte[] message, MessageAck messageAck) {
			try {
				lastTime = System.currentTimeMillis();
				messageCount++;
				NodeCommand command = getCommand(message);
				commandId = command.getIdAsStr();
				List<Future<Object>> futures = TaskManager.getInstance().notifyTask(command);
				for (Future<Object> f : futures)
					result.putFuture(new MessageAckFuture(f, messageAck));
			} catch (Exception e) {
				Logger.error(e, "error processing command");
				result.fail.incrementAndGet();
				messageAck.notAck();
			}
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
		
		public String currentCommandId()
		{
			return commandId;
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
				Object r = f.getResult();
				if (r != null)
					messageAck.ack();
				else
					messageAck.notAck();
				if (f.getResult() != null && f.getResult() instanceof ITaskResult)
				{
					ITaskResult result = (ITaskResult) f.getResult();
					ret = ((Boolean)result.getResult());
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
