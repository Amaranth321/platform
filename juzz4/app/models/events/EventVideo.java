package models.events;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import lib.util.Util;
import models.abstracts.ServerPagedResult;
import platform.db.QueryHelper;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsHelper;
import platform.events.EventInfo;
import play.Logger;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 * @since v4.4
 */
@Entity
@Indexes({
                 @Index("eventInfo.eventId")
         })
public class EventVideo extends Model
{
    private final EventInfo eventInfo;
    private final GridFsDetails videoDetails;

    public static EventVideo createNew(EventInfo eventInfo, GridFsDetails videoDetails)
    {
        Logger.debug("Event video ready (%s : %s)", eventInfo.getType(), videoDetails.getFilename());
        EventVideo eventVideo = new EventVideo(eventInfo, videoDetails);
        return eventVideo.save();
    }

    public static ServerPagedResult<EventVideo> query(int skip, int take)
    {
        MorphiaQuery query = EventVideo.q().order("-eventInfo.time");
        return QueryHelper.preparePagedResult(query, skip, take);
    }

    public static EventVideo find(String eventId)
    {
        return EventVideo.q().filter("eventInfo.eventId", eventId).first();
    }

    public static void removeEntriesOlderThan(int days)
    {
        //must loop in order to call overridden delete() function
        Iterable<EventVideo> videos = QueryHelper.getEntriesOlderThan(days, q(), "eventInfo.time").fetch();
        for (EventVideo video : videos)
        {
            video.delete();
        }
    }

    private EventVideo(EventInfo eventInfo,
                       GridFsDetails videoDetails)
    {
        this.eventInfo = eventInfo;
        this.videoDetails = videoDetails;
    }

    public EventInfo getInfo()
    {
        return eventInfo;
    }

    public GridFsDetails getVideoDetails()
    {
        return videoDetails;
    }

    @Override
    public EventVideo delete()
    {
        boolean result = GridFsHelper.removeFile(videoDetails);
        if (!result)
        {
            Logger.error(Util.whichFn() + "failed: %s", videoDetails.getFilename());
        }
        else
        {
            super.delete();
        }
        return this;
    }
}
