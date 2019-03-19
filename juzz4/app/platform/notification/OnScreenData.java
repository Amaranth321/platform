package platform.notification;

import com.google.gson.Gson;
import platform.events.EventManager;
import platform.events.EventType;
import platform.label.LabelNotificationType;
import play.i18n.Messages;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class OnScreenData
{
    private final long eventTime;
    private final String eventId;
    private final EventType eventType;
    private final String title;
    private final String itemName;
    private final NotificationSource source;
    private final Map notificationData;

    public static OnScreenData forCameraNotification(NotificationInfo info)
    {
        String title = Messages.get(info.getEventType().toString());
        return new OnScreenData(info.getEventTime(),
                                info.getEventId(),
                                info.getEventType(),
                                title,
                                info.getDeviceDisplayText(),
                                NotificationSource.CAMERA,
                                info.getNotificationData());
    }

    public static OnScreenData forLabelNotification(LabelNotificationInfo info)
    {
        LabelNotificationType type = info.getType();
        return new OnScreenData(info.getEventTime(),
                                info.getEventId(),
                                type.getEventType(),
                                type.onScreenDisplayName(info.getNotificationData()),
                                info.getLabelName(),
                                NotificationSource.LABEL,
                                info.getNotificationData());
    }

    public static OnScreenData forLocalCameraNotification(NotificationInfo info)
    {
        String title = Messages.get(info.getEventType().toString());
        return new OnScreenData(info.getEventTime(),
                                info.getEventId(),
                                info.getEventType(),
                                title,
                                info.getDeviceName(),
                                NotificationSource.CAMERA,
                                info.getNotificationData());
    }

    public String toApiOutput()
    {
        Map infoMap = new LinkedHashMap();
        infoMap.put("id", eventId);
        infoMap.put("timeMillis", eventTime);
        infoMap.put("title", title);
        infoMap.put("itemName", itemName);
        infoMap.put("source", source);
        infoMap.put("notificationData", notificationData);

        //backward compatibility
        infoMap.put("type", eventType.toString());
        infoMap.put("deviceName", itemName);
        infoMap.put("time", EventManager.getOriginalTime(eventTime));

        return new Gson().toJson(infoMap);
    }

    private OnScreenData(long eventTime,
                         String eventId,
                         EventType eventType,
                         String title,
                         String itemName,
                         NotificationSource source,
                         Map<String, Object> notificationData)
    {
        this.eventTime = eventTime;
        this.eventId = eventId;
        this.eventType = eventType;
        this.title = title;
        this.itemName = itemName;
        this.source = source;
        this.notificationData = notificationData;
    }
}
