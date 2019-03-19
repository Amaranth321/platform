package models.archived;

import com.google.code.morphia.annotations.Entity;
import platform.db.QueryHelper;
import platform.events.EventInfo;
import play.modules.morphia.Model;

/**
 * Events that are used for reporting will be archived later.
 * Refer to {@link platform.events.EventType#isUsedForReporting()}
 * <p/>
 * DO NOT index or query this collection.
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class ArchivedEvent extends Model
{
    private EventInfo eventInfo;
    private String jsonData;

    public static ArchivedEvent find(String eventId)
    {
        return ArchivedEvent.q().filter("eventInfo.eventId", eventId).first();
    }

    public static void removeEntriesOlderThan(int days)
    {
        String timeField = "eventInfo.time";
        QueryHelper.removeOlderThan(days, q(), timeField);
    }

    public static ArchivedEvent createNew(EventInfo eventInfo, String jsonData)
    {
        if (!eventInfo.getType().isUsedForReporting())
        {
            return null;
        }

        ArchivedEvent newOne = new ArchivedEvent(eventInfo, jsonData);
        return newOne.save();
    }

    protected ArchivedEvent()
    {
    }

    protected ArchivedEvent(EventInfo eventInfo, String jsonData)
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
}
