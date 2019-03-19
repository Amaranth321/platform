package platform.data.collective;

import models.notification.LabelEventToNotify;
import platform.analytics.occupancy.OccupancyLimit;
import platform.data.Triggerable;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedOccupancySettings;
import platform.devices.DeviceChannelPair;
import platform.rt.RTFeedManager;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class OccupancyDataCollector extends LabelDataCollector
{
    private final OccupancyData occupancyData;

    public OccupancyDataCollector(String labelId, OccupancyData occupancyData)
    {
        super(labelId);
        this.occupancyData = occupancyData;
        addTrigger(new RealTimeTrigger());
        addTrigger(new NotificationTrigger());
    }

    @Override
    protected void compile(Object... data)
    {
        DeviceChannelPair camera = (DeviceChannelPair) data[0];
        Integer newOccupancy = (Integer) data[1];
        occupancyData.update(camera, newOccupancy);
        Logger.info("[%s] updated (camera:%s, occupancy:%s)", getLabelId(), camera, newOccupancy);
    }

    public OccupancyData getOccupancyData()
    {
        return occupancyData;
    }

    private class RealTimeTrigger implements Triggerable
    {
        @Override
        public void checkAndTrigger()
        {
            //inform web socket listeners
            RTFeedManager.getInstance().collectiveOccupancyChanged(occupancyData);
        }
    }

    private class NotificationTrigger implements Triggerable
    {
        @Override
        public void checkAndTrigger()
        {
            CachedOccupancySettings occSettings = CacheClient.getInstance().getOccupancySettings(getLabelId());
            if (occSettings == null || !occSettings.isEnabled())
            {
                return;
            }

            //check breached limit
            int collectiveOccupancy = occupancyData.getCollectiveOccupancy();
            OccupancyLimit breachedLimit = occSettings.getHighestLimitUnder(collectiveOccupancy);
            if (breachedLimit == null)
            {
                occupancyData.dropsBelowAllLimits(collectiveOccupancy);
                return;
            }

            if (!occupancyData.shouldSendNotification(breachedLimit.getLimit(),
                                                      collectiveOccupancy,
                                                      occSettings.getMinNotifyIntervalSeconds()))
            {
                return;
            }

            //send
            LabelEventToNotify.queueOccupancy(getLabelId(), breachedLimit);
            occupancyData.notificationSent(breachedLimit.getLimit());
            Logger.info("[%s] queued for occupancy notification : %s", getLabelId(), collectiveOccupancy);
        }
    }
}
