package platform.mq;

import platform.services.ResultStatistics;
import play.Logger;
import play.Play;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultCounting extends Thread implements ResultStatistics {

	AtomicInteger success = new AtomicInteger(0);
	AtomicInteger fail = new AtomicInteger(0);
	private AtomicInteger count = new AtomicInteger(0);
	private AtomicInteger total = new AtomicInteger(0);
	private volatile boolean executed;
	private ArrayBlockingQueue<Future<Object>> futures;
	private ResultCheckDelegate delegate;
	private long start;
	private long end;
	private long processTime;
	
	public ResultCounting(ResultCheckDelegate delegate)
	{
		int poolSize = Integer.parseInt(Play.configuration.getProperty("play.jobs.pool", "10")) * 4;
		futures = new ArrayBlockingQueue<Future<Object>>(poolSize, false);
		this.delegate = delegate;
	}
	
	@Override
	public void run()
	{
		executed = true;
		start = System.nanoTime();
		boolean reset = true;
		while (executed)
		{
			Future<Object> f = null;
			try {
				reset = futures.size() == 0;
				f = futures.take();
				if (reset)
				{
					start = System.nanoTime();
					end = 0;
					total.set(0);
				}
				Object obj = f.get();
				if (delegate != null)
				{
					if (delegate.checkResult(obj))
						success.incrementAndGet();
					else
						fail.incrementAndGet();
				}
//				Logger.info("finished result check: %s (%d)", ((MessageAckFuture)obj).getMessageAck().getMessageId(), System.nanoTime());
			} catch (InterruptedException e) {
				break;
			} catch (ExecutionException e) {
				Logger.error("Execution error: %s", e.getMessage());
				if (delegate != null)
					delegate.onException(f, e);
			} catch (Exception e) {
				Logger.error(e, "error checking result");
				fail.incrementAndGet();
			} finally {
				total.incrementAndGet();
				count.incrementAndGet();
				processTime = System.currentTimeMillis();
			}
			end = System.nanoTime();
		}
		start = 0;
	}
	
	public void putFuture(Future<Object> f) throws InterruptedException
	{
		futures.put(f);
	}
	
	public int size()
	{
		return futures.size();
	}
	
	public void quit()
	{
		executed = false;
		interrupt();
	}
	
	@Override
	public int getTotalCount()
	{
		return count.get();
	}
	
	@Override
	public int getSuccessCount()
	{
		return success.get();
	}
	
	@Override
	public int getFailCount()
	{
		return fail.get();
	}
	
	@Override
	public double getAverage()
	{
		double average = 0;
		if (end > 0 && start > 0 && end > start)
		{
			average = total.get() / (double)(end - start) * 1000000000;
		}
		return average;
	}
	
	@Override
	public long getLastProcessTime()
	{
		return processTime;
	}

	public interface ResultCheckDelegate
	{
		public boolean checkResult(Object obj);
		
		public void onException(Future f, Throwable e);
	}
}
