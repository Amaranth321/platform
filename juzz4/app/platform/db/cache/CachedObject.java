package platform.db.cache;

import java.util.concurrent.TimeUnit;

/**
 * Override {@link #getTtlMillis()} to use custom ttl
 * <p/>
 * DO NOT cache db object itself, cache the fields instead
 *
 * @author Aye Maung
 * @since v4.4
 */
public abstract class CachedObject<T>
{
    private final String cacheKey;

    protected CachedObject(String cacheKey)
    {
        this.cacheKey = cacheKey;
    }

    public long getTtlMillis()
    {
        return Math.round(TimeUnit.MINUTES.toMillis(15));   //15 mins
    }

    public String getCacheKey()
    {
        return cacheKey;
    }

    public abstract T getObject();
}
