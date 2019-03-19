package platform.events;

import com.google.gson.Gson;
import com.kaisquare.events.thrift.EventDetails;
import com.kaisquare.kaisync.platform.MessagePacket;
import com.kaisquare.kaisync.platform.MessagePacket.PacketDataHelper;
import lib.util.Util;
import models.Analytics.VcaError;
import models.UnprocessedVcaEvent;
import models.archived.ArchivedEvent;
import models.events.EventToCloud;
import models.events.EventWithBinary;
import models.events.RejectedEvent;
import models.labels.LabelStore;
import models.stats.VcaHourlyStats;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import platform.Environment;
import platform.content.FileFormat;
import platform.data.collective.DataCollectorFactory;
import platform.data.collective.LabelDataCollector;
import platform.data.collective.OccupancyDataCollector;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedEventVideo;
import platform.db.gridfs.GridFsDetails;
import platform.devices.DeviceChannelPair;
import platform.label.LabelManager;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventSubscriber;
import platform.pubsub.PlatformEventTask;
import platform.pubsub.PlatformEventType;
import play.Logger;

import java.io.File;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 */
public enum EventManager implements PlatformEventSubscriber
{
    INSTANCE;

    public static final String EVENT_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
    public static final int EVENT_VIDEO_SECONDS = 10;
    public static final FileFormat EVENT_VIDEO_FORMAT = FileFormat.MP4;
    public static final String IMAGE_FORMAT = "jpg";

    public static EventManager getInstance()
    {
        return INSTANCE;
    }

    public static Date parseEventTime(String timestamp)
    {
        if (Util.isNullOrEmpty(timestamp))
        {
            return new Date(0);
        }

        DateTime dt = DateTime.parse(timestamp, DateTimeFormat.forPattern(EVENT_TIME_FORMAT).withZoneUTC());
        DateTime dtUtc = new DateTime(dt.getMillis(), DateTimeZone.UTC);
        return dtUtc.toDate();
    }

    public static String getOriginalTime(long millis)
    {
        return new DateTime(millis, DateTimeZone.UTC).toString(EventManager.EVENT_TIME_FORMAT);
    }

    public void subscribePlatformEvents()
    {
        PlatformEventMonitor evtMon = PlatformEventMonitor.getInstance();

        /**
         * VCA_REMOVED
         *
         */
        evtMon.subscribe(PlatformEventType.VCA_REMOVED, new PlatformEventTask()
        {
            @Override
            public void run(Object... params) throws Exception
            {
                String vcaInstanceId = (String) params[0];
                if (Util.isNullOrEmpty(vcaInstanceId))
                {
                    throw new IllegalArgumentException();
                }

                VcaError.q().filter("instanceId", vcaInstanceId).delete();
            }
        });
    }

    public MessagePacket convertToMessagePacket(EventDetails event)
    {
        MessagePacket packet = new MessagePacket();
        packet.put("id", event.getId());
        packet.put("data", event.getData());
        packet.put("type", event.getType());
        packet.put("time", event.getTime());
        packet.put("deviceid", event.getDeviceId());
        packet.put("channelid", event.getChannelId());
        if (event.getBinaryData() != null)
        {
            packet.putBytes("binary", event.getBinaryData());
        }

        return packet;
    }

    public EventDetails convertToEventDetails(MessagePacket packet)
    {
        Map<String, PacketDataHelper> map = packet.toMap();

        EventDetails ed = new EventDetails();
        ed.setId(map.get("id").readAsString());
        ed.setData(map.get("data").readAsString());
        ed.setType(map.get("type").readAsString());
        ed.setTime(map.get("time").readAsString());
        ed.setDeviceId(map.get("deviceid").readAsString());
        ed.setChannelId(map.get("channelid").readAsString());
        byte[] binary = map.get("binary").getRaw();
        if (binary != null)
        {
            ed.setBinaryData(binary);
        }

        return ed;
    }

    public void processVcaError(EventInfo eventInfo, String jsonData)
    {
        Map<String, String> errorInfo = new LinkedHashMap<>();
        try
        {
            errorInfo = new Gson().fromJson(jsonData, errorInfo.getClass());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return;
        }

        String instanceId = errorInfo.get("instanceId");
        VcaError error = new VcaError(
                instanceId,
                errorInfo.get("source"),
                errorInfo.get("error"),
                eventInfo.getTime()
        );

        error.save();

        //stats
        VcaHourlyStats.errorEventReceived(instanceId);
    }

    /**
     * @param eventId
     *
     * @return null if there is no video
     */
    public GridFsDetails getEventVideo(String eventId)
    {
        CachedEventVideo eventVideo = CacheClient.getInstance().getEventVideo(eventId);
        if (eventVideo == null)
        {
            return null;
        }
        return eventVideo.getVideoDetails();
    }

    public String getEventVideoUrl(String eventId)
    {
        String urlFormat = "/geteventvideo/%s.%s";

        GridFsDetails fsDetails = getEventVideo(eventId);
        if (fsDetails == null)
        {
            return "";
        }

        return String.format(urlFormat, eventId, EVENT_VIDEO_FORMAT.getExtension());
    }
    
    public String getEventImageUrl(long evtTime) {
    	String result = String.format("%s/%s.%s",getImagesPath(),evtTime,IMAGE_FORMAT);
    	File file = new File(result);
    	if(!file.exists()) {
    		result = "";
    	}
    	return result;
    }

    public String extractEventIdFrom(String filename)
    {
        int endIndex = filename.toLowerCase().indexOf("." + EVENT_VIDEO_FORMAT.getExtension());
        return filename.substring(0, endIndex);
    }

    public void reject(EventInfo eventInfo, String jsonData, String reason)
    {
        RejectedEvent.createNew(eventInfo, jsonData, reason);
    }

    public void queueForReportProcessing(EventInfo eventInfo, String jsonData, byte[] binaryData)
    {
        if (!eventInfo.getType().isUsedForReporting())
        {
            return;
        }

        //save binary
        if (eventInfo.getType().binaryAllowed())
        {
            EventWithBinary.createNew(eventInfo, jsonData, binaryData);
        }

        //archive
        ArchivedEvent event = ArchivedEvent.createNew(eventInfo, jsonData);

        //hourly report compilers
        UnprocessedVcaEvent.copyFrom(event).save();
    }

    public void queueForCloud(EventInfo eventInfo, String jsonData, byte[] binaryData)
    {
        EventToCloud.createNew(eventInfo, jsonData, binaryData);
    }

    public void occupancyChanged(DeviceChannelPair camera, int newOccupancy)
    {
        try
        {
            LabelStore storeLabel = LabelManager.getInstance().getAssignedStoreLabel(camera);
            if (storeLabel == null)
            {
                return;
            }

            LabelDataCollector collector = DataCollectorFactory.getInstance()
                    .getCollector(OccupancyDataCollector.class, storeLabel.getLabelId());
            synchronized (this)
            {
                collector.collect(camera, newOccupancy);
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }
    
    
    private static String getImagesPath(){
        String result = null;
        File file = Paths.get("").toAbsolutePath().toFile().getParentFile();
        result = Util.combine(file.getAbsolutePath(),"objImages");
        File imageDirectory = new File(result);
        return result;
    }
    
}

