package platform.data.collective;

import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedOccupancyData;

/**
 * @author Aye Maung
 * @since v4.5
 */
public enum DataCollectorFactory
{
    INSTANCE;

    private final CacheClient cacheClient = CacheClient.getInstance();

    public static DataCollectorFactory getInstance()
    {
        return INSTANCE;
    }

    public LabelDataCollector getCollector(Class<? extends LabelDataCollector> collectorClass, String labelId)
    {
        if (OccupancyDataCollector.class.equals(collectorClass))
        {
            //retrieve from cache
            OccupancyData currentData = cacheClient.getOccupancyData(labelId).getOccupancyData();
            return new OccupancyDataCollector(labelId, currentData);
        }

        throw new UnsupportedOperationException();
    }

    /**
     * This method will be automatically called by  {@link LabelDataCollector#collect(Object...)}
     * <p/>
     * If collectors do not need to survive the server restart, put them on memory.
     * Otherwise, cache them on memcached.
     * Or, save them in db if the change/query frequency is estimated to be not too high.
     */
    public void save(LabelDataCollector collector)
    {
        if (collector.getClass().equals(OccupancyDataCollector.class))
        {
            OccupancyDataCollector occupancyCollector = (OccupancyDataCollector) collector;

            //replace the cache
            CachedOccupancyData cache = cacheClient.getOccupancyData(collector.getLabelId());
            cache.setOccupancyData(occupancyCollector.getOccupancyData());
            cacheClient.update(cache);
        }
    }
}
