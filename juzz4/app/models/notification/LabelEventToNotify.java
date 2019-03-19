package models.notification;

import com.google.code.morphia.annotations.Entity;
import lib.util.Util;
import models.labels.DeviceLabel;
import models.labels.LabelStore;
import platform.analytics.occupancy.OccupancyLimit;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedBucket;
import platform.label.LabelNotificationType;
import play.Logger;
import play.modules.morphia.Model;

import java.util.Map;
import java.util.UUID;

/**
 * Only for notifications triggered by labels (which will have a group of assigned cameras).
 * <p/>
 * As such, there will be no device information. You may try querying the list of assigned cameras
 * based on the label id. But note that the list may have changed since the notification.
 *
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class LabelEventToNotify extends Model
{
    private final String eventId;
    private final long time;
    private final long bucketId;
    private final String labelId;
    private final LabelNotificationType type;
    private final Map notificationData;

    /**
     * @param labelId       label id. Only store labels can have occupancy notifications
     * @param breachedLimit breached limit
     */
    public static void queueOccupancy(String labelId, OccupancyLimit breachedLimit)
    {
        LabelNotificationType labelNotiType = LabelNotificationType.OCCUPANCY;

        //find label
        LabelStore label = LabelStore.q().filter("labelId", labelId).first();
        if (label == null)
        {
            Logger.info("[%s] store label not found (%s). Skipped notification.", Util.whichClass(), labelId);
            return;
        }

        //check bucket settings
        CachedBucket cachedBucket = CacheClient.getInstance().getBucket(label.getBucketId() + "");
        if (!cachedBucket.isNotificationEnabled(labelNotiType.getEventType()))
        {
            return;
        }

        Map notiData = labelNotiType.packageData(breachedLimit.getLimit(), breachedLimit.getAlertMessage());
        new LabelEventToNotify(label, labelNotiType, notiData).save();
    }

    public static LabelEventToNotify getOldest()
    {
        return q().order("_created").first();
    }

    private LabelEventToNotify(DeviceLabel label, LabelNotificationType type, Map notificationData)
    {
        this.bucketId = label.getBucketId();
        this.labelId = label.getLabelId();
        this.type = type;
        this.notificationData = notificationData;

        this.eventId = UUID.randomUUID().toString();
        this.time = System.currentTimeMillis();
    }

    public String getEventId()
    {
        return eventId;
    }

    public long getTime()
    {
        return time;
    }

    public long getBucketId()
    {
        return bucketId;
    }

    public String getLabelId()
    {
        return labelId;
    }

    public LabelNotificationType getType()
    {
        return type;
    }

    public Map getNotificationData()
    {
        return notificationData;
    }
}
