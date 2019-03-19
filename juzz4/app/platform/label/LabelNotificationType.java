package platform.label;

import platform.events.EventType;
import play.i18n.Messages;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.5
 */
public enum LabelNotificationType
{
    OCCUPANCY(EventType.OCCUPANCY_LIMIT);

    private final EventType eventType;

    LabelNotificationType(EventType eventType)
    {
        this.eventType = eventType;
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public Map<String, Object> packageData(Object... data)
    {
        Map<String, Object> dataMap = new LinkedHashMap<>();

        /**
         * DO NOT modify the entries below without checking the dependent parts (UI, emails, etc ...)
         */
        switch (this)
        {
            case OCCUPANCY:
                dataMap.put("occupancyLimit", data[0]);
                dataMap.put("message", data[1]);
                return dataMap;

        }

        throw new IllegalStateException(this.name());
    }

    public String onScreenDisplayName(Map<String, Object> notificationData)
    {
        String eventName = Messages.get(getEventType().toString());
        switch (this)
        {
            case OCCUPANCY:
                String limit = String.valueOf(notificationData.get("occupancyLimit"));
                return String.format("%s (Limit=%s)", eventName, limit);

            default:
                return eventName;
        }
    }
}
