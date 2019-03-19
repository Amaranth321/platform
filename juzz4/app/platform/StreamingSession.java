package platform;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.UUID;

//A simple Streaming Session object
public class StreamingSession
{
    public static int DEFAULT_SESSION_TTL = 300; //5 minutes
    public String sessionKey;
    public String userSession;
    public DateTime expiryTime;

    /**
     * Instantiate a StreamingSession object.
     *
     * @param loginSession The "session-key" of logged in user.
     */
    public StreamingSession(String loginSession)
    {
        sessionKey = UUID.randomUUID().toString();
        userSession = loginSession;
        expiryTime = DateTime.now(DateTimeZone.UTC).plusSeconds(StreamingSession.DEFAULT_SESSION_TTL); //default expire after 300 seconds
    }
}

