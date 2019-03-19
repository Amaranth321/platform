package platform.content.delivery;

import com.google.gson.Gson;
import platform.db.cache.CrossServerEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Only one record of this will exist
 *
 * @author Aye Maung
 * @since v4.4
 */
public class DeliveryStats extends CrossServerEntity
{
    private final Map<DeliveryMethod, Integer> successCounts;
    private final Map<DeliveryMethod, Integer> failCounts;
    private final Map<DeliveryMethod, Integer> remaining;

    public static DeliveryStats get()
    {
        Object o = find(getCachedKey());
        if (o != null)
        {
            return new Gson().fromJson((String) o, DeliveryStats.class);
        }
        DeliveryStats stats = new DeliveryStats();
        stats.save();
        return stats;
    }

    public static void reset(DeliveryMethod method)
    {
        DeliveryStats stats = get();
        stats.successCounts.put(method, 0);
        stats.failCounts.put(method, 0);
    }

    public int getSuccessCount(DeliveryMethod method)
    {
        if (!successCounts.containsKey(method))
        {
            return 0;
        }
        return successCounts.get(method);
    }

    public int getFailCount(DeliveryMethod method)
    {
        if (!failCounts.containsKey(method))
        {
            return 0;
        }
        return failCounts.get(method);
    }

    public int getRemaining(DeliveryMethod method)
    {
        if (!remaining.containsKey(method))
        {
            return 0;
        }
        return remaining.get(method);
    }

    public void incrementSuccess(DeliveryMethod method)
    {
        int current = getSuccessCount(method);
        successCounts.put(method, current + 1);
    }

    public void incrementFail(DeliveryMethod method)
    {
        int current = getFailCount(method);
        failCounts.put(method, current + 1);
    }

    public void setRemaining(DeliveryMethod method, int count)
    {
        remaining.put(method, count);
    }

    private static String getCachedKey()
    {
        return DeliveryStats.class.getName();
    }

    private DeliveryStats()
    {
        super(getCachedKey());
        this.successCounts = new LinkedHashMap<>();
        this.failCounts = new LinkedHashMap<>();
        this.remaining = new LinkedHashMap<>();
    }

}
