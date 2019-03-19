package platform.devices;

import lib.util.exceptions.ApiException;
import models.mobile.MobileDevice;
import platform.content.mobile.push.PushServiceType;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class MobileDeviceManager
{
    private static final MobileDeviceManager instance = new MobileDeviceManager();

    public static MobileDeviceManager getInstance()
    {
        return instance;
    }

    public void registerDevice(String userId,
                               String deviceToken,
                               String deviceModel,
                               String identifier,
                               PushServiceType pushServiceType,
                               String location)
    {
        Logger.info("[%s] register %s device: deviceToken=%s, location=%s, deviceModel=%s, identifier=%s",
                    userId, pushServiceType,
                    deviceToken, location, deviceModel, identifier);

        MobileDevice mobileDvc = MobileDevice.findByToken(deviceToken);
        if (mobileDvc == null)
        {
            mobileDvc = new MobileDevice(Long.parseLong(userId), deviceModel, pushServiceType);
        }

        mobileDvc.setIdentifier(identifier);
        mobileDvc.setDeviceToken(deviceToken);
        mobileDvc.setLocation(location);
        mobileDvc.setNotificationEnabled(true);
        mobileDvc.save();
    }

    public void removeDevice(String identifier) throws ApiException
    {
        MobileDevice mobileDevice = MobileDevice.findByIdentifier(identifier);
        if (mobileDevice == null)
        {
            return;
        }

        mobileDevice.delete();
    }

    private MobileDeviceManager()
    {
    }
}
