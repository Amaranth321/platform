package platform.db.cache.proxies;

import models.events.EventVideo;
import platform.db.cache.CachedObject;
import platform.db.gridfs.GridFsDetails;
import platform.events.EventInfo;

import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CachedEventVideo extends CachedObject<CachedEventVideo>
{
    private final EventInfo eventInfo;
    private final GridFsDetails videoDetails;

    public CachedEventVideo(String cacheKey, EventVideo dbVideo)
    {
        super(cacheKey);

        this.eventInfo = dbVideo.getInfo();
        this.videoDetails = dbVideo.getVideoDetails();
    }

    @Override
    public long getTtlMillis()
    {
        return TimeUnit.DAYS.toMillis(1);
    }

    @Override
    public CachedEventVideo getObject()
    {
        return this;
    }

    public EventInfo getEventInfo()
    {
        return eventInfo;
    }

    public GridFsDetails getVideoDetails()
    {
        return videoDetails;
    }
}
