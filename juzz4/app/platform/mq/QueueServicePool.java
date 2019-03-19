package platform.mq;

import platform.services.ResultStatistics;
import play.utils.PThreadFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class QueueServicePool implements ResultStatistics, QueueService {
	
	public ExecutorService threadPool;
	public QueueService[] services;
	
	public QueueServicePool(QueueService...qs)
	{
		services = qs;
	}

	@Override
	public int getTotalCount() {
		int sum = 0;
		for (QueueService qs : services)
		{
			try {
				sum += ((ResultStatistics)qs).getTotalCount();
			} catch (Exception e) {}
		}
		return sum;
	}

	@Override
	public int getSuccessCount() {
		int sum = 0;
		for (QueueService qs : services)
		{
			try {
				sum += ((ResultStatistics)qs).getSuccessCount();
			} catch (Exception e) {}
		}
		return sum;
	}

	@Override
	public int getFailCount() {
		int sum = 0;
		for (QueueService qs : services)
		{
			try {
				sum += ((ResultStatistics)qs).getFailCount();
			} catch (Exception e) {}
		}
		return sum;
	}

	@Override
	public double getAverage() {
		int sum = 0;
		for (QueueService qs : services)
		{
			try {
				sum += ((ResultStatistics)qs).getAverage();
			} catch (Exception e) {}
		}
		return sum;
	}

	@Override
	public long getLastProcessTime() {
		long time = 0;
		for (QueueService qs : services)
		{
			try {
				time = Math.max(((ResultStatistics)qs).getLastProcessTime(), time);
			} catch (Exception e) {}
		}
		return time;
	}

	@Override
	public void run() {
		if (services.length > 0)
		{
			ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
			threadPool = Executors.newFixedThreadPool(services.length, new PThreadFactory("QueueServicePool-" + services.length));
			for (QueueService qs : services)
			{
				Future<?> f = threadPool.submit(qs);
				futures.add(f);
			}
			
			for (Future<?> f : futures)
			{
				try {
					f.get();
				} catch (InterruptedException | ExecutionException e) {
				}
			}
		}
	}

	@Override
	public void setPrefetch(int prefetch) {
	}

	@Override
	public void setAutoAck(boolean autoAck) {
	}

	@Override
	public void close() {
		if (threadPool != null)
		{
			for (QueueService qs : services)
				qs.close();
			threadPool.shutdown();
		}
	}

}
