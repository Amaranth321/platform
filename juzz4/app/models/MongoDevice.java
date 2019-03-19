package models;

import com.google.code.morphia.annotations.Entity;
import com.kaisquare.core.thrift.DeviceDetails;
import lib.util.Util;
import models.backwardcompatibility.DeviceModel;
import models.licensing.LicenseStatus;
import models.licensing.NodeLicense;
import models.node.NodeCamera;
import platform.CloudLicenseManager;
import platform.Environment;
import platform.analytics.VcaFeature;
import platform.analytics.VcaType;
import platform.devices.DeviceStatus;
import platform.events.EventType;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author tbnguyen1407
 */
@Entity(value = "Device", noClassnameStored = true)
@Model.NoAutoTimestamp
public class MongoDevice extends Model
{
    // region fields

    private String id;
    private String coreDeviceId;
    private String name;
    private String modelId;
    private String deviceKey;
    private String host;
    private String port;
    private String login;
    private String password;
    private String address;
    private Double latitude;
    private Double longitude;
    private boolean cloudRecordingEnabled;
    private Long lastCheckedTime;
    private String status; //ENUM field

    public String bucketId;
    public List<String> userIds;

    // endregion

    // region getters

    public String getDeviceId()
    {
        return this.id;
    }

    public String getCoreDeviceId()
    {
        return this.coreDeviceId;
    }

    public String getBucketId()
    {
        return this.bucketId;
    }

    public String getName()
    {
        return this.name;
    }

    public String getModelId()
    {
        return this.modelId;
    }

    public String getDeviceKey()
    {
        return this.deviceKey == null ? "" : this.deviceKey;
    }

    public String getHost()
    {
        return this.host;
    }

    public String getPort()
    {
        return this.port;
    }

    public String getLogin()
    {
        return this.login;
    }

    public String getPassword()
    {
        return this.password;
    }

    public String getAddress()
    {
        return this.address;
    }

    public Double getLatitude()
    {
        return this.latitude;
    }

    public Double getLongitude()
    {
        return this.longitude;
    }

    public Boolean isCloudRecordingEnabled()
    {
        return this.cloudRecordingEnabled;
    }

    public Long getLastCheckedTime()
    {
        return this.lastCheckedTime;
    }

    public DeviceStatus getStatus()
    {
        return Util.isNullOrEmpty(status)
               ? DeviceStatus.UNKNOWN
               : DeviceStatus.fromEvent(EventType.parse(status));
    }

    public List<String> getUserIds()
    {
        return this.userIds;
    }

    // endregion

    // region setters

    public void setDeviceId(String newId)
    {
        this.id = newId;
    }

    public void setCoreDeviceId(String newCoreDeviceId)
    {
        this.coreDeviceId = newCoreDeviceId;
    }

    public void setBucketId(String newBucketId)
    {
        this.bucketId = newBucketId;
    }

    public void setName(String newName)
    {
        this.name = newName;
    }

    public void setModelId(String newModelId)
    {
        this.modelId = newModelId;
    }

    public void setDeviceKey(String newDeviceKey)
    {
        this.deviceKey = newDeviceKey;
    }

    public void setHost(String newHost)
    {
        this.host = newHost;
    }

    public void setPort(String newPort)
    {
        this.port = newPort;
    }

    public void setLogin(String newLogin)
    {
        this.login = newLogin;
    }

    public void setPassword(String newPassword)
    {
        this.password = newPassword;
    }

    public void setAddress(String newAddress)
    {
        this.address = newAddress;
    }

    public void setLatitude(Double newLatitude)
    {
        this.latitude = newLatitude;
    }

    public void setLongitude(Double newLongitude)
    {
        this.longitude = newLongitude;
    }

    public void setCloudRecordingEnabled(boolean newCloudRecordingEnabled)
    {
        this.cloudRecordingEnabled = newCloudRecordingEnabled;
    }

    public void setLastCheckedTime(Long newLastCheckedTime)
    {
        this.lastCheckedTime = newLastCheckedTime;
    }

    public void setStatus(DeviceStatus enumStatus)
    {
        status = enumStatus.toString();
    }

    public void setUserIds(List<String> newUserIds)
    {
        this.userIds = newUserIds;
    }

    // endregion

    public MongoDevice()
    {
        this.userIds = new ArrayList<>();
    }

    // region public methods

