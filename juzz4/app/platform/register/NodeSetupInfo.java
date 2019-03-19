package platform.register;

import com.google.gson.Gson;
import models.backwardcompatibility.DeviceModel;
import models.licensing.NodeLicense;
import models.notification.BucketNotificationSettings;

/**
 * DO NOT USE enum here directly.
 * <p/>
 * If the server sends back an enum type that the node doesn't have (lower versions),
 * registration/replacement will fail
 * <p/>
 * Use json strings and manually parse them on the node side.
 * Encapsulate these conversions inside get/set functions
 *
 * @author Aye Maung
 */
public class NodeSetupInfo
{
    private String bucketName;
    private String nodeName;
    private Long cloudPlatformDeviceId;
    private String cloudCoreDeviceId;
    private NodeLicense nodeLicense;
    private BrandingAssets brandingAssets;
    private String jsonNotificationSettings;
    private DeviceModel deviceModel;

    public String getBucketName()
    {
        return bucketName;
    }

    public void setBucketName(String bucketName)
    {
        this.bucketName = bucketName;
    }

    public String getNodeName()
    {
        return nodeName;
    }

    public void setNodeName(String nodeName)
    {
        this.nodeName = nodeName;
    }

    public Long getCloudPlatformDeviceId()
    {
        return cloudPlatformDeviceId;
    }

    public void setCloudPlatformDeviceId(Long cloudPlatformDeviceId)
    {
        this.cloudPlatformDeviceId = cloudPlatformDeviceId;
    }

    public NodeLicense getNodeLicense()
    {
        return nodeLicense;
    }

    public void setNodeLicense(NodeLicense nodeLicense)
    {
        this.nodeLicense = nodeLicense;
    }

    public BrandingAssets getBrandingAssets()
    {
        return brandingAssets;
    }

    public void setBrandingAssets(BrandingAssets brandingAssets)
    {
        this.brandingAssets = brandingAssets;
    }

    public String getCloudCoreDeviceId()
    {
        return cloudCoreDeviceId;
    }

    public void setCloudCoreDeviceId(String cloudCoreDeviceId)
    {
        this.cloudCoreDeviceId = cloudCoreDeviceId;
    }

    public BucketNotificationSettings getNotificationSettings()
    {
        return BucketNotificationSettings.parse(jsonNotificationSettings);
    }

    public void setNotificationSettings(BucketNotificationSettings notificationSettings)
    {
        this.jsonNotificationSettings = new Gson().toJson(notificationSettings);
    }

    public DeviceModel getDeviceModel()
    {
        return deviceModel;
    }

    public void setDeviceModel(DeviceModel deviceModel)
    {
        this.deviceModel = deviceModel;
    }
}
