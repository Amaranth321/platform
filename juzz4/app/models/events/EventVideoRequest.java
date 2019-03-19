package models.events;

import com.google.code.morphia.annotations.Entity;
import models.abstracts.ServerPagedResult;
import platform.db.QueryHelper;
import platform.events.EventInfo;
import play.modules.morphia.Model;

/**
 * request sent to core for recording.
 * <p/>
 * This request can fail in two ways
 * 1. the call to Core Engine returned false (unknown reason)
 * 2. Platform is restarting when the video ready reply from Core was sent and missed (unlikely, but it can happen).
 *
 * @author Aye Maung
 * @see jobs.node.events.EventVideoReRequestJob
 * @since v4.4
 */
@Entity
public class EventVideoRequest extends Model
{
    private final EventInfo ownerEventInfo;
    private final String requestData;
    private int failCount;

    public static EventVideoRequest createNew(EventInfo ownerEventInfo, String requestData)
    {
        EventVideoRequest request = new EventVideoRequest(ownerEventInfo, requestData);
        return request.save();
    }

    public static ServerPagedResult<EventVideoRequest> query(int skip, int take)
    {
        MorphiaQuery query = EventVideoRequest.q().order("-_created");
        return QueryHelper.preparePagedResult(query, skip, take);
    }

    public static EventVideoRequest find(String eventId)
    {
        return EventVideoRequest.q().filter("ownerEventInfo.eventId", eventId).first();
    }

    private EventVideoRequest(EventInfo ownerEventInfo, String requestData)
    {
        this.ownerEventInfo = ownerEventInfo;
        this.requestData = requestData;
        this.failCount = 0;
    }

    public EventInfo getOwnerEventInfo()
    {
        return ownerEventInfo;
    }

    public String getRequestData()
    {
        return requestData;
    }

    public int getFailCount()
    {
        return failCount;
    }

    public void callFailed()
    {
        failCount++;
        this.save();
    }
}
