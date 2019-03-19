package platform.db.cache.proxies;

import models.MongoBucket;
import models.MongoUser;
import models.notification.BucketNotificationSettings;
import platform.db.cache.CachedObject;
import platform.events.EventType;

import java.util.*;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CachedBucket extends CachedObject<CachedBucket>
{
    private final String name;
    private final String path;
    private final String description;
    private final String parentId;
    private final boolean activated;
    private final boolean deleted;
    private final Set<String> userIdSet;
    private final Map<EventType, BucketNotificationSettings.EventTypeSettings> notificationSettings;

    public CachedBucket(String identifier, MongoBucket dbBucket)
    {
        super(identifier);

        name = dbBucket.getName();
        path = dbBucket.getPath();
        description = dbBucket.getDescription();
        parentId = dbBucket.getParentId();
        activated = dbBucket.isActivated();
        deleted = dbBucket.isDeleted();

        //user list
        userIdSet = new HashSet<>();
        List<MongoUser> bucketUsers = MongoUser.q().filter("bucketId", dbBucket.getBucketId()).fetchAll();
        for (MongoUser user : bucketUsers)
        {
            userIdSet.add(user.getUserId());
        }

        //notification
        notificationSettings = new LinkedHashMap<>();
        BucketNotificationSettings notiSetts = dbBucket.getNotificationSettings();
        for (EventType eventType : notiSetts.getSupportedEventTypes())
        {
            notificationSettings.put(eventType, notiSetts.getSettingsByType(eventType));
        }
    }

    @Override
    public CachedBucket getObject()
    {
        return this;
    }

    public String getName()
    {
        return name;
    }

    public String getPath()
    {
        return path;
    }

    public String getDescription()
    {
        return description;
    }

    public String getParentId()
    {
        return parentId;
    }

    public boolean isActivated()
    {
        return activated;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public Set<String> getUserIdSet()
    {
        return userIdSet;
    }

    public boolean isNotificationEnabled(EventType eventType)
    {
        BucketNotificationSettings.EventTypeSettings typeSetts = notificationSettings.get(eventType);
        return typeSetts != null && typeSetts.isNotificationEnabled();
    }

    public boolean isVideoEnabled(EventType eventType)
    {
        BucketNotificationSettings.EventTypeSettings typeSetts = notificationSettings.get(eventType);
        if (typeSetts == null || !typeSetts.isNotificationEnabled())
        {
            return false;
        }

        return typeSetts.isVideoRequired();
    }
}
