package models.node;

import com.google.code.morphia.annotations.Entity;
import models.backwardcompatibility.DeviceModel;
import platform.register.NodeSetupInfo;
import play.modules.morphia.Model;

/**
 * Node information.
 *
 * @author Aye Maung
 */
@Entity
public class NodeInfo extends Model
{
    private final String cloudBucketId;
    private final String cloudPlatformDeviceId;
    private final String cloudCoreDeviceId;
    private DeviceModel deviceModel;

    private NodeSettings settings;
    private long cloudLastContacted;
    private boolean suspended;

    @Deprecated
    private String name;    //don't use this field. It will not get updated when cloud changes it

    public NodeInfo(NodeSetupInfo setupInfo)
    {
        this.name = setupInfo.getNodeName();
        this.cloudBucketId = setupInfo.getNodeLicense().cloudBucketId.toString();
        this.cloudPlatformDeviceId = setupInfo.getCloudPlatformDeviceId().toString();
        this.cloudCoreDeviceId = setupInfo.getCloudCoreDeviceId();
        this.deviceModel = setupInfo.getDeviceModel();
        suspended = false;
    }

    public String getCloudBucketId()
    {
        return cloudBucketId;
    }

    public String getCloudPlatformDeviceId()
    {
        return cloudPlatformDeviceId;
    }

    public String getCloudCoreDeviceId()
    {
        return cloudCoreDeviceId;
    }

    public DeviceModel getDeviceModel()
    {
        //nodes registered before 4.4.1.8 do not keep the model of the node itself.
        //set them as an Ingrasys node 4
        if (deviceModel == null)
        {
            DeviceModel ingrasysNode = new DeviceModel();
            ingrasysNode.name = "KAI NODE";
            ingrasysNode.modelId = 115L;
            ingrasysNode.channels = 4;
            ingrasysNode.capabilities = "video mjpeg video mjpeg h264 node";
            deviceModel = ingrasysNode;
            save();
        }

        return deviceModel;
    }

    public NodeSettings getSettings()
    {
        return settings;
    }

    public void setSettings(NodeSettings settings)
    {
        this.settings = settings;
    }

    public long getCloudLastContacted()
    {
        return cloudLastContacted;
    }

    public void setCloudLastContacted(long cloudLastContacted)
    {
        this.cloudLastContacted = cloudLastContacted;
    }

    public boolean isSuspended()
    {
        return suspended;
    }

    public void setSuspended(boolean suspended)
    {
        this.suspended = suspended;
    }

}
