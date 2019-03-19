package platform.db.cache.proxies;

import models.MongoBucket;
import models.MongoUser;
import models.access.UserSession;
import platform.db.cache.CachedObject;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CachedLoginSession extends CachedObject<CachedLoginSession>
{
    private final String sessionKey;
    private final String bucketId;
    private final String userId;
    private final String ipAddress;
    private final String userAgent;
    private final long expiry;

    private final String bucketName;
    private final String userLoginName;
    private final String userFullName;

    public CachedLoginSession(String cacheKey, UserSession dbSession, MongoBucket bucket, MongoUser user)
    {
        super(cacheKey);

        this.sessionKey = dbSession.getSessionKey();
        this.bucketId = dbSession.getBucketId() + "";
        this.userId = dbSession.getUserId() + "";
        this.ipAddress = dbSession.getIpAddress();
        this.userAgent = dbSession.getUserAgent();
        this.expiry = dbSession.getExpiry();
        this.bucketName = bucket.getName();
        this.userLoginName = user.getLogin();
        this.userFullName = user.getName();
    }

    @Override
    public CachedLoginSession getObject()
    {
        return this;
    }

    public String getSessionKey()
    {
        return sessionKey;
    }

    public String getBucketId()
    {
        return bucketId;
    }

    public String getBucketName()
    {
        return bucketName;
    }

    public String getUserId()
    {
        return userId;
    }

    public String getUserLoginName()
    {
        return userLoginName;
    }

    public String getUserFullName()
    {
        return userFullName;
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

}
