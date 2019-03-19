package models.events;

import com.google.code.morphia.annotations.Entity;
import models.abstracts.ServerPagedResult;
import platform.db.QueryHelper;
import platform.db.gridfs.GridFsDetails;
import platform.events.EventInfo;
import play.modules.morphia.Model;

/**
 * Event videos that are ready on Node, but not yet sent to Cloud
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class UnsyncedEventVideo extends Model
{
    private final EventInfo ownerEventInfo;
    private int failCount;

    public static synchronized UnsyncedEventVideo createNew(EventInfo ownerEventInfo)
    {
        UnsyncedEventVideo evtVid = new UnsyncedEventVideo(ownerEventInfo);
        return evtVid.save();
    }

    public static UnsyncedEventVideo getOldest()
    {
        return UnsyncedEventVideo.q().order("ownerEventInfo.time").first();
    }

    public static ServerPagedResult<UnsyncedEventVideo> query(int skip, int take)
    {
        MorphiaQuery query = UnsyncedEventVideo.q().order("_created");
        return QueryHelper.preparePagedResult(query, skip, take);
    }

    private UnsyncedEventVideo(EventInfo ownerEventInfo)
    {
        this.ownerEventInfo = ownerEventInfo;
        failCount = 0;
    }

    public void incrementFailCount()
    {
        ++failCount;
        this.save();
    }

    public EventInfo getOwnerEventInfo()
    {
        return ownerEventInfo;
    }

    public GridFsDetails getVideoDetails()
    {
        EventVideo eventVideo = EventVideo.find(ownerEventInfo.getEventId());
        return eventVideo == null ? null : eventVideo.getVideoDetails();
    }

    public int getFailCount()
    {
        return failCount;
    }
}
