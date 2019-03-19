package platform.events;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import lib.util.Util;
import platform.analytics.VcaType;
import play.Logger;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * Use {@link #toString()} to get the event name for outside communications (API, thrift, etc ...)
 *
 * @author Aye Maung
 * @since v4.4
 */
public enum EventType
{
    UNKNOWN("event-unknown"),

    //VCA
    VCA_INTRUSION("event-vca-intrusion", EventOrigin.VCA),
    VCA_PERIMETER_DEFENSE("event-vca-perimeter", EventOrigin.VCA),
    VCA_LOITERING("event-vca-loitering", EventOrigin.VCA),
    VCA_OBJECT_COUNTING("event-vca-object-counting", EventOrigin.VCA),
    VCA_VIDEO_BLUR("event-vca-video-blur", EventOrigin.VCA),
    VCA_FACE_INDEXING("event-vca-face", EventOrigin.VCA),
    VCA_TRAFFIC_FLOW("event-vca-traffic", EventOrigin.VCA),
    VCA_PEOPLE_COUNTING("event-vca-people-counting", EventOrigin.VCA),
    VCA_CROWD_DETECTION("event-vca-crowd", EventOrigin.VCA),
    VCA_PROFILING("event-vca-audienceprofiling", EventOrigin.VCA),
    VCA_PASSERBY("event-vca-passerby", EventOrigin.VCA),
    //add by renzongke 
    VCA_OBJECT_DETECTION("event-vca-object-detection",EventOrigin.VCA),
    //RealTime feeds
    VCA_FEED_PROFILING("event-feed-profiling", EventOrigin.VCA),
    VCA_STATUS_CHANGED("event-vca-status-changed", EventOrigin.VCA),

    //Errors
    ERROR_VCA("event-vca-internal-error", EventOrigin.VCA),

    //Camera sensor events
    SENSOR_PIR("event-passive-infrared"),
    SENSOR_AUDIO("event-audio"),

    //Core engine events
    CORE_EVENT_VIDEO_READY("event-recording", EventOrigin.CORE_ENGINE),
    CORE_DEVICE_CONNECTED("event-connected", EventOrigin.CORE_ENGINE),
    CORE_DEVICE_CONNECTION_LOST("event-connection-lost", EventOrigin.CORE_ENGINE),
    CORE_DEVICE_CONNECTION_POOR("event-connection-poor", EventOrigin.CORE_ENGINE),
    CORE_DEVICE_CONNECTION_FAILED("event-connection-failed", EventOrigin.CORE_ENGINE),
    CORE_UNRECOGNIZED_NODE("event-node-unregistered", EventOrigin.CORE_ENGINE),
    CORE_NODE_REGISTERED("event-node-registered", EventOrigin.CORE_ENGINE),
    CORE_NODE_UPSTREAM_FAILED("event-upstream-failed", EventOrigin.CORE_ENGINE),
    CORE_RECORDING_STARTED("event-storage-started", EventOrigin.CORE_ENGINE),
    CORE_RECORDING_STOPPED("event-storage-stopped", EventOrigin.CORE_ENGINE),
    CORE_RECORDING_DISK_FULL("event-disk-full", EventOrigin.CORE_ENGINE),

    //Node
    //outgoing event to node-core to record event video
    CAPTURE_EVENT_VIDEO("CAPTURE_EVENT_VIDEO", EventOrigin.NODE_PLATFORM),
    NODE_EVENT_VIDEO_UPLOADED("event-video-uploaded", EventOrigin.NODE_PLATFORM),

    //Label related events
    OCCUPANCY_LIMIT("event-occupancy-limit", EventOrigin.CLOUD_PLATFORM),

    /**
     * End of Types
     */
    ;

    /**
     * @param typeString either enum name or the type name
     */
    public static EventType parse(String typeString)
    {
        if (!Util.isNullOrEmpty(typeString))
        {
            for (EventType type : EventType.values())
            {
                if (typeString.equals(type.toString()) || typeString.equals(type.name()))
                {
                    return type;
                }
            }
        }

        Logger.error(Util.getCallerFn() + "unknown type: %s", typeString);
        return UNKNOWN;
    }

    public static class Deserializer implements JsonDeserializer<EventType>
    {
        @Override
        public EventType deserialize(JsonElement jsonElement,
                                     Type type,
                                     JsonDeserializationContext jsonDeserializationContext) throws JsonParseException
        {
            return EventType.parse(jsonElement.getAsString());
        }
    }

    @Override
    public String toString()
    {
        return stringValue;
    }

    public boolean isVcaEvent()
    {
        return isSecurityVcaEvent() || isBIVcaEvent();
    }

    public boolean isSecurityVcaEvent()
    {
        try
        {
            VcaType vcaType = VcaType.of(this);
            return vcaType.isSecurity();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public boolean isBIVcaEvent()
    {
        try
        {
            VcaType vcaType = VcaType.of(this);
            return vcaType.isBI();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public EventOrigin getOrigin()
    {
        return origin;
    }

    public boolean isUsedForReporting()
    {
        return isSecurityVcaEvent() || isBIVcaEvent();
    }

    public boolean binaryAllowed()
    {
        List<EventType> types = Arrays.asList(
                VCA_FACE_INDEXING
        );

        return types.contains(this);
    }

    public boolean in(EventType... types)
    {
        for (EventType type : types)
        {
            if (this.equals(type))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Private fields
     */

    private final String stringValue;
    private final EventOrigin origin;

    private EventType(String stringValue)
    {
        this.stringValue = stringValue;
        this.origin = EventOrigin.UNKNOWN;
    }

    private EventType(String stringValue, EventOrigin origin)
    {
        this.stringValue = stringValue;
        this.origin = origin;
    }

}
