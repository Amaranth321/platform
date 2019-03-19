package platform.devices;

import lib.util.Util;
import models.MongoDeviceModel;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class DeviceModelInfo
{
    private final long modelId;
    private final String name;
    private final int channels;
    private final String capabilities;

    public DeviceModelInfo(String modelId)
    {
        MongoDeviceModel dbModel = MongoDeviceModel.getByModelId(modelId);
        this.modelId = Long.parseLong(dbModel.getModelId());
        this.name = dbModel.getName();
        this.channels = dbModel.getChannels();
        this.capabilities = dbModel.getCapabilities();
    }

    public DeviceModelInfo(MongoDeviceModel dbModel)
    {
        this.modelId = Long.parseLong(dbModel.getModelId());
        this.name = dbModel.getName();
        this.channels = dbModel.getChannels();
        this.capabilities = dbModel.getCapabilities();
    }

    public long getModelId()
    {
        return modelId;
    }

    public String getName()
    {
        return name;
    }

    public int getChannels()
    {
        return channels;
    }

    public boolean isKainode()
    {
        return !Util.isNullOrEmpty(capabilities) && capabilities.contains(MongoDeviceModel.CAP_NODE);
    }

    public boolean isNodeOne()
    {
        return channels == 1;
    }
}
