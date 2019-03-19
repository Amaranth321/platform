package com.kaisquare.kaisync.transport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.jboss.netty.channel.Channel;

import com.kaisquare.kaisync.utils.AppLogger;

public class ThreadedTransportHandler extends ExternalTransportHandler {
	
	private ExecutorService mExecutor;

	public ThreadedTransportHandler(ExecutorService threadPool, ITransportListener listener) {
		super(listener);
		initHandler(threadPool);
	}
	
	public ThreadedTransportHandler(ExecutorService threadPool, ITransportListener listener, String keystore, String keypass) {
		super(listener, keystore, keypass);
		initHandler(threadPool);
	}
	
	private void initHandler(ExecutorService threadPool)
	{
		mExecutor = threadPool;
	}

	@Override
	protected void fireTransportListener(final Channel channel, final Object message) throws Exception {
		if (mExecutor instanceof ScheduledThreadPoolExecutor)
		{
			ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) mExecutor; 
			AppLogger.v(this, "handler thread pool: %d/%d/%d", 
					executor.getActiveCount(), executor.getPoolSize(), executor.getQueue().size());
		}
		mExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					getListener().onTransportReceived(channel, message);
				} catch (Exception e) {
					AppLogger.e(this, e, "error triggering listener method.");
					channel.close();
				}
			}
		});
	}

}
