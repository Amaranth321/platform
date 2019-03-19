package models.mobile;

import com.google.code.morphia.annotations.Entity;
import platform.content.mobile.push.PushServiceType;
import play.modules.morphia.Model;

/**
 * {@link #identifier} is the UUID generated on the app. Unique per app installation
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class MobileDevice extends Model
{
    private final long userId;
    private final String deviceModel;
    private final PushServiceType pushServiceType;
    private String identifier;
    private String deviceToken;
    private String name;
    private String location;
    private boolean notificationEnabled;

    public static MorphiaQuery findByUserId(long userId)
    {
        return MobileDevice.q().filter("userId", userId);
    }

    public static MobileDevice findByIdentifier(String identifier)
    {
        return MobileDevice.q().filter("identifier", identifier).first();
    }

    public static MobileDevice findByToken(String deviceToken)
    {
        return MobileDevice.q().filter("deviceToken", deviceToken).first();
    }

    public MobileDevice(long userId, String deviceModel, PushServiceType pushServiceType)
    {
        this.userId = userId;
        this.deviceModel = deviceModel;
        this.pushServiceType = pushServiceType;
        this.name = deviceModel;
        this.notificationEnabled = true;
    }

    public long getUserId()
    {
        return userId;
    }

    public String getDeviceModel()
    {
        return deviceModel;
    }

    public PushServiceType getPushServiceType()
    {
        return pushServiceType;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    public String getDeviceToken()
    {
        return deviceToken;
    }

    public void setDeviceToken(String newToken)
    {
        deviceToken = newToken;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public boolean isNotificationEnabled()
    {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled)
    {
        this.notificationEnabled = notificationEnabled;
    }
}
