package models.notification;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import models.transportobjects.CameraNotificationTransport;
import platform.common.Location;
import platform.db.QueryHelper;
import platform.events.EventType;
import platform.notification.LabelNotificationInfo;
import platform.notification.NotifyMethod;
import play.modules.morphia.Model;

import java.util.*;

/**
 * Label Notifications that were sent out to users.
 * <p/>
 * Notes:
 * - userList and notifiedMethodsList are stored in the same order. They will always tally.
 * - Use {@link CameraNotificationTransport} for UI
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
@Indexes({
                 @Index("labelId"),
                 @Index("eventTime"),
                 @Index("eventId")
         })
public class SentLabelNotification extends Model
{
    private final String eventId;
    private final long eventTime;
    private final EventType eventType;
    private final String labelId;
    private final Location location;
    private final Map notificationData;
    private final List<Long> userList;
    private final List<Set<NotifyMethod>> notifiedMethodsList;
    private AckStatus ackStatus;

    public static SentLabelNotification createNew(LabelNotificationInfo notificationInfo,
                                                  Map<Long, Set<NotifyMethod>> notifiedUsers)
    {
        //convert map to two lists
        List<Long> users = new ArrayList<>();
        List<Set<NotifyMethod>> methodsPerUser = new ArrayList<>();
        for (Long userId : notifiedUsers.keySet())
        {
            Set<NotifyMethod> methods = notifiedUsers.get(userId);
            if (methods.isEmpty())
            {
                continue;
            }
            users.add(userId);
            methodsPerUser.add(methods);
        }

        //don't save if user list is empty
        if (users.isEmpty())
        {
            return null;
        }

        SentLabelNotification notification = new SentLabelNotification(notificationInfo, users, methodsPerUser);
        return notification.save();
    }

    public static void removeEntriesOlderThan(int days)
    {
        QueryHelper.removeOlderThan(days, q(), "eventTime");
    }

    private SentLabelNotification(LabelNotificationInfo notificationInfo,
                                  List<Long> userList,
                                  List<Set<NotifyMethod>> notifiedMethodsList)
    {
        this.eventId = notificationInfo.getEventId();
        this.eventTime = notificationInfo.getEventTime();
        this.eventType = notificationInfo.getType().getEventType();
        this.labelId = notificationInfo.getLabelId();
        this.location = notificationInfo.getLocation();
        this.notificationData = notificationInfo.getNotificationData();
        this.userList = userList;
        this.notifiedMethodsList = notifiedMethodsList;
        this.ackStatus = AckStatus.NO_ACTION;
    }

    public String getEventId()
    {
        return eventId;
    }

    public long getEventTime()
    {
        return eventTime;
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public String getLabelId()
    {
        return labelId;
    }

    public Location getLocation()
    {
        return location;
    }

    public Map getNotificationData()
    {
        return notificationData;
    }

    public List<Long> getUserList()
    {
        return userList;
    }

    public Set<NotifyMethod> getNotifiedMethodsFor(long userId)
    {
        int index = userList.indexOf(userId);
        if (index < 0)
        {
            return new HashSet<>();
        }
        return notifiedMethodsList.get(index);
    }

    public AckStatus getAckStatus()
    {
        return ackStatus == null ? AckStatus.NO_ACTION : ackStatus;
    }

    public void setAckStatus(AckStatus ackStatus)
    {
        this.ackStatus = ackStatus;
    }

}

