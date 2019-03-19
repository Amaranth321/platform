package platform.db.cache.proxies;

import models.node.NodeCamera;
import models.node.NodeObject;
import platform.StorageManager;
import platform.db.cache.CachedObject;
import platform.devices.DeviceChannelPair;

import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CachedNodeCamera extends CachedObject<CachedNodeCamera>
{
    private final DeviceChannelPair camera;
    private final String nodeName;
    private final String cameraName;
    private final String modelId;
    private final String deviceKey;
    private final String host;
    private final String port;
    private final String login;
    private final String address;
    private final String latitude;
    private final String longitude;
    private final int recordingLimitMB;
    private final int recordingUsageMB;

    public CachedNodeCamera(String cacheKey, NodeObject nodeObject, NodeCamera nodeCamera)
    {
        super(cacheKey);

        this.camera = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId);
        nodeName = nodeObject.getName();
        cameraName = nodeCamera.name;
        modelId = nodeCamera.model.getId().toString();
        deviceKey = nodeCamera.deviceKey;
        host = nodeCamera.host;
        port = nodeCamera.port;
        login = nodeCamera.login;
        address = nodeCamera.address;
        latitude = nodeCamera.latitude;
        longitude = nodeCamera.longitude;

        //calculate storage
        recordingLimitMB = nodeObject.getCameraStorageLimit(nodeCamera.nodeCoreDeviceId);
        recordingUsageMB = StorageManager.getInstance().getUploadedRecordingSize(this.camera);
    }

    @Override
    public CachedNodeCamera getObject()
    {
        return this;
    }

    @Override
    public long getTtlMillis()
    {
        return TimeUnit.MINUTES.toMillis(5);
    }

    public String getNodeName()
    {
        return nodeName;
    }

    public String getCameraName()
    {
        return cameraName;
    }

    public String getNodeCoreDeviceId()
    {
        return camera.getCoreDeviceId();
    }

    public String getCameraCoreDeviceId()
    {
        return camera.getChannelId();
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

    public String getLogin()
    {
        return login;
    }

    public String getAddress()
    {
        return address;
    }

    public String getLatitude()
    {
        return latitude;
    }

    public String getLongitude()
    {
        return longitude;
    }

    public long getRecordingLimitMB()
    {
        return recordingLimitMB;
    }

    public long getRecordingUsageMB()
    {
        return recordingUsageMB;
    }
}
