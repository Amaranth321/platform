package models.transportobjects;

import platform.db.cache.proxies.CachedLoginSession;

/**
 * @author Aye Maung
 */
public class UserSessionTransport
{
    public final String sessionKey;
    public final long expiry;

    public UserSessionTransport(CachedLoginSession dbSession)
    {
        this.sessionKey = dbSession.getSessionKey();
        this.expiry = dbSession.getExpiry();
    }
}
