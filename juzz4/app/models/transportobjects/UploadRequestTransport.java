package models.transportobjects;

import models.RecordingUploadRequest;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedUser;
import platform.time.UtcPeriod;
import play.i18n.Messages;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class UploadRequestTransport
{
    public final String sessionKey;
    public final UtcPeriod period;
    public final long requesterId;
    public final String requesterName;

    public UploadRequestTransport(RecordingUploadRequest dbRequest)
    {
        this.sessionKey = dbRequest.getSessionKey();
        this.period = dbRequest.getPeriod();
        this.requesterId = dbRequest.getRequesterUserId();

        CachedUser user = CacheClient.getInstance().getUser(dbRequest.getRequesterUserId() + "");
        this.requesterName = user == null ? Messages.get("deleted-db-entry") : user.getName();
    }
}
