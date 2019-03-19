package models;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;

import models.backwardcompatibility.UnprocessedEvent;
import platform.events.EventManager;
import platform.devices.DeviceChannelPair;
import platform.events.EventInfo;
import platform.events.EventType;
import models.archived.ArchivedEvent;

@Entity
@Indexes({
	@Index("eventInfo.type, eventInfo.time"),
	@Index("eventInfo.time")
})
public class UnprocessedVcaEvent extends ArchivedEvent {
	
	protected UnprocessedVcaEvent(EventInfo eventInfo, String jsonData)
	{
		super(eventInfo, jsonData);
	}
	
	public static UnprocessedVcaEvent copyFrom(ArchivedEvent e)
	{
		return new UnprocessedVcaEvent(e.getEventInfo(), e.getJsonData());
	}

	public static UnprocessedVcaEvent copyFrom(@SuppressWarnings("deprecation") UnprocessedEvent e) {
		EventInfo eventInfo = new EventInfo(
                e.eventId,
                EventType.parse(e.type),
                new DeviceChannelPair(e.deviceId, e.channelId),
                EventManager.parseEventTime(e.time).getTime()
        );
		return new UnprocessedVcaEvent(eventInfo, e.data);
	}

}
