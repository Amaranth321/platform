package models.events;

import com.google.code.morphia.annotations.Entity;
import platform.events.EventInfo;
import play.modules.morphia.Model;

/**
 * In the previous design, video ready event includes eventId and cameraCoreDeviceId only
 * So, for old nodes sending those events,
 * node's id and original event type must be retrieved from {@link models.archived.ArchivedEvent}.
 * These videos will be verified and saved as {@link models.events.EventVideo} by another job
 * <p/>
 * Consider removing this in the future.
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class OldEventVideo extends Model
{
    private final String eventId;
    private final String cameraCoreDeviceId;
    private final String videoFilename;
    private int attempts;

    public static MorphiaQuery getQuery()
    {
        return OldEventVideo.q().filter("attempts <", 10).order("attempts, _created");
    }

    public OldEventVideo(EventInfo eventInfo)
    {
        this.eventId = eventInfo.getEventId();
        this.cameraCoreDeviceId = eventInfo.getCamera().getCoreDeviceId();
        videoFilename = String.format("%s_%s.mp4", cameraCoreDeviceId, eventId);
    }

    public String getEventId()
    {
        return eventId;
    }

    public String getCameraCoreDeviceId()
    {
        return cameraCoreDeviceId;
    }

    public String getVideoFilename()
    {
        return videoFilename;
    }

    public void incrementAttempts()
    {
        attempts++;
        this.save();
    }
}
