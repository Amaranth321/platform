package models.backwardcompatibility;

import lib.util.Util;
import models.*;
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
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static lib.util.Util.isNullOrEmpty;

/**
 * Device.
 *
 * @author kdp
 */
@Entity
@Table(name = "`devices`")
@Deprecated
public class Device extends Model
{
    public String name;
    public String deviceId;

    @ManyToOne
    public DeviceModel model;

    public String deviceKey;
    public String host;
    public String port;
    public String login;
    public String password;
    public String address;
    public String latitude;
    public String longitude;
    public boolean cloudRecordingEnabled;
    public long lastCheckedTime;

    //ENUM field
    private String status;

    @ManyToOne
    public Bucket bucket;
    @ManyToMany
    public Collection<User> users;

    public Device(MongoDevice mongoDevice)
    {
        this.id = Long.parseLong(mongoDevice.getDeviceId());
        this.model = new DeviceModel(MongoDeviceModel.getByModelId(mongoDevice.getModelId()));
        this.name = mongoDevice.getName();
        this.deviceId = mongoDevice.getCoreDeviceId();
        this.deviceKey = mongoDevice.getDeviceKey();
        this.host = mongoDevice.getHost();
        this.port = mongoDevice.getPort();
        this.login = mongoDevice.getLogin();
        this.password = mongoDevice.getPassword();
        this.address = mongoDevice.getAddress();
        this.latitude = mongoDevice.getLatitude().toString();
        this.longitude = mongoDevice.getLongitude().toString();
        this.cloudRecordingEnabled = mongoDevice.isCloudRecordingEnabled();
        this.status = mongoDevice.getStatus().toString();
        if (mongoDevice.getLastCheckedTime() != null)
        {
            this.lastCheckedTime = mongoDevice.getLastCheckedTime();
        }

        MongoBucket mongoBucket = MongoBucket.getById(mongoDevice.getBucketId());
        this.bucket = new Bucket(mongoBucket);

        List<String> userIds = mongoDevice.getUserIds();
        List<User> sqlUsers = new ArrayList<>();
        for (String userId : userIds)
        {
            MongoUser mongoUser = MongoUser.getById(userId);
            sqlUsers.add(new User(mongoUser));
        }
        this.users = sqlUsers;
    }

    public Device()
    {
        this.users = new ArrayList<User>();
    }

    public boolean addUser(User user)
    {
        try
        {
            if (user == null)
            {
                Logger.warn("Invalid user object");
                return false;
            }
            else if (this.users.contains(user))
            {
                return true;
            }
            else
            {
                this.users.add(user);
            }
            return true;
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean removeUser(User user)
    {
        try
        {
            if (this.users.contains(user))
            {
                this.users.remove(user);
                return true;
            }
            return false;
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    public NodeCamera toNodeCamera()
    {
        NodeCamera nodeCamera = new NodeCamera();
        nodeCamera.name = this.name;
        nodeCamera.nodePlatformDeviceId = this.getId().toString();
        nodeCamera.nodeCoreDeviceId = this.deviceId;
        //nodeCamera.model = this.model;
        nodeCamera.deviceKey = this.deviceKey;
        nodeCamera.host = this.host;
        nodeCamera.port = this.port;
        nodeCamera.login = this.login;
        nodeCamera.password = this.password;
        nodeCamera.address = this.address;
        nodeCamera.latitude = this.latitude;
        nodeCamera.longitude = this.longitude;
        nodeCamera.cloudRecordingEnabled = this.cloudRecordingEnabled;
        nodeCamera.setStatus(this.getStatus());

        return nodeCamera;
    }

    public double[] getLocation()
    {
        double lat = 0.0;
        double lng = 0.0;
        if (Util.isDouble(latitude) && Util.isDouble(longitude))
        {
            lat = Double.parseDouble(latitude);
            lng = Double.parseDouble(longitude);
        }

        return new double[]{lat, lng};
    }

    public DeviceStatus getStatus()
    {
        if (Util.isNullOrEmpty(status))
        {
            return DeviceStatus.UNKNOWN;
        }
        EventType eventType = EventType.parse(status);
        return DeviceStatus.fromEvent(eventType);
    }

    public void setStatus(DeviceStatus enumStatus)
    {
        status = enumStatus.toString();
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Device)
        {
            Device other = (Device) o;
            return this.deviceId.equals(other.deviceId);
        }
        return false;
    }

    public boolean isKaiNode()
    {
        if (isNullOrEmpty(model.capabilities))
        {
            return false;
        }

        return model.isKaiNode();
    }

    public boolean isSuspended()
    {
        if (!this.isKaiNode())
        {
            return false;
        }

        NodeLicense license = CloudLicenseManager.getInstance().getLicenseByNode(this.id.toString());
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
        NodeLicense nodeLicense = CloudLicenseManager.getInstance().getLicenseByNode(this.id.toString());
        if (nodeLicense == null)
        {
            Logger.info(Util.whichFn() + "node license not found");
            return false;
        }

        return nodeLicense.featureNameList.contains(requiredFeature.getName());
    }
}

