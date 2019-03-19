package models.backwardcompatibility;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.utils.IndexDirection;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class has been deprecated. DO NOT USE it in any new codes.
 * <p/>
 * Refer to {@link platform.events.EventInfo}
 */
@Entity
@Deprecated
public class Event extends Model
{
    //instance members
    public String eventId;
    public String deviceId;
    public String channelId;
    public String data;
    public String type;
    public String time;
    @Indexed(value = IndexDirection.DESC)
    public Date timeobject;
    public String blobId;
    public int messageCount;
    public String patchEventVideoURL;
    public String deviceName;
    @Indexed(value = IndexDirection.GEO2D)
    public double[] loc;
    @Embedded
    public List<Alert> alerts;
    public String nodeEventId; //node-platform generated ID, for use in cloud
    @Indexed(value = IndexDirection.DESC)
    public Date lastMessageTimestamp;
    public String eventVideoThumbnailUrl;
    public String lastMessage;
    public String lastMessageSenderName;

    public Event()
    {
        eventId = "";
        deviceId = "";
        channelId = "";
        data = "";
        type = "";
        time = "";
        blobId = "";
        messageCount = 0;
        patchEventVideoURL = "";
        deviceName = "";
        loc = new double[2];
        nodeEventId = "";
        alerts = new ArrayList<>();
        eventVideoThumbnailUrl = "";
        lastMessage = "";
    }

}