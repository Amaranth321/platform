package platform.notification;

import models.notification.LabelEventToNotify;
import platform.common.Location;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedStoreLabel;
import platform.label.LabelNotificationType;
import play.i18n.Messages;

import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class LabelNotificationInfo
{
    private final String eventId;
    private final long eventTime;
    private final LabelNotificationType type;
    private final String labelId;
    private final String labelName;
    private final Location location;
    private final Map notificationData;

    public static LabelNotificationInfo forEvent(LabelEventToNotify eventToNotify)
    {
        CachedStoreLabel storeLabel = CacheClient.getInstance().getStoreLabel(eventToNotify.getLabelId());
        return new LabelNotificationInfo(eventToNotify.getEventId(),
                                         eventToNotify.getTime(),
                                         eventToNotify.getType(),
                                         eventToNotify.getLabelId(),
                                         storeLabel.getLabelName(),
                                         storeLabel.getLocation(),
                                         eventToNotify.getNotificationData()
        );
    }

    public LabelNotificationInfo(String eventId,
                                 long eventTime,
                                 LabelNotificationType type,
                                 String labelId,
                                 String labelName, Location location, Map notificationData)
    {
        this.eventId = eventId;
        this.eventTime = eventTime;
        this.type = type;
        this.labelId = labelId;
        this.labelName = labelName;
        this.location = location;
        this.notificationData = notificationData;
    }

    public String getEventId()
    {
        return eventId;
    }

    public long getEventTime()
    {
        return eventTime;
    }

    public LabelNotificationType getType()
    {
        return type;
    }

    public String getLabelId()
    {
        return labelId;
    }

    public String getLabelName()
    {
        return labelName;
    }

    public Location getLocation()
    {
        return location;
    }

    public Map getNotificationData()
    {
        return notificationData;
    }

    public String getLocalizedEventName()
    {
        return Messages.get(type.getEventType().toString());
    }
}
