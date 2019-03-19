package models.transportobjects;

import models.notification.AckStatus;
import models.notification.AcknowledgementLog;
import models.notification.SentLabelNotification;
import platform.notification.NotifyMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class LabelNotificationTransport
{
    public final long userId;
    public final long time;
    public final String eventType;
    public final String eventId;
    public final String labelId;
    public final Map notificationData;
    public final List<String> notifiedMethods;
    public final AckStatus ackStatus;
    public final String acknowledgedBy;
    public final long acknowledgedTime;

    public LabelNotificationTransport(long userId, SentLabelNotification dbEntry)
    {
        this.userId = userId;
        this.time = dbEntry.getEventTime();
        this.eventType = dbEntry.getEventType().toString();
        this.eventId = dbEntry.getEventId();
        this.labelId = dbEntry.getLabelId();
        this.notificationData = dbEntry.getNotificationData();
        this.ackStatus = dbEntry.getAckStatus();

        //acknowledged by
        String userName = null;
        long acknowledgedTime = 0;
        if (ackStatus == AckStatus.ACKNOWLEDGED)
        {
            AcknowledgementLog log = AcknowledgementLog.findByEventId(eventId).first();
            if (log != null)
            {
                userName = log.getUserFullName();
                acknowledgedTime = log._getCreated();
            }
        }
        this.acknowledgedBy = userName;
        this.acknowledgedTime = acknowledgedTime;

        //sent methods
        this.notifiedMethods = new ArrayList<>();
        Set<NotifyMethod> notifiedMethods = dbEntry.getNotifiedMethodsFor(userId);
        for (NotifyMethod method : NotifyMethod.values()) //to maintain the standard order
        {
            if (notifiedMethods.contains(method))
            {
                this.notifiedMethods.add(method.localizedName());
            }
        }
    }
}
