package com.kaisquare.util;

import java.lang.ref.Reference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.handler.codec.serialization.SoftReferenceMap;

import play.cache.Cache;
import play.cache.CacheImpl;
import play.libs.Time;

public class CacheProxy implements CacheImpl {
	
	private static CacheProxy instanceLocal;
	private static CacheProxy instance;
	
	private CacheImpl cacheImpl;
	
	private CacheProxy(boolean self)
	{
		if (self)
			cacheImpl = new LocalMemoryCache();
		else
			cacheImpl = Cache.cacheImpl;
	}
	
	public void add(String key, Object value)
	{
		add(key, value, Time.parseDuration(null));
	}

	@Override
	public void add(String key, Object value, int expiration) {
		cacheImpl.add(key, value, expiration);
	}

	@Override
	public boolean safeAdd(String key, Object value, int expiration) {
		return cacheImpl.safeAdd(key, value, expiration);
	}

	public void set(String key, Object value)
	{
		set(key, value, Time.parseDuration(null));
	}
	
	@Override
	public void set(String key, Object value, int expiration) {
		cacheImpl.set(key, value, expiration);
	}

	@Override
	public boolean safeSet(String key, Object value, int expiration) {
		return cacheImpl.safeSet(key, value, expiration);
	}
	
	public void replace(String key, Object value)
	{
		replace(key, value, Time.parseDuration(null));
	}

	@Override
	public void replace(String key, Object value, int expiration) {
		cacheImpl.replace(key, value, expiration);
	}

	@Override
	public boolean safeReplace(String key, Object value, int expiration) {
		return cacheImpl.safeReplace(key, value, expiration);
	}

	@Override
	public Object get(String key) {
		return cacheImpl.get(key);
	}

	@Override
	public Map<String, Object> get(String[] keys) {
		return cacheImpl.get(keys);
	}

	@Override
	public long incr(String key, int by) {
		return cacheImpl.incr(key, by);
	}

	@Override
	public long decr(String key, int by) {
		return cacheImpl.decr(key, by);
	}

	@Override
	public void clear() {
		cacheImpl.clear();
	}

	@Override
	public void delete(String key) {
		cacheImpl.delete(key);
	}

	@Override
	public boolean safeDelete(String key) {
		return cacheImpl.safeDelete(key);
	}

	@Override
	public void stop() {
		cacheImpl.stop();
	}
	
	/**
	 * Get default cache instance
	 * @return
	 */
	public static CacheProxy getInstance()
	{
		return getInstance(true);
	}

	/**
	 * Get 'Cache' proxy, which is able to use local memory, or use the implementation provided by Play framework
	 * @param local use local memory for cache
	 * @return
	 */
	public synchronized static CacheProxy getInstance(boolean local)
	{
		CacheProxy proxy = null;
		if (local)
		{
			if (instanceLocal == null)
				instanceLocal = new CacheProxy(true);
			
			proxy = instanceLocal;
		}
		else
		{
			if (instance == null)
				instance = new CacheProxy(false);
			
			proxy = instance;
		}
		
		return proxy;
	}
}
