package platform.access;

import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoBucket;
import models.MongoUser;
import models.access.UserSession;
import org.apache.commons.lang.RandomStringUtils;
import platform.config.readers.ConfigsShared;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedLoginSession;
import play.cache.Cache;

import java.util.concurrent.TimeUnit;

/**
 * This manager uses the combination of db Model and CacheClient to manage the sessions.
 * <p/>
 * Both WebInterceptor and APIInterceptor check session validity very frequently,
 * hence cached copy will be used mostly.
 *
 * @author Aye Maung
 * @since v4.4
 */
public enum UserSessionManager
{
    INSTANCE;

    private static long REMEMBER_ME_TTL = TimeUnit.DAYS.toMillis(14);
    private static long KEEP_ALIVE_THRESHOLD = TimeUnit.HOURS.toMillis(6);
    private static long URL_REDIRECT_CACHE_SECONDS = 5 * 60;

    public static UserSessionManager getInstance()
    {
        return INSTANCE;
    }

    public long getDefaultTtl()
    {
        int defaultTtlMins = ConfigsShared.getInstance().defaultSessionTimeOutMins();
        return TimeUnit.MINUTES.toMillis(defaultTtlMins);
    }

    public UserSession createSession(String userId, String ipAddress, String userAgent, boolean rememberMe)
    {
        long ttlMillis = rememberMe ? REMEMBER_ME_TTL : getDefaultTtl();
        long expiry = System.currentTimeMillis() + ttlMillis;
        MongoUser user = MongoUser.getById(userId);
        MongoBucket bucket = MongoBucket.getById(user.getBucketId());

        UserSession userSession = new UserSession(bucket.getBucketId(), user.getUserId(), ipAddress, userAgent, expiry);
        return userSession.save();
    }

    public boolean isSessionValid(String sessionKey)
    {
        if (Util.isNullOrEmpty(sessionKey))
        {
            return false;
        }

        CachedLoginSession cachedSession = findSession(sessionKey);
        if (cachedSession == null)
        {
            return false;
        }

        if (cachedSession.hasExpired())
        {
            destroy(sessionKey);
            return false;
        }

        return true;
    }

    public CachedLoginSession findSession(String sessionKey)
    {
        if (Util.isNullOrEmpty(sessionKey))
        {
            return null;
        }

        return CacheClient.getInstance().getLoginSession(sessionKey);
    }

    public CachedLoginSession keepSessionAlive(CachedLoginSession cachedSession) throws ApiException
    {
        if (cachedSession == null || cachedSession.hasExpired())
        {
            throw new ApiException("session-expired");
        }

        //extend if session is expiring in less than KEEP_ALIVE_THRESHOLD
        if ((cachedSession.getExpiry() - System.currentTimeMillis()) < KEEP_ALIVE_THRESHOLD)
        {
            UserSession dbSession = UserSession.find(cachedSession.getSessionKey());
            dbSession.extend(getDefaultTtl());
            dbSession.save();
            removeCachedSession(cachedSession.getSessionKey());
        }

        return CacheClient.getInstance().getLoginSession(cachedSession.getSessionKey());
    }

    public void removeSessionsOfUser(String userId)
    {
        Iterable<UserSession> sessions = UserSession.q().filter("userId", userId).fetch();
        for (UserSession session : sessions)
        {
            destroy(session.getSessionKey());
        }
    }

    public void removeSessionsOfBucket(String bucketId)
    {
        Iterable<UserSession> sessions = UserSession.q().filter("bucketId", Long.parseLong(bucketId)).fetch();
        for (UserSession session : sessions)
        {
            destroy(session.getSessionKey());
        }
    }

    public void removeExpiredSessions()
    {
        Iterable<UserSession> expiredSessions = UserSession.q().filter("expiry <", System.currentTimeMillis()).fetch();
        for (UserSession expiredSession : expiredSessions)
        {
            destroy(expiredSession.getSessionKey());
        }
    }

    public void destroy(String sessionKey)
    {
        UserSession.q().filter("sessionKey", sessionKey).delete();
        removeCachedSession(sessionKey);
    }

    public String generateRedirectId(String absoluteUrlPath)
    {
        if (Util.isNullOrEmpty(absoluteUrlPath) || absoluteUrlPath.trim().equals("/"))
        {
            return null;
        }

        //cache
        String redirectId = RandomStringUtils.randomAlphanumeric(40).toLowerCase();
        String cacheKey = "redirect_" + redirectId;
        Cache.add(cacheKey, absoluteUrlPath, URL_REDIRECT_CACHE_SECONDS + "s");
        return redirectId;
    }

    public String getCachedUrlPath(String redirectId)
    {
        String cacheKey = "redirect_" + redirectId;
        Object value = Cache.get(cacheKey);

        String path = "";
        if (value != null)
        {
            path = String.valueOf(value);
            Cache.delete(cacheKey);
        }

        return path;
    }

    private void removeCachedSession(String sessionKey)
    {
        CachedLoginSession cachedSession = CacheClient.getInstance().getLoginSession(sessionKey);
        if (cachedSession != null)
        {
            CacheClient.getInstance().remove(cachedSession);
        }
    }
}
