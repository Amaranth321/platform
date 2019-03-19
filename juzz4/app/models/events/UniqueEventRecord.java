package models.events;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import platform.events.EventInfo;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 */
@Entity
@Indexes({
        @Index(unique = true, value = "eventId, coreDeviceId")
})
public class UniqueEventRecord extends Model
{    private final String eventId;
    private final String coreDeviceId;

    public static boolean duplicateExists(EventInfo eventInfo)
    {
        try
        {
            new UniqueEventRecord(eventInfo).save();
            return false;
        }
        catch (Exception e)
        {
            return true;
        }
    }

    private UniqueEventRecord(EventInfo eventInfo)
    {
        this.eventId = eventInfo.getEventId();
        this.coreDeviceId = eventInfo.getCamera().getCoreDeviceId();
    }
}
