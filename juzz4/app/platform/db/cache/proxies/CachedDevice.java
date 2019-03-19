package platform.db.cache.proxies;

import models.MongoDevice;
import models.MongoDeviceModel;
import platform.db.cache.CachedObject;
import platform.devices.DeviceStatus;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CachedDevice extends CachedObject<CachedDevice>
{
    private final String bucketId;
    private final String platformDeviceId;
    private final String coreDeviceId;
    private final String name;
    private final String modelId;
    private final String deviceKey;
    private final String host;
    private final String port;
    private final String address;
    private final double latitude;
    private final double longitude;
    private final DeviceStatus status;

    private final Set<String> userIdSet;

    public CachedDevice(String identifier, MongoDevice dbDevice)
    {
        super(identifier);

        bucketId = dbDevice.getBucketId();
        platformDeviceId = dbDevice.getDeviceId();
        coreDeviceId = dbDevice.getCoreDeviceId();
        name = dbDevice.getName();
        modelId = dbDevice.getModelId();
        deviceKey = dbDevice.getDeviceKey();
        host = dbDevice.getHost();
        port = dbDevice.getPort();
        address = dbDevice.getAddress();
        latitude = dbDevice.getLatitude();
        longitude = dbDevice.getLongitude();
        status = dbDevice.getStatus();

        userIdSet = new HashSet<>();
        userIdSet.addAll(dbDevice.getUserIds());
    }

    @Override
    public CachedDevice getObject()
    {
        return this;
    }

    public String getBucketId()
    {
        return bucketId;
    }

    public String getPlatformDeviceId()
    {
        return platformDeviceId;
    }

    public String getCoreDeviceId()
    {
        return coreDeviceId;
    }

    public String getName()
    {
        return name;
    }

    public String getModelId()
    {
        return modelId;
    }

    public String getDeviceKey()
    {
        return deviceKey;
    }

    public String getHost()
    {
        return host;
    }

    public String getPort()
    {
        return port;
    }

    public String getAddress()
    {
        return address;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public DeviceStatus getStatus()
    {
        return status;
    }

    public Set<String> getUserIdSet()
    {
        return userIdSet == null ? new LinkedHashSet<String>() : userIdSet;
    }

    public boolean isKaiNode()
    {
        MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(this.modelId);
        return deviceModel != null && deviceModel.isKaiNode();
    }
}