    // NOTE: this method generate a String Id of Long-parsable Value to be compatible with existing mongo models
    // To be called before saving a new device
    public static String generateNewId()
    {
        Long maxId = 0L;

        CollectionSetting collectionSetting = CollectionSetting.getByCollectionName("Device");
        if (collectionSetting == null)
        {
            List<MongoDevice> entities = MongoDevice.q().fetchAll();
            for (MongoDevice entity : entities)
            {
                Long curId = Long.parseLong(entity.getDeviceId());
                if (curId > maxId)
                {
                    maxId = curId;
                }
            }

            // generate new if not exists
            collectionSetting = new CollectionSetting("Device", maxId);
        }
        else
        {
            maxId = collectionSetting.getMaxLongId();
        }

        // save new max id
        Long newMaxId = maxId + 1;
        collectionSetting.setMaxLongId(newMaxId);
        collectionSetting.save();

        return newMaxId.toString();
    }

    public static MongoDevice getByPlatformId(String newDevicePlatformId)
    {
        return MongoDevice.q().filter("id", newDevicePlatformId).get();
    }

    public static MongoDevice getByCoreId(String newDeviceCoreId)
    {
        return MongoDevice.q().filter("coreDeviceId", newDeviceCoreId).get();
    }

    public static MongoDevice getByDeviceKey(String newDeviceKey)
    {
        return MongoDevice.q().filter("deviceKey", newDeviceKey).get();
    }

    public DeviceDetails toDeviceDetails()
    {
        DeviceDetails deviceDetails = new DeviceDetails();
        deviceDetails.setId(this.coreDeviceId);
        deviceDetails.setName(this.name);
        deviceDetails.setModelId(this.modelId);
        deviceDetails.setKey(this.deviceKey);
        deviceDetails.setHost(this.host);
        deviceDetails.setPort(this.port);
        deviceDetails.setLat(this.latitude.toString());
        deviceDetails.setLng(this.longitude.toString());
        deviceDetails.setLogin(this.login);
        deviceDetails.setPassword(this.password);
        deviceDetails.setAddress(this.address);
        deviceDetails.setCloudRecordingEnabled(String.valueOf(this.cloudRecordingEnabled));

        return deviceDetails;
    }

    public NodeCamera toNodeCamera()
    {
        NodeCamera nodeCamera = new NodeCamera();
        nodeCamera.name = this.name;
        nodeCamera.nodePlatformDeviceId = this.getDeviceId();
        nodeCamera.nodeCoreDeviceId = this.getCoreDeviceId();
        nodeCamera.model = new DeviceModel(MongoDeviceModel.getByModelId(modelId));
        nodeCamera.deviceKey = this.deviceKey;
        nodeCamera.host = this.host;
        nodeCamera.port = this.port;
        nodeCamera.login = this.login;
        nodeCamera.password = this.password;
        nodeCamera.address = this.address;
        nodeCamera.latitude = this.latitude.toString();
        nodeCamera.longitude = this.longitude.toString();
        nodeCamera.cloudRecordingEnabled = this.cloudRecordingEnabled;
        nodeCamera.setStatus(this.getStatus());

        return nodeCamera;
    }

    public boolean addUserId(String newUserId)
    {
        try
        {
            if (!this.userIds.contains(newUserId))
            {
                this.userIds.add(newUserId);
            }
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean removeUserId(String newUserId)
    {
        try
        {
            this.userIds.removeAll(Collections.singleton(newUserId));
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof MongoDevice)
        {
            MongoDevice other = (MongoDevice) o;
            return this.id.equals(other.id);
        }
        return false;
    }

    public boolean isKaiNode()
    {
        MongoDeviceModel model = MongoDeviceModel.q().filter("id", modelId).get();
        return model != null && model.isKaiNode();
    }

    public boolean isSuspended()
    {
        if (!this.isKaiNode())
        {
            return false;
        }

        NodeLicense license = CloudLicenseManager.getInstance().getLicenseByNode(this.id);

        if (license == null)
        {
            Logger.error(Util.whichFn() + "no node license for %s", name);
            return false;
        }

        return !LicenseStatus.ACTIVE.equals(license.status);
    }

    public boolean isLicensedToRun(VcaType vcaType)
    {
        if (!Environment.getInstance().onCloud())
        {
            return true;
        }

        if (!isKaiNode())
        {
            return true;
        }

        VcaFeature requiredFeature = vcaType.getConfigFeature();
        NodeLicense nodeLicense = CloudLicenseManager.getInstance().getLicenseByNode(this.id);
        if (nodeLicense == null)
        {
            Logger.info(Util.whichFn() + "node license not found");
            return false;
        }

        return nodeLicense.featureNameList.contains(requiredFeature.getName());
    }

    // endregion
}
