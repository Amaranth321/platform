package platform.content.delivery;

import lib.util.Util;
import models.cloud.UIConfigurableCloudSettings;
import platform.NotificationManager;
import platform.content.email.EmailComposer;
import platform.content.email.EmailItem;
import platform.content.mobile.push.PushMessage;
import platform.content.mobile.push.PushMessageType;
import platform.content.mobile.sms.SMSClient;
import platform.content.mobile.sms.SMSComposer;
import platform.content.mobile.sms.SMSItem;
import platform.db.cache.proxies.CachedUser;
import platform.notification.NotificationInfo;
import platform.notification.OnScreenData;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class DeviceEventNotifier implements EventNotifier
{
    private final DeliveryManager deliveryMgr = DeliveryManager.getInstance();
    private final CachedUser user;
    private final NotificationInfo info;

    public DeviceEventNotifier(CachedUser user, NotificationInfo info)
    {
        this.user = user;
        this.info = info;
    }

    @Override
    public boolean onScreen()
    {
        try
        {
            OnScreenData screenData = OnScreenData.forCameraNotification(info);
            String jsonData = screenData.toApiOutput();

            //send to mq
            NotificationManager.getManager().publishEventNotification(Long.parseLong(user.getUserId()), jsonData);
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    @Override
    public boolean toEmail()
    {
        //no email info or email disabled
        boolean emailEnabled = UIConfigurableCloudSettings.server().delivery().allowEmail;
        if (!emailEnabled || Util.isNullOrEmpty(user.getEmail()))
        {
            return false;
        }

        EmailItem emailItem = EmailComposer.getInstance().createEventAlertEmail(user, info);
        return deliveryMgr.queue(DeliveryMethod.EMAIL, new Deliverable<>(emailItem));
    }

    @Override
    public boolean toSMS()
    {
        //no phone number or sms disabled
        if (Util.isNullOrEmpty(user.getPhone()) || !SMSClient.isEnabled())
        {
            return false;
        }

        SMSItem smsItem = SMSComposer.getInstance().createAlertSMS(user, info);
        return deliveryMgr.queue(DeliveryMethod.SMS, new Deliverable<>(smsItem));
    }

    @Override
    public boolean toMobilePush()
    {
        //mobile push disabled
        if (!UIConfigurableCloudSettings.server().delivery().allowMobilePush)
        {
            return false;
        }

        //message
        String displayName = String.format("%s event from %s",
                                           info.getLocalizedEventName(),
                                           info.getDeviceDisplayText());

        PushMessage pushMessage = new PushMessage(
                displayName,
                PushMessageType.ALERT,
                String.valueOf(info.getPlatformDeviceId())
        );

        return deliveryMgr.queuePushMessage(Long.parseLong(user.getUserId()), pushMessage);
    }
}
