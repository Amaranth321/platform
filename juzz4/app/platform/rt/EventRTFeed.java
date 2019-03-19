package platform.rt;

import com.google.gson.Gson;
import platform.events.EventInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class EventRTFeed implements RTFeed
{
    private final EventInfo eventInfo;
    private final String jsonData;

    public EventRTFeed(EventInfo eventInfo, String jsonData)
    {
        this.eventInfo = eventInfo;
        this.jsonData = jsonData;
    }

    public EventInfo getEventInfo()
    {
        return eventInfo;
    }

    public String getJsonData()
    {
        return jsonData;
    }

    @Override
    public String json()
    {
        return new Gson().toJson(this);
    }

    @Override
    public Map toAPIOutput()
    {
        Map output = new LinkedHashMap();
        output.put("time", eventInfo.getTime());
        output.put("eventId", eventInfo.getEventId());
        output.put("type", eventInfo.getType().toString());
        output.put("deviceId", eventInfo.getCamera().getCoreDeviceId());
        output.put("channelId", eventInfo.getCamera().getChannelId());
        output.put("data", jsonData);

        return output;
    }
}
