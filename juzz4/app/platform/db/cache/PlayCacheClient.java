package platform.db.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lib.util.Util;
import platform.Environment;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import play.Logger;
import play.cache.Cache;

import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 * @since v4.4
 */
class PlayCacheClient<T extends CachedObject>
{
    private static final int MIN_TTL_SECONDS = 30;
    private static final int MAX_TTL_SECONDS = 24 * 60 * 60;    //1 day
    private static final Gson gson;

    private final Class<T> clazz;

    static
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(EventType.class, new EventType.Deserializer());
        gsonBuilder.registerTypeAdapter(DeviceChannelPair.class, new DeviceChannelPair.Deserializer());
        gson = gsonBuilder.create();
    }

    public static void clearAll()
    {
        Cache.clear();
    }

    public PlayCacheClient(Class<T> clazz)
    {
        this.clazz = clazz;
    }

    public T get(String cacheKey)
    {
        Object o = play.cache.Cache.get(getClassBasedCacheKey(cacheKey));
        return o == null ? null : deserialize((String) o);
    }

    public void add(T cachedObject)
    {
        if (Environment.getInstance().onKaiNode())
        {
            //users expect instant updates on nodes and minor delays caused by caching is not acceptable
            //nodes do not have a high number of db queries to benefit from caching anyway.
            //DO NOT enable this. ALL callers of functions under CacheClient assumed that caching is disabled on nodes.
            return;
        }

        String key = getClassBasedCacheKey(cachedObject.getCacheKey());
        String expiration = toSecondFormat(cachedObject.getTtlMillis());
        play.cache.Cache.safeAdd(key, serialize(cachedObject), expiration);
        Logger.debug("Cache added (%s)", key);
    }

    public void update(T cachedObject)
    {
        String key = getClassBasedCacheKey(cachedObject.getCacheKey());
        String expiration = toSecondFormat(cachedObject.getTtlMillis());
        play.cache.Cache.safeReplace(key, serialize(cachedObject), expiration);
        Logger.debug("Cache updated (%s)", key);
    }

    public void remove(T cachedObject)
    {
        String key = getClassBasedCacheKey(cachedObject.getCacheKey());
        play.cache.Cache.safeDelete(key);
        Logger.debug("Cache removed (%s)", key);
    }

    private String getClassBasedCacheKey(String cacheKey)
    {
        return String.format("%s_%s", clazz.getSimpleName(), cacheKey);
    }

    private String toSecondFormat(long ttlMillis)
    {
        int jitteredTtl = getJitteredTtl(ttlMillis);
        long ttlSeconds = TimeUnit.MILLISECONDS.toSeconds(jitteredTtl);
        ttlSeconds = ttlSeconds < MIN_TTL_SECONDS ? MIN_TTL_SECONDS : ttlSeconds;
        ttlSeconds = ttlSeconds > MAX_TTL_SECONDS ? MAX_TTL_SECONDS : ttlSeconds;
        return ttlSeconds + "s";
    }

    /**
     * this prevents multiple objects from expiring at the exact same time
     */
    private int getJitteredTtl(long exactTtl)
    {
        int percent10 = Math.round(0.1f * exactTtl);
        Long min = exactTtl - percent10;
        Long max = exactTtl + percent10;
        return Util.randInt(min.intValue(), max.intValue());
    }

    private String serialize(T cachedObject)
    {
        return gson.toJson(cachedObject);
    }

    private T deserialize(String json)
    {
        return gson.fromJson(json, clazz);
    }
}
