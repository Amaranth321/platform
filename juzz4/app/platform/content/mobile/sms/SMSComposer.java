package platform.content.mobile.sms;

import org.joda.time.DateTime;
import platform.db.cache.proxies.CachedUser;
import platform.notification.LabelNotificationInfo;
import platform.notification.NotificationInfo;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum SMSComposer
{
    INSTANCE;

    private static final String DEFAULT_SENDER = "KAI UP";

    public static SMSComposer getInstance()
    {
        return INSTANCE;
    }

    public SMSItem createAlertSMS(CachedUser user, NotificationInfo notificationInfo)
    {
        //get device time
        DateTime dtDevice = new DateTime(notificationInfo.getEventTime(), notificationInfo.getLocation().getTimeZone());
        String timestamp = dtDevice.toString("dd/MM/yyyy HH:mm:ss z");

        //text body
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("[Alert] %s\n", notificationInfo.getLocalizedEventName()));
        builder.append(String.format("From : %s\n", notificationInfo.getDeviceDisplayText()));
        builder.append(String.format("At   : %s\n", timestamp));
        String textBody = builder.toString();

        SMSItem smsItem = new SMSItem(DEFAULT_SENDER, user.getPhone(), textBody);
        return smsItem;
    }

    public SMSItem createLabelNotification(CachedUser user, LabelNotificationInfo info)
    {
        //get device time
        DateTime dtDevice = new DateTime(info.getEventTime(), info.getLocation().getTimeZone());
        String timestamp = dtDevice.toString("dd/MM/yyyy HH:mm:ss z");

        //text body
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s\n", info.getLocalizedEventName()));
        builder.append(String.format("From : %s\n", info.getLabelName()));
        builder.append(String.format("Time : %s\n", timestamp));

        //type-specific info
        switch (info.getType())
        {
            case OCCUPANCY:
                builder.append(String.format("Limit : %s\n", info.getNotificationData().get("occupancyLimit")));
                builder.append(String.format("Message : %s\n", info.getNotificationData().get("message")));
        }

        String textBody = builder.toString();

        SMSItem smsItem = new SMSItem(DEFAULT_SENDER, user.getPhone(), textBody);
        return smsItem;
    }
}
