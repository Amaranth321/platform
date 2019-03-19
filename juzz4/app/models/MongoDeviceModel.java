package models;

import com.google.code.morphia.annotations.Entity;
import lib.util.Util;
import platform.devices.NodeEnv;
import play.modules.morphia.Model;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author tbnguyen1407
 */
@Entity(value="DeviceModel", noClassnameStored = true)
@Model.NoAutoTimestamp
public class MongoDeviceModel extends Model
{
    // region constants

    public static final String CAP_GPS = "gps";
    public static final String CAP_GSENSOR = "gsensor";
    public static final String CAP_VIDEO = "video";
    public static final String CAP_VIDEO_MJPEG = "mjpeg";
    public static final String CAP_VIDEO_H264 = "h264";
    public static final String CAP_AUDIO = "audio";
    public static final String CAP_INDOOR_LOCATION = "indoor_location";
    public static final String CAP_GPIO = "gpio";
    public static final String CAP_NODE = "node";

    // endregion

    //region fields

    private String id;
    private String name;
    private String capabilities; // e.g. "gps, video" or "video, audio"
    private Integer channels;
    private String liveview;
    private String misc;

    //endregion

    // region getters

    public String getModelId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public String getCapabilities()
    {
        return this.capabilities;
    }

    public Integer getChannels()
    {
        return this.channels;
    }

    public String getLiveview()
    {
        return this.liveview;
    }

    public String getMisc()
    {
        return this.misc;
    }

    // endregion

    // region setters

    public void setModelId(String newId)
    {
        this.id = newId;
    }

    public void setName(String newName)
    {
        this.name = newName;
    }

    public void setCapabilities(String newCapabilities)
    {
        this.capabilities = newCapabilities;
    }

    public void setChannels(Integer newChannels)
    {
        this.channels = newChannels;
    }

    public void setLiveview(String newLiveview)
    {
        this.liveview = newLiveview;
    }

    public void setMisc(String newMisc)
    {
        this.misc = newMisc;
    }

    // endregion

    public MongoDeviceModel()
    {

    }

    public static List<MongoDeviceModel> getNodeModels(NodeEnv nodeEnv)
    {
        Pattern queryPattern = Pattern.compile(CAP_NODE);

        switch (nodeEnv)
        {
            case UBUNTU:
                return MongoDeviceModel.q()
                        .filter("capabilities", queryPattern)
                        .filter("channels >", 1).asList();
            case EMBEDDED:
                return MongoDeviceModel.q()
                        .filter("capabilities", queryPattern)
                        .filter("channels ==", 1).asList();

            default:
                throw new UnsupportedOperationException();
        }
    }

    // region public methods

    public static MongoDeviceModel getByModelId(String newModelId)
    {
        return MongoDeviceModel.q().filter("id", newModelId).get();
    }

    public String toString()
    {
        return String.format("%s, %s channel(s), %s", this.name, this.channels, this.capabilities);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof MongoDeviceModel)
        {
            MongoDeviceModel other = (MongoDeviceModel) o;
            return this.id.equals(other.id);
        }
        return false;
    }

    public boolean isKaiNode()
    {
        return !Util.isNullOrEmpty(capabilities) && capabilities.contains(MongoDeviceModel.CAP_NODE);
    }

    // endregion
}
