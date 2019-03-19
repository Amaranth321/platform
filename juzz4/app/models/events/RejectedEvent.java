package models.events;

import com.google.code.morphia.annotations.Entity;
import models.abstracts.ServerPagedResult;
import platform.db.QueryHelper;
import platform.Environment;
import platform.events.EventInfo;
import play.modules.morphia.Model;

/**
 *
 * These are the events that are rejected by the platform for the reason given
 *
 * @author Aye Maung
 * @since v4.4
 *
 */
@Entity
public class RejectedEvent extends Model {
    private final EventInfo eventInfo;
    private final String jsonData;
    private final String reason;

    public static RejectedEvent createNew(EventInfo eventInfo, String jsonData, String reason) {
        RejectedEvent evt = new RejectedEvent(eventInfo, jsonData, reason);
        return evt.save();
    }

    public static ServerPagedResult<RejectedEvent> query(int skip, int take) {
        MorphiaQuery query = RejectedEvent.q().order("eventInfo.time");
        return QueryHelper.preparePagedResult(query, skip, take);
    }

    private RejectedEvent(EventInfo eventInfo, String jsonData, String reason) {
        this.eventInfo = eventInfo;
        this.jsonData = jsonData;
        this.reason = reason;
    }

    public EventInfo getEventInfo() {
        return eventInfo;
    }

    public String getJsonData() {
        return jsonData;
    }

    public String getReason() {
        return reason;
    }

}
