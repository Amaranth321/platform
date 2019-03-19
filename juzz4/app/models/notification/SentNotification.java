package models.notification;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import models.transportobjects.CameraNotificationTransport;
import platform.db.QueryHelper;
import platform.notification.NotificationInfo;
import platform.notification.NotifyMethod;
import play.modules.morphia.Model;

import java.util.*;

/**
 * Notifications that were sent out to users.
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
                 @Index("notificationInfo.eventId"),
                 @Index("notificationInfo.eventTime")
         })
public class SentNotification extends Model
{
    private final NotificationInfo notificationInfo;
    private final List<Long> userList;
    private final List<Set<NotifyMethod>> notifiedMethodsList;
    private AckStatus ackStatus;

    public static SentNotification createNew(NotificationInfo notificationInfo,
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

        SentNotification notification = new SentNotification(
                notificationInfo,
                users,
                methodsPerUser
        );
        return notification.save();
    }

    public static void removeEntriesOlderThan(int days)
    {
        QueryHelper.removeOlderThan(days, q(), "notificationInfo.eventTime");
    }

    private SentNotification(NotificationInfo notificationInfo,
                             List<Long> userList,
                             List<Set<NotifyMethod>> notifiedMethodsList)
    {
        this.notificationInfo = notificationInfo;
        this.userList = userList;
        this.notifiedMethodsList = notifiedMethodsList;
        this.ackStatus = AckStatus.NO_ACTION;
    }

    public NotificationInfo getNotificationInfo()
    {
        return notificationInfo;
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

