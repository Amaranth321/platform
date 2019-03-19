package platform.db.cache.proxies;

import models.notification.LabelOccupancySettings;
import platform.analytics.occupancy.OccupancyLimit;
import platform.db.cache.CachedObject;

import java.util.TreeSet;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CachedOccupancySettings extends CachedObject<CachedOccupancySettings>
{
    private final String labelId;
    private final boolean enabled;
    private final TreeSet<OccupancyLimit> limits;
    private final int minNotifyIntervalSeconds;

    public CachedOccupancySettings(String cacheKey, LabelOccupancySettings settings)
    {
        super(cacheKey);
        labelId = settings.getLabelId();
        enabled = settings.isEnabled();
        limits = settings.getLimits();
        minNotifyIntervalSeconds = settings.getMinNotifyIntervalSeconds();
    }

    @Override
    public CachedOccupancySettings getObject()
    {
        return this;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public int getMinNotifyIntervalSeconds()
    {
        return minNotifyIntervalSeconds;
    }

    public OccupancyLimit getHighestLimitUnder(int occupancyCount)
    {
        OccupancyLimit cutOff = new OccupancyLimit(occupancyCount);
        return limits.floor(cutOff);
    }
}
