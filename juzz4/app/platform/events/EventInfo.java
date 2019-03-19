package platform.events;

import com.google.code.morphia.annotations.Embedded;
import com.kaisquare.events.thrift.EventDetails;
import lib.util.Util;
import models.backwardcompatibility.Event;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.Environment;
import platform.devices.DeviceChannelPair;

import java.util.UUID;

/**
 * DO NOT modify the fields of this class. A lot of db models use this embedded class.
 * Keep it immutable.
 * <p/>
 * The event data, if any, will be accessible using the methods below:
 * For jsonData, refer to {@link models.archived.ArchivedEvent}.
 * For binary, refer to {@link models.events.EventWithBinary}.
 *
 * @author Aye Maung
 * @since v4.4
 */
@Embedded
public class EventInfo
{
    private final long time;
    private final String eventId;
    private final EventType type;
    private final DeviceChannelPair camera;

    public static EventInfo fromThriftEvent(EventDetails eventDetails)
    {
        //get millis
        long evtTime = EventManager.parseEventTime(eventDetails.getTime()).getTime();

        //generate eventId if empty
        //Or on KAI Node, eventId will be regenerated regardless, to ensure uniqueness
        String evtId = eventDetails.getId();
        if (Util.isNullOrEmpty(evtId) || evtId.equals("0") || Environment.getInstance().onKaiNode())
        {
            evtId = UUID.randomUUID().toString();
        }

        return new EventInfo(
                evtId,
                EventType.parse(eventDetails.getType()),
                new DeviceChannelPair(eventDetails.getDeviceId(), eventDetails.getChannelId()),
                evtTime
        );
    }

    public EventInfo(String eventId,
                     EventType type,
                     DeviceChannelPair camera,
                     long time)
    {
        this.eventId = eventId;
        this.type = type;
        this.camera = camera;
        this.time = time;
    }

    public String getEventId()
    {
        return eventId;
    }

    public EventType getType()
    {
        return type;
    }

    public DeviceChannelPair getCamera()
    {
        return camera;
    }

    public long getTime()
    {
        return time;
    }

    public EventDetails toThriftEvent(String jsonData, byte[] binaryData)
    {
        EventDetails evt = new EventDetails();
        evt.setTime(new DateTime(time, DateTimeZone.UTC).toString(EventManager.EVENT_TIME_FORMAT));
        evt.setId(eventId);
        evt.setType(type.toString());
        evt.setDeviceId(camera.getCoreDeviceId());
        evt.setChannelId(camera.getChannelId());
        evt.setData(jsonData);
        evt.setBinaryData(binaryData);
        return evt;
    }

    public Event toLegacyEvent(String jsonData)
    {
        Event evt = new Event();
        evt.eventId = eventId;
        evt.deviceId = camera.getCoreDeviceId();
        evt.channelId = camera.getChannelId();
        evt.type = type.toString();
        evt.time = EventManager.getOriginalTime(time);
        evt.nodeEventId = eventId;
        evt.data = jsonData;
        return evt;
    }

    @Override
    public String toString()
    {
        return String.format("(%s) %s", type, eventId);
    }

}
