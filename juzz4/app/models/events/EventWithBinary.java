package models.events;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import lib.util.Util;
import platform.content.FileFormat;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsFileGroup;
import platform.db.gridfs.GridFsHelper;
import platform.db.QueryHelper;
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
public class EventWithBinary extends Model
{
    private final EventInfo eventInfo;
    private final String jsonData;
    private final GridFsDetails fileDetails;

    public static void createNew(EventInfo eventInfo, String jsonData, byte[] binaryData)
    {
        if (binaryData == null || binaryData.length == 0)
        {
            throw new IllegalArgumentException();
        }

        GridFsDetails gridFsDetails = GridFsHelper.saveBinaryFile(
                eventInfo.getEventId(),
                binaryData,
                FileFormat.UNKNOWN,
                GridFsFileGroup.EVENT_BINARY_DATA);

        if (gridFsDetails == null)
        {
            Logger.error(Util.whichFn() + "failed to save event binary (%s)", eventInfo);
            return;
        }

        EventWithBinary dbData = new EventWithBinary(eventInfo, jsonData, gridFsDetails);
        dbData.save();
    }

    public static EventWithBinary find(String eventId)
    {
        return EventWithBinary.q()
                .filter("eventInfo.eventId", eventId)
                .first();
    }

    public static void removeEntriesOlderThan(int days)
    {
        String timeField = "eventInfo.time";

        //must loop in order to call overridden EventBinaryData.delete() function
        Iterable<EventWithBinary> binaryFiles = QueryHelper.getEntriesOlderThan(days, EventWithBinary.q(), timeField).fetch();
        for (EventWithBinary binaryFile : binaryFiles)
        {
            binaryFile.delete();
        }
    }

    private EventWithBinary(EventInfo eventInfo, String jsonData, GridFsDetails fileDetails)
    {
        this.eventInfo = eventInfo;
        this.jsonData = jsonData;
        this.fileDetails = fileDetails;
    }

    public EventInfo getEventInfo()
    {
        return eventInfo;
    }

    public GridFsDetails getFileDetails()
    {
        return fileDetails;
    }

    public String getJsonData()
    {
        return jsonData;
    }

    @Override
    public EventWithBinary delete()
    {
        boolean result = GridFsHelper.removeFile(fileDetails);
        if (!result)
        {
            Logger.error(Util.whichFn() + "failed: %s", fileDetails.getFilename());
        }
        else
        {
            super.delete();
        }
        return this;
    }

}
