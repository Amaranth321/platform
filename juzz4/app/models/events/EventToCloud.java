package models.events;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import lib.util.Util;
import models.abstracts.ServerPagedResult;
import platform.db.QueryHelper;
import platform.events.EventInfo;
import play.Logger;
import play.modules.morphia.Model;

/**
 * Events that are received on Node, but not sent to Cloud yet.
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
@Indexes({
        @Index("eventInfo.eventId")
})
public class EventToCloud extends Model
{
    private final EventInfo eventInfo;
    private final String jsonData;
    private final byte[] binaryData;

    public static synchronized EventToCloud createNew(EventInfo eventInfo, String jsonData, byte[] binaryData)
    {
        //Duplicate checking for debugging
        EventToCloud duplicate = EventToCloud.find("eventInfo.eventId", eventInfo.getEventId()).first();
        if (duplicate != null)
        {
            Logger.error(Util.whichFn() + "Already in queue (%s)", eventInfo);
            return duplicate;
        }

        EventToCloud event = new EventToCloud(eventInfo, jsonData, binaryData);
        return event.save();
    }

    public static EventToCloud getOldest()
    {
        return EventToCloud.q().order("_created").first();
    }

    public static ServerPagedResult<EventToCloud> query(int skip, int take)
    {
        MorphiaQuery query = EventToCloud.q().order("_created");
        return QueryHelper.preparePagedResult(query, skip, take);
    }

    private EventToCloud(EventInfo eventInfo, String jsonData, byte[] binaryData)
    {
        this.eventInfo = eventInfo;
        this.jsonData = jsonData;
        this.binaryData = binaryData;
    }

    public EventInfo getEventInfo()
    {
        return eventInfo;
    }

    public String getJsonData()
    {
        return jsonData;
    }

    public byte[] getBinaryData()
    {
        return binaryData;
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", eventInfo.getType().toString(), eventInfo.getEventId());
    }

}
