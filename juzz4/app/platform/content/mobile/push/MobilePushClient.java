package platform.content.mobile.push;

/**
 * @author Aye Maung
 * @since v4.4
 */
public interface MobilePushClient
{
    boolean send(PushNotificationItem pushItem);
}
