package platform.coreengine;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CoreSession
{
    private final List<String> urlList;
    private final String sessionKey;
    private final int ttlSeconds;
    private final String clientIp;

    public CoreSession(List<String> urlList,
                       String sessionKey,
                       int ttlSeconds,
                       String clientIp)
    {
        this.urlList = urlList;
        this.sessionKey = sessionKey;
        this.ttlSeconds = ttlSeconds;
        this.clientIp = clientIp;
    }

    public List<String> getUrlList()
    {
        return urlList;
    }

    public String getSessionKey()
    {
        return sessionKey;
    }

    public int getTtlSeconds()
    {
        return ttlSeconds;
    }

    public String getClientIp()
    {
        return clientIp;
    }
}
