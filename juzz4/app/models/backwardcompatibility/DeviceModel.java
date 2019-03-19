package models.backwardcompatibility;

import lib.util.Util;
import models.MongoDeviceModel;
import platform.devices.NodeEnv;
import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

/**
 * @author kdp
 */
@Entity
@Table(name = "`device_models`")
@Deprecated
public class DeviceModel extends Model
{
    public static final String CAP_GPS = "gps";
    public static final String CAP_GSENSOR = "gsensor";
    public static final String CAP_VIDEO = "video";
    public static final String CAP_VIDEO_MJPEG = "mjpeg";
    public static final String CAP_VIDEO_H264 = "h264";
    public static final String CAP_AUDIO = "audio";
    public static final String CAP_INDOOR_LOCATION = "indoor_location";
    public static final String CAP_GPIO = "gpio";
    public static final String CAP_NODE = "node";


    public static List<DeviceModel> getNodeModels(NodeEnv nodeEnv)
    {
        String queryKeyword = "%" + CAP_NODE + "%";
        switch (nodeEnv)
        {
            case UBUNTU:
                return DeviceModel.find("capabilities like ? AND channels > ?", queryKeyword, 1).fetch();

            case EMBEDDED:
                return DeviceModel.find("capabilities like ? AND channels = ?", queryKeyword, 1).fetch();

            default:
                throw new UnsupportedOperationException();
        }
    }

    public Long modelId;

    public String name;

    public Integer channels;

    public String capabilities; // e.g. "gps, video" or "video, audio"

    @Column(columnDefinition = "TEXT")
    public String liveview;

    @Column(columnDefinition = "TEXT")
    public String misc;

    public DeviceModel()
    {
    }

    // for compatibility
    public DeviceModel(MongoDeviceModel mongoDeviceModel)
    {
        this.modelId = Long.parseLong(mongoDeviceModel.getModelId());
        this.name = mongoDeviceModel.getName();
        this.capabilities = mongoDeviceModel.getCapabilities();
        this.channels = mongoDeviceModel.getChannels();
        this.liveview = mongoDeviceModel.getLiveview();
        this.misc = mongoDeviceModel.getMisc();
    }

    public String toString()
    {
        return String.format("%s, %s channel(s), %s", this.name, this.channels, this.capabilities);
    }

    @Override
    public Long getId()
    {
        return modelId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof DeviceModel)
        {
            DeviceModel other = (DeviceModel) o;
            return this.modelId.equals(other.modelId);
        }
        return false;
    }

    public boolean isKaiNode()
    {
        return !Util.isNullOrEmpty(capabilities) && capabilities.contains(DeviceModel.CAP_NODE);
    }
}
