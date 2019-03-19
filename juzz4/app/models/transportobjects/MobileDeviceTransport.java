package models.transportobjects;

import models.mobile.MobileDevice;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class MobileDeviceTransport
{
    public final String identifier;
    public final String name;
    public final String model;
    public final String location;
    public final boolean notificationEnabled;
    public final long created;

    public MobileDeviceTransport(MobileDevice dbDevice)
    {
        this.identifier = dbDevice.getIdentifier();
        this.name = dbDevice.getName();
        this.model = dbDevice.getDeviceModel();
        this.location = dbDevice.getLocation();
        this.notificationEnabled = dbDevice.isNotificationEnabled();
        this.created = dbDevice._getCreated();
    }
}
