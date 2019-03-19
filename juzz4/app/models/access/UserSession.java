package models.access;

import com.google.code.morphia.annotations.Entity;
import play.modules.morphia.Model;

import java.util.UUID;

/**
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class UserSession extends Model
{
    private final String sessionKey;
    private final long bucketId;
    private final long userId;
    private final String ipAddress;
    private final String userAgent;
    private long expiry;

    public static UserSession find(String sessionKey)
    {
        return UserSession.q().filter("sessionKey", sessionKey).first();
    }

    public UserSession(String bucketId, String userId, String ipAddress, String userAgent, long expiry)
    {
        this.sessionKey = UUID.randomUUID().toString();
        this.bucketId = Long.parseLong(bucketId);
        this.userId = Long.parseLong(userId);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.expiry = expiry;
    }

    public String getSessionKey()
    {
        return sessionKey;
    }

    public long getBucketId()
    {
        return bucketId;
    }

    public long getUserId()
    {
        return userId;
    }

    public String getIpAddress()
    {
        return ipAddress;
    }

    public String getUserAgent()
    {
        return userAgent;
    }

    public long getExpiry()
    {
        return expiry;
    }

    public boolean hasExpired()
    {
        return System.currentTimeMillis() > expiry;
    }

    public void extend(long ttlMillis)
    {
        expiry = System.currentTimeMillis() + ttlMillis;
    }
}
