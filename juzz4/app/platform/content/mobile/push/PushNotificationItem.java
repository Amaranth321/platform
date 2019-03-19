package platform.content.mobile.push;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class PushNotificationItem
{
    private final PushServiceType serviceType;
    private final String deviceToken;
    private final long userId;
    private final PushMessage message;

    public PushNotificationItem(PushServiceType serviceType,
                                String deviceToken,
                                long userId,
                                PushMessage message)
    {
        this.serviceType = serviceType;
        this.deviceToken = deviceToken;
        this.userId = userId;
        this.message = message;
    }

    public PushServiceType getPushServiceType()
    {
        return serviceType;
    }

    public String getDeviceToken()
    {
        return deviceToken;
    }

    public long getUserId()
    {
        return userId;
    }

    public PushMessage getMessage()
    {
        return message;
    }

    @Override
    public String toString()
    {
        return String.format("to %s (%s)", userId, message);
    }

}
