package models.backwardcompatibility;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.gson.Gson;
import play.modules.morphia.Model;

import java.util.*;

/**
 * Alert.
 *
 * @author Nischal
 */
@Entity
@Indexes({
        @Index("eventId"),
        @Index("eventType, -time")
})
@Deprecated
public class Alert extends Model {
    public String alertName;
    public List<String> alertlabels;
    public String sendVia;
    public String deviceId;
    public String channelId;
    public Date time;
    public String eventType;
    public String eventId;
    public Long userId;
    public String notificationLevel; // eg. low, critical, moderate
    public String eventVideoUrl;
    public String eventVideoThumbnailUrl;

    public Alert() {
        alertName = "";
        alertlabels = new ArrayList<>();
        sendVia = "";
        deviceId = "";
        channelId = "";
        time = new Date();
        eventType = "";
        eventId = "";
        userId = 0L;
        notificationLevel = "";
        eventVideoUrl = "";
        eventVideoThumbnailUrl = "";
    }

    @Override
    public String toString() {
        Map map = new LinkedHashMap();
        map.put("deviceId", deviceId);
        map.put("channelId", channelId);
        map.put("eventId", eventId);
        map.put("userId", userId);
        map.put("videoUrl", eventVideoUrl);
        return new Gson().toJson(map);
    }
}

