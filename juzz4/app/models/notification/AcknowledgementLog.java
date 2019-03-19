package models.notification;

import com.google.code.morphia.annotations.Entity;
import models.MongoUser;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedUser;
import play.modules.morphia.Model;

/**
 * This can be used for purposes other than notifications.
 * But, make sure eventId is a UUID.
 *
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class AcknowledgementLog extends Model
{
    private final String eventId;
    private final AckStatus ackStatus;
    private final String message;
    private final long userId;              //user who acknowledged it
    private final String userFullName;     //full name in case user got deleted

    public static MorphiaQuery findByEventId(String eventId)
    {
        return q().filter("eventId", eventId);
    }

    public static void save(String eventId, AckStatus ackStatus, String message, MongoUser user)
    {
        AcknowledgementLog ackLog = new AcknowledgementLog(eventId, ackStatus, message, Long.parseLong(user.getUserId()), user.getName());
        ackLog.save();
    }

    private AcknowledgementLog(String eventId,
                               AckStatus ackStatus,
                               String message,
                               long userId, String userFullName)
    {
        this.eventId = eventId;
        this.ackStatus = ackStatus;
        this.message = message;
        this.userId = userId;
        this.userFullName = userFullName;
    }

    public AckStatus getAckStatus()
    {
        return ackStatus;
    }

    public String getUserFullName()
    {
        CachedUser user = CacheClient.getInstance().getUser(userId + "");
        return user == null ? userFullName : user.getName();
    }

    public String getMessage()
    {
        return message;
    }
}
