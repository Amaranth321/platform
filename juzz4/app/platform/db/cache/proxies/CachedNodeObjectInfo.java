package platform.db.cache.proxies;

import models.MongoDeviceModel;
import models.node.NodeObject;
import models.node.NodeSettings;
import platform.db.cache.CachedObject;
import platform.devices.DeviceModelInfo;
import platform.nodesoftware.NodeSoftwareStatus;

import java.util.concurrent.TimeUnit;

/**
 * Cached info for {@link models.node.NodeObject}.
 *
 * @author Aye Maung
 * @since v4.4
 */
public class CachedNodeObjectInfo extends CachedObject<CachedNodeObjectInfo>
{
    private final String name;
    private final String platformDeviceId;
    private final String coreDeviceId;
    private final String nodeVersion;
    private final double releaseNumber;
    private final NodeSettings settings;
    private final NodeSoftwareStatus softwareStatus;
    private final String downloadedVersion;
    private final DeviceModelInfo modelInfo;

    public CachedNodeObjectInfo(String cacheKey, NodeObject dbObject)
    {
        super(cacheKey);

        this.name = dbObject.getName();
        this.platformDeviceId = dbObject.getNodeId();
        this.coreDeviceId = dbObject.getNodeCoreDeviceId();
        this.nodeVersion = dbObject.getNodeVersion();
        this.releaseNumber = dbObject.getReleaseNumber();
        this.settings = dbObject.getSettings();
        this.softwareStatus = dbObject.getSoftwareStatus();
        this.downloadedVersion = dbObject.getDownloadedVersion();
        MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(dbObject.getDbDevice().getModelId());
        this.modelInfo = new DeviceModelInfo(deviceModel);
    }

    @Override
    public long getTtlMillis()
    {
        return TimeUnit.SECONDS.toMillis(30);
    }

    @Override
    public CachedNodeObjectInfo getObject()
    {
        return this;
    }

    public String getName()
    {
        return name;
    }

    public String getPlatformDeviceId()
    {
        return platformDeviceId;
    }

    public String getCoreDeviceId()
    {
        return coreDeviceId;
    }

    public String getNodeVersion()
    {
        return nodeVersion;
    }

    public NodeSoftwareStatus getSoftwareStatus()
    {
        return softwareStatus;
    }

    public double getReleaseNumber()
    {
        return releaseNumber;
    }

    public NodeSettings getSettings()
    {
        return settings;
    }

    public String getDownloadedVersion()
    {
        return downloadedVersion;
    }

    public DeviceModelInfo getModelInfo()
    {
        return modelInfo;
    }

}