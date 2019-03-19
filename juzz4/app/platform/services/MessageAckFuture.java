package platform.services;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import platform.mq.MessageAck;

public class MessageAckFuture<T> implements Future {
	
	private Future<T> future;
	private MessageAck messageAck;
	private T result;
	
	public MessageAckFuture(Future future, MessageAck messageAck)
	{
		this.future = future;
		this.messageAck = messageAck;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public MessageAckFuture<T> get() throws InterruptedException, ExecutionException {
		result = future.get();
		return this;
	}

	@Override
	public MessageAckFuture<T> get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		result = future.get(timeout, unit);
		return this;
	}
	
	public T getResult()
	{
		return result;
	}

	public MessageAck getMessageAck()
	{
		return messageAck;
	}
}
