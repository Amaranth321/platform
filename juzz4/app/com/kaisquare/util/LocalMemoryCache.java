package com.kaisquare.util;

import java.lang.ref.Reference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.handler.codec.serialization.SoftReferenceMap;

import play.Logger;
import play.cache.CacheImpl;

/**
 * This is a local memory hashmap cache using soft reference
 */
public class LocalMemoryCache implements CacheImpl {
	
	private static final SoftReferenceMap<String, Object> CACHE =
			new SoftReferenceMap<String, Object>(new ConcurrentHashMap<String, Reference<Object>>());

	@Override
	public void add(String key, Object value, int expiration) {
		if (CACHE.get(key) == null)
			CACHE.put(key, value);
	}

	@Override
	public boolean safeAdd(String key, Object value, int expiration) {
		
		try {
			add(key, value, expiration);
			return true;
		} catch (Exception e) {
			Logger.error(e, "unable to add value: %s", value);
		}
		
		return false;
	}

	@Override
	public void set(String key, Object value, int expiration) {
		CACHE.put(key, value);
	}

	@Override
	public boolean safeSet(String key, Object value, int expiration) {
		try {
			set(key, value, expiration);
			return true;
		} catch (Exception e) {
			Logger.error(e, "unable to set value: %s", value);
		}
		
		return false;
	}

	@Override
	public void replace(String key, Object value, int expiration) {
		if (CACHE.get(key) != null)
			set(key, value, expiration);
	}

	@Override
	public boolean safeReplace(String key, Object value, int expiration) {
		try {
			replace(key, value, expiration);
			return true;
		} catch (Exception e) {
			Logger.error(e, "unable to replace value: %s", value);
		}
		
		return false;
	}

	@Override
	public Object get(String key) {
		return CACHE.get(key);
	}

	@Override
	public Map<String, Object> get(String[] keys) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		for (String key : keys) {
			map.put(key, get(key));
		}
		
		return map;
	}

	@Override
	public synchronized long incr(String key, int by) {
		Number number = (Number) get(key);
		long newValue = number.longValue() + by;
		if (safeSet(key, newValue, 0))
			return newValue;
		else
			return number.longValue();
	}

	@Override
	public synchronized long decr(String key, int by) {
		Number number = (Number) get(key);
		long newValue = number.longValue() - by;
		if (safeSet(key, newValue, 0))
			return newValue;
		else
			return number.longValue();
	}

	@Override
	public void clear() {
		CACHE.clear();
	}

	@Override
	public void delete(String key) {
		CACHE.remove(key);
	}

	@Override
	public boolean safeDelete(String key) {
		try {
			delete(key);
			return true;
		} catch (Exception e) {
			Logger.error(e, "unable to delete cache '%s'", key);
		}
		
		return false;
	}

	@Override
	public void stop() {
		clear();
	}

}
