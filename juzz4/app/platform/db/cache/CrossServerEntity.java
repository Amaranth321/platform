package platform.db.cache;

import com.google.gson.Gson;
import play.cache.Cache;

/**
 * Uses play cache (memcached) to store temporary data required in the load-balanced environment.
 * If the objects of the inherited class can grow unbounded, use mongo {@link play.modules.morphia.Model} instead.
 *
 * @author Aye Maung
 * @since v4.4
 */
public abstract class CrossServerEntity
{
    private static final Gson gson = new Gson();
    private final String cacheKey;

    public static Object find(String cacheKey)
    {
        return Cache.get(cacheKey);
    }

    protected CrossServerEntity(String cacheKey)
    {
        this.cacheKey = cacheKey;
    }

    public void save()
    {
        Object existing = Cache.get(cacheKey);
        if (existing == null)
        {
            Cache.add(cacheKey, serialize());
        }
        else
        {
            Cache.replace(cacheKey, serialize());
        }
    }

    public void delete()
    {
        Object existing = Cache.get(cacheKey);
        if (existing != null)
        {
            Cache.delete(cacheKey);
        }
    }

    private String serialize()
    {
        return gson.toJson(this);
    }
}
