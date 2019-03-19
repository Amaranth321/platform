package models.transportobjects;

import models.notification.AckStatus;
import models.notification.AcknowledgementLog;
import models.notification.SentNotification;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.notification.NotificationInfo;
import platform.notification.NotifyMethod;

import java.util.*;

/**
 * for UI use
 *
 * @author Aye Maung
 * @since v4.4
 */
public class CameraNotificationTransport
{
    public final String sendVia;
    public final String deviceId;
    public final String channelId;
    public final Date time;
    public final String eventType;
    public final String eventId;
    public final long userId;
    public final String eventVideoUrl;
    public final String eventImageUrl;
    public final Map notificationData;
    public final long timeMillis;
    public final AckStatus ackStatus;
    public final String acknowledgedBy;
    public final long acknowledgedTime;

    public CameraNotificationTransport(long forUser, SentNotification dbNotification)
    {
        NotificationInfo notificationInfo = dbNotification.getNotificationInfo();

        userId = forUser;
        deviceId = notificationInfo.getCamera().getCoreDeviceId();
        channelId = notificationInfo.getCamera().getChannelId();
        time = new DateTime(notificationInfo.getEventTime(), DateTimeZone.UTC).toDate();
        timeMillis = notificationInfo.getEventTime();
        eventType = notificationInfo.getEventType().toString();
        eventId = notificationInfo.getEventId();
        eventVideoUrl = notificationInfo.getEventVideoUrl();
        notificationData = notificationInfo.getNotificationData();
        ackStatus = dbNotification.getAckStatus();
        eventImageUrl = notificationInfo.getEventImageUrl();
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
        Set<NotifyMethod> methods = dbNotification.getNotifiedMethodsFor(userId);
        List<String> localizedMethods = new ArrayList<>();
        for (NotifyMethod method : NotifyMethod.values()) //ensures the correct order
        {
            if (methods.contains(method))
            {
                localizedMethods.add(method.localizedName());
            }
        }
        sendVia = StringUtils.join(localizedMethods, ", ");
    }
}
