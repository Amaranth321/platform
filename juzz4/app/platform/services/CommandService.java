package platform.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import platform.mq.CommandMessageQueue;
import platform.mq.QueueService;
import platform.mq.CommandMessageQueue.CommandQueueService;
import play.Logger;
import play.Play;
import play.services.Service;

public class CommandService extends Service {

	private QueueService qs = null;
	
	public CommandService() {
		super();
	}

	@Override
	public boolean isMutex() {
		return false;
	}

	@Override
	protected void startService() {
		int poolSize = Integer.parseInt(Play.configuration.getProperty("play.jobs.pool", "10")) + 1;
		CommandMessageQueue commandQueue = new CommandMessageQueue();
		while (Play.started)
		{
			try {
				qs = commandQueue.newQueueService(poolSize);
				qs.run();//blocking process
			} catch (Exception e) {
				Logger.error(e, "command queue stopped unexpectedlly");
			} finally {
				stopService();
			}
			if (Play.started)
			{
				try {
					Logger.warn("restarting command queue service in 5 seconds");
					Thread.sleep(5000);
				} catch (InterruptedException e1) {}
			}
		}
		Logger.info("command service stopped");
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
		ResultStatistics s = (ResultStatistics) qs;
		out.println("messages: " + s.getTotalCount());
		out.println("success: " + s.getSuccessCount());
		out.println("fail: " + s.getFailCount());
		out.println("average: " + s.getAverage() + " /sec");
		out.println("last process: " + new Date(s.getLastProcessTime()));
		
		return sw.toString();
	}

}
