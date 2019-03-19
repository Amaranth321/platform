package platform.events;

import com.google.gson.Gson;
import play.Logger;

/**
 * Data of {@link platform.events.EventType#CORE_EVENT_VIDEO_READY}
 *
 * @author Aye Maung
 * @since v4.4
 */
public class VideoReadyEventData
{
    private final long time;
    private final String eventId;
    private final String path;      //for node

    public static VideoReadyEventData parse(String jsonString)
    {
        try
        {
            return new Gson().fromJson(jsonString, VideoReadyEventData.class);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    private VideoReadyEventData(long time, String eventId, String path)
    {
        this.time = time;
        this.eventId = eventId;
        this.path = path;
    }

    public String getOwnerEventId()
    {
        return eventId;
    }

    public String getPath()
    {
        return path;
    }
}
