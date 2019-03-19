package models.notification;

import com.google.code.morphia.annotations.Entity;
import models.abstracts.ServerPagedResult;
import platform.db.QueryHelper;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedBucket;
import platform.db.cache.proxies.CachedDevice;
import platform.events.EventInfo;
import play.modules.morphia.Model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class EventToNotify extends Model
{
    private final long bucketId;
    private final EventInfo eventInfo;
    private final Map notificationData;

    public static void queue(EventInfo eventInfo)
    {
        queue(eventInfo, new LinkedHashMap());
    }

    public static void queue(EventInfo eventInfo, Map notificationData)
    {
        //check bucket alert settings
        CacheClient cacheClient = CacheClient.getInstance();
        CachedDevice cachedDevice = cacheClient.getDeviceByCoreId(eventInfo.getCamera().getCoreDeviceId());
        CachedBucket cachedBucket = cacheClient.getBucket(cachedDevice.getBucketId());
        if (!cachedBucket.isNotificationEnabled(eventInfo.getType()))
        {
            return;
        }

        EventToNotify evt = new EventToNotify(Long.parseLong(cachedDevice.getBucketId()), eventInfo, notificationData);
        evt.save();
    }

    public static EventToNotify getOldest()
    {
        return EventToNotify.q().order("_created").first();
    }

    public static ServerPagedResult<EventToNotify> query(int skip, int take)
    {
        MorphiaQuery query = EventToNotify.q().order("_created");
        return QueryHelper.preparePagedResult(query, skip, take);
    }

    private EventToNotify(long bucketId, EventInfo eventInfo, Map notificationData)
    {
        this.bucketId = bucketId;
        this.eventInfo = eventInfo;
        this.notificationData = notificationData;
    }

    public long getBucketId()
    {
        return bucketId;
    }

    public EventInfo getEventInfo()
    {
        return eventInfo;
    }

    public Map getNotificationData()
    {
        return notificationData;
    }
}
