package models.transportobjects;

import models.archived.ArchivedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.events.EventManager;
import platform.events.EventInfo;

import java.util.Date;

/**
 * @author Aye Maung
 * @since v4.4
 *
 */
public class ArchivableEventTransport {
    public final String id;
    public final String eventId;
    public final String deviceId;
    public final String channelId;
    public final String data;
    public final String type;
    public final String time;
    public final Date timeobject;

    public ArchivableEventTransport(ArchivedEvent dbEvent) {
        EventInfo eventInfo = dbEvent.getEventInfo();
        id = eventInfo.getEventId();
        eventId = eventInfo.getEventId();
        deviceId = eventInfo.getCamera().getCoreDeviceId();
        channelId = eventInfo.getCamera().getChannelId();
        type = eventInfo.getType().toString();
        time = EventManager.getOriginalTime(eventInfo.getTime());
        timeobject = new DateTime(eventInfo.getTime(), DateTimeZone.UTC).toDate();

        data = dbEvent.getJsonData();
    }
}
