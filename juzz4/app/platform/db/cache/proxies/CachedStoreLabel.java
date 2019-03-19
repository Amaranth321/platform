package platform.db.cache.proxies;

import models.labels.LabelStore;
import platform.common.Location;
import platform.db.cache.CacheClient;
import platform.db.cache.CachedObject;
import platform.devices.DeviceChannelPair;
import platform.time.OperatingSchedule;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class CachedStoreLabel extends CachedObject<CachedStoreLabel>
{
    private final long bucketId;
    private final String labelId;
    private final String labelName;
    private final List<DeviceChannelPair> assignedCameras;
    private final Location location;
    private final OperatingSchedule schedule;

    public CachedStoreLabel(String cacheKey, LabelStore storeLabel)
    {
        super(cacheKey);
        this.bucketId = storeLabel.getBucketId();
        this.labelId = storeLabel.getLabelId();
        this.labelName = storeLabel.getLabelName();
        this.assignedCameras = storeLabel.getCameraList();
        this.location = storeLabel.getLocation();
        this.schedule = storeLabel.getSchedule();
    }

    @Override
    public CachedStoreLabel getObject()
    {
        return this;
    }

    public long getBucketId()
    {
        return bucketId;
    }

    public String getLabelId()
    {
        return labelId;
    }

    public String getLabelName()
    {
        return labelName;
    }

    public List<DeviceChannelPair> getAssignedCameras()
    {
        return assignedCameras;
    }

    public Location getLocation()
    {
        return location;
    }

    public OperatingSchedule getSchedule()
    {
        return schedule;
    }

    public List<String> getCameraUserIdList()
    {
        List<String> userIdList = new ArrayList<>();
        for (DeviceChannelPair camera : assignedCameras)
        {
            CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByCoreId(camera.getCoreDeviceId());
            if (cachedDevice == null)
            {
                Logger.error("CachedDevice is null (coreDeviceId=%s)", camera.getCoreDeviceId());
                continue;
            }

            userIdList.addAll(new ArrayList<>(cachedDevice.getUserIdSet()));
        }
        return userIdList;
    }
}
