package platform.notification;

import lib.util.Util;
import models.node.NodeSettings;
import models.notification.EventToNotify;
import models.notification.SentNotification;
import platform.common.Location;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.db.cache.proxies.CachedNodeCamera;
import platform.db.cache.proxies.CachedNodeObjectInfo;
import platform.devices.DeviceChannelPair;
import platform.events.EventInfo;
import platform.events.EventManager;
import platform.events.EventType;
import play.Logger;
import play.i18n.Messages;

import java.util.Map;
import java.util.TimeZone;

/**
 * DO NOT modify the fields. {@link SentNotification} entries will be corrupted
 * <p/>
 * The purpose of this class is to pass around pre-calculated information
 * to reduce redundant codes (e.g. getting deviceName)
 * <p/>
 * Only for camera-based notifications.
 * <p/>
 * For label-based, refer to {@link LabelNotificationInfo}
 *
 * @author Aye Maung
 * @since v4.4
 */
public class NotificationInfo
{
    private final String eventId;
    private final long eventTime;
    private final EventType eventType;
    private final DeviceChannelPair camera;
    private final long platformDeviceId;
    private final String deviceName;
    private final String channelName;
    private final Location location;
    private final Map notificationData;

    public static NotificationInfo forEvent(EventToNotify eventToNotify)
    {
        EventInfo eventInfo = eventToNotify.getEventInfo();
        CacheClient cacheClient = CacheClient.getInstance();
        CachedDevice device = cacheClient.getDeviceByCoreId(eventInfo.getCamera().getCoreDeviceId());
        if (device == null)
        {
            Logger.error(Util.whichFn() + "Event device (%s) no longer exists", eventInfo.getCamera());
            return null;
        }

        if (device.isKaiNode())
        {
            CachedDevice node = device;
            CachedNodeCamera nodeCamera = cacheClient.getNodeCamera(eventInfo.getCamera());
            if (nodeCamera == null)
            {
                Logger.error(Util.whichFn() + "Node camera (%s) no longer exists", eventInfo.getCamera());
                return null;
            }

            CachedNodeObjectInfo nodeObjectInfo = cacheClient.getNodeObject(node);
            NodeSettings nodeSettings = nodeObjectInfo.getSettings();
            Location loc = new Location(node.getAddress(),
                                        node.getLatitude(),
                                        node.getLongitude(),
                                        nodeSettings.getTimezone());

            return new NotificationInfo(eventInfo,
                                        Long.parseLong(node.getPlatformDeviceId()),
                                        node.getName(),
                                        nodeCamera.getCameraName(),
                                        loc,
                                        eventToNotify.getNotificationData());
        }
        else
        {
            String timeZoneId = TimeZone.getDefault().getID();
            Location loc = new Location(device.getAddress(),
                                        device.getLatitude(),
                                        device.getLongitude(),
                                        timeZoneId);

            return new NotificationInfo(eventInfo,
                                        Long.parseLong(device.getPlatformDeviceId()),
                                        device.getName(),
                                        eventInfo.getCamera().getChannelId(),
                                        loc,
                                        eventToNotify.getNotificationData());
        }
    }

    public String getEventId()
    {
        return eventId;
    }

    public long getEventTime()
    {
        return eventTime;
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public DeviceChannelPair getCamera()
    {
        return camera;
    }

    public long getPlatformDeviceId()
    {
        return platformDeviceId;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public String getChannelName()
    {
        return channelName;
    }

    public Location getLocation()
    {
        return location;
    }

    public Map getNotificationData()
    {
        return notificationData;
    }

    public String getDeviceDisplayText()
    {
        return String.format("%s - %s", deviceName, channelName);
    }

    public String getLocalizedEventName()
    {
        return Messages.get(eventType.toString());
    }

    public String getEventVideoUrl()
    {
        return EventManager.getInstance().getEventVideoUrl(eventId);
    }
    
    public String getEventImageUrl() {
    	return String.format("%s.%s",eventTime,"jpg");
    }

    public NotificationInfo(EventInfo eventInfo,
                            long platformDeviceId,
                            String deviceName,
                            String channelName,
                            Location location,
                            Map notificationData)
    {
        this.eventId = eventInfo.getEventId();
        this.eventTime = eventInfo.getTime();
        this.eventType = eventInfo.getType();
        this.camera = eventInfo.getCamera();
        this.platformDeviceId = platformDeviceId;
        this.deviceName = deviceName;
        this.channelName = channelName;
        this.location = location;
        this.notificationData = notificationData;
    }
}
