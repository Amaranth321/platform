package com.kaisquare.kaisync.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class SimpleLock implements Lock {
	
	private Object mLock = new Object();
	private AtomicBoolean mIsLocked = new AtomicBoolean(false);
	private long mLockedId;
	private boolean fair = false;
	private List<Long> ordering = new ArrayList<Long>();
	
	public SimpleLock()
	{
		this(false);
	}
	
	public SimpleLock(boolean fair)
	{
		this.fair = fair;
	}

	@Override
	public void lock() {
		lock(-1, TimeUnit.SECONDS);
	}

	private boolean lock(long time, TimeUnit unit) {
		long currentThreadId = Thread.currentThread().getId();
		boolean acquired = false;
		long start = System.nanoTime();
		while (true)
		{
			if (!fair || (fair && isItTurn(currentThreadId)))
				acquired = !mIsLocked.getAndSet(true);
			
			if (mLockedId == currentThreadId)
				return true;
			
			if (acquired)
			{
				synchronized (mLock) {
					mLockedId = currentThreadId;
					break;
				}
			}

			if (fair)
				putInOrder(currentThreadId);
			if (time == -1)
			{
				synchronized (mLock) {
					try {
						while (mIsLocked.get())
							mLock.wait(100);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
			else if (time > 0)
			{
				long timeout = unit.toMillis(time);
				if (timeout >= 0)
				{
					long t = (System.nanoTime() - start) / 1000000;
					if (t >= timeout)
						return false;
					
					synchronized (mLock) {
						try {
							mLock.wait(timeout - t);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
				else
					break;
			}
		}
		removeFromOrder(currentThreadId);
		
		return acquired;
	}

	private void putInOrder(long threadId) {
		synchronized (ordering) {
			if (!ordering.contains(threadId))
				ordering.add(threadId);
		}
	}
	
	private void removeFromOrder(long threadId)
	{
		synchronized (ordering) {
			if (ordering.contains(threadId))
				ordering.remove(threadId);
		}
	}
	
	private boolean isItTurn(long threadId)
	{
		return ordering.size() == 0 || (ordering.size() > 0 && ordering.get(0).longValue() == threadId) ? true : false;
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		throw new RuntimeException("Not supported");
	}

	@Override
	public boolean tryLock() {
		return lock(0, TimeUnit.SECONDS);
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		return tryLock(time, unit);
	}

	@Override
	public void unlock() {
		synchronized (mLock) {
			long currentThreadId = Thread.currentThread().getId();
			
			if (mLockedId == currentThreadId)
			{
				mIsLocked.set(false);
				mLockedId = 0;
				mLock.notifyAll();
			}
		}
	}

	@Override
	public Condition newCondition() {
		return null;
	}

}
