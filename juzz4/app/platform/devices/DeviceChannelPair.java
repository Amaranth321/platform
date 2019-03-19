package platform.devices;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import lib.util.Util;
import models.MongoDevice;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.lang.reflect.Type;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class DeviceChannelPair
{
    private final String coreDeviceId;
    private final String channelId;

    public DeviceChannelPair(String coreDeviceId, String channelId)
    {
        if (Util.isNullOrEmpty(coreDeviceId))
        {
            throw new NullPointerException();
        }
        this.coreDeviceId = coreDeviceId;
        this.channelId = channelId == null ? "" : channelId;
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", coreDeviceId, channelId);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof DeviceChannelPair)
        {
            DeviceChannelPair other = (DeviceChannelPair) o;
            boolean dvcOk = this.getCoreDeviceId().equals(other.getCoreDeviceId());
            boolean chnOk = this.getChannelId().equals(other.getChannelId());
            return dvcOk && chnOk;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
                .append(coreDeviceId)
                .append(channelId)
                .toHashCode();
    }

    public String getCoreDeviceId()
    {
        return coreDeviceId;
    }

    public String getChannelId()
    {
        return channelId;
    }

    public MongoDevice getDbDevice()
    {
        return MongoDevice.getByCoreId(coreDeviceId);
    }

    public DeviceChannelPair adjustIdPairForCloud(String nodeCoreDeviceId)
    {
        // Node's coreDeviceId becomes coreDeviceId
        // Node camera's coreDeviceId becomes channelId
        return new DeviceChannelPair(nodeCoreDeviceId, coreDeviceId);
    }

    public DeviceChannelPair adjustIdPairForNode()
    {
        // channelId on cloud becomes coreDeviceId
        // all are single-channel cameras
        return new DeviceChannelPair(channelId, "0");
    }

    public static class Deserializer implements JsonDeserializer<DeviceChannelPair>
    {
        @Override
        public DeviceChannelPair deserialize(JsonElement jsonElement,
                                             Type type,
                                             JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException
        {
            if (jsonElement.isJsonObject())
            {
                return new DeviceChannelPair(jsonElement.getAsJsonObject().get("coreDeviceId").getAsString(),
                                             jsonElement.getAsJsonObject().get("channelId").getAsString());
            }
            else
            {
                //if this class is used as a key of some map, toString method will be called during the serialization.
                //hence this code below is necessary sometimes
                //DO NOT add a serializer. That will break other classes.
                String[] split = jsonElement.getAsString().split(":");
                return new DeviceChannelPair(split[0], split[1]);
            }
        }
    }
}
