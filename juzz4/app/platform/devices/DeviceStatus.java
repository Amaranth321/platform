package platform.devices;

import platform.events.EventType;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum DeviceStatus
{
    UNKNOWN,
    CONNECTED,
    DISCONNECTED;

    public static DeviceStatus fromEvent(EventType eventType)
    {
        switch (eventType)
        {
            case CORE_DEVICE_CONNECTED:
                return DeviceStatus.CONNECTED;

            case CORE_DEVICE_CONNECTION_LOST:
            case CORE_DEVICE_CONNECTION_FAILED:
                return DeviceStatus.DISCONNECTED;

            default:
                Logger.error("Unknown device status event (%s)", eventType.toString());
                return UNKNOWN;
        }
    }

    @Override
    public String toString()
    {
        switch (this)
        {
            case CONNECTED:
                return EventType.CORE_DEVICE_CONNECTED.toString();

            case DISCONNECTED:
                return EventType.CORE_DEVICE_CONNECTION_LOST.toString();

            default:
                return "";
        }
    }
}
