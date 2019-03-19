package platform.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import platform.mq.CommandMessageQueue;
import platform.mq.EventMessageQueue;
import platform.mq.EventMessageQueue.EventQueueService;
import platform.mq.QueueService;
import platform.mq.CommandMessageQueue.CommandQueueService;
import play.Logger;
import play.Play;
import play.services.Service;

public class EventService extends Service {

	private QueueService qs = null;
	
	public EventService() {
		super();
	}

	@Override
	public boolean isMutex() {
		return true;
	}

	@Override
	protected void startService() {
		int poolSize = Integer.parseInt(Play.configuration.getProperty("play.jobs.pool", "10")) * 2;
		EventMessageQueue eventQueue = new EventMessageQueue();
		while (Play.started)
		{
			try {
				eventQueue.open();
				eventQueue.close();
				qs = eventQueue.newQueueService(poolSize);
				qs.run();
			} catch (Exception e) {
				Logger.error(e, "event queue stopped unexpectedlly");
			} finally {
				stopService();
			}
			if (Play.started)
			{
				try {
					Logger.warn("restarting event queue service in 5 seconds");
					Thread.sleep(5000);
				} catch (InterruptedException e1) {}
			}
		}
		Logger.info("event queue service stopped");
	}
	
	@Override
	public void stopService()
	{
		try {
			if (qs != null)
				qs.close();
			qs = null;
		} catch (Exception e) {}
	}

	@Override
	protected String dump() {
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		EventMessageQueue.EventQueueService s = (EventQueueService) qs;
		out.println("messages: " + s.getTotalCount());
		out.println("success: " + s.getSuccessCount());
		out.println("fail: " + s.getFailCount());
		out.println("average: " + s.getAverage() + " /sec");
		out.println("last event: " + s.currentEventId() + " at " + new Date(s.getLastProcessTime()));
		
		return sw.toString();
	}
}
