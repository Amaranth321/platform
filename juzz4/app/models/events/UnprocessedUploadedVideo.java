package models.events;

import com.google.code.morphia.annotations.Entity;
import com.google.gson.Gson;
import com.kaisquare.transports.event.VideoOwnerEventData;
import lib.util.JsonReader;
import lib.util.exceptions.InvalidJsonException;
import platform.Environment;
import platform.devices.DeviceChannelPair;
import platform.events.EventInfo;
import platform.events.EventType;
import play.Logger;
import play.modules.morphia.Model;

import java.util.Map;

/**
 * List of {@link platform.events.EventType#NODE_EVENT_VIDEO_UPLOADED} events. Hence, this only exists on cloud.
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class UnprocessedUploadedVideo extends Model
{
    private final String fileName;  //filename in kaisync file server
    private final EventInfo ownerEventInfo;    // event that the uploaded video is for
    private int attempts;

    public static void queue(String jsonData)
    {
        /**
         *
         * Use eventInfo to get json data only, which holds the info of the event that the video belongs to
         *
         */
        VideoOwnerEventData uploadedVideoData = parseJsonData(jsonData);
        if (uploadedVideoData == null)
        {
            Logger.error("invalid json data");
            return;
        }

        EventType ownerEventType = EventType.parse(uploadedVideoData.eventType);
        if (ownerEventType == EventType.UNKNOWN)
        {
            Logger.error("invalid eventType");
            return;
        }

        //re-build original event's info
        EventInfo ownerEventInfo = new EventInfo(
                uploadedVideoData.eventId,
                ownerEventType,
                new DeviceChannelPair(uploadedVideoData.nodeCoreDeviceId, uploadedVideoData.cameraCoreDeviceId),
                Environment.getInstance().getCurrentUTCTimeMillis()
        );

        //queue
        new UnprocessedUploadedVideo(uploadedVideoData.filename, ownerEventInfo).save();
    }

    public static VideoOwnerEventData parseJsonData(String jsonData)
    {
        try
        {
            Map dataMap = new Gson().fromJson(jsonData, Map.class);
            JsonReader reader = new JsonReader();
            reader.load(dataMap);

            String eventId = reader.getAsString("eventId", null);
            if (eventId == null)
            {
                throw new InvalidJsonException("No eventId");
            }

            String filename = reader.getAsString("filename", null);
            if (filename == null)
            {
                throw new InvalidJsonException("No filename");
            }

            String eventType = reader.getAsString("eventType", null);
            if (eventType == null)
            {
                throw new InvalidJsonException("invalid eventType");
            }

            String nodeCoreDeviceId = reader.getAsString("nodeCoreDeviceId", null);
            if (nodeCoreDeviceId == null)
            {
                throw new InvalidJsonException("No nodeCoreDeviceId");
            }

            String cameraCoreDeviceId = reader.getAsString("cameraCoreDeviceId", null);
            if (cameraCoreDeviceId == null)
            {
                throw new InvalidJsonException("No cameraCoreDeviceId");
            }

            return new VideoOwnerEventData(eventId, filename, eventType, nodeCoreDeviceId, cameraCoreDeviceId);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    private UnprocessedUploadedVideo(String fileName, EventInfo ownerEventInfo)
    {
        this.fileName = fileName;
        this.ownerEventInfo = ownerEventInfo;
    }

    public String getFileName()
    {
        return fileName;
    }

    public EventInfo getEventInfo()
    {
        return ownerEventInfo;
    }

    public void incrementAttempt()
    {
        ++attempts;
        save();
    }
}
