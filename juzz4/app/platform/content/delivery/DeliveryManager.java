package platform.content.delivery;

import lib.util.Util;
import models.content.DeliveryItem;
import models.mobile.MobileDevice;
import platform.content.mobile.push.PushMessage;
import platform.content.mobile.push.PushNotificationItem;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.3
 */
public enum DeliveryManager
{
    INSTANCE;

    public static DeliveryManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Use respective composers below to better manage deliverable items.<p/>
     * {@link platform.content.email.EmailComposer}<p/>
     * {@link platform.content.mobile.sms.SMSComposer}
     *
     * @param method
     * @param deliverable
     */
    public boolean queue(DeliveryMethod method, Deliverable deliverable)
    {
        if (!method.hasValidItem(deliverable))
        {
            Logger.error(Util.whichFn() + "invalid item for delivery by %", method);
            return false;
        }

        DeliveryItem deliveryItem = new DeliveryItem(method, deliverable);
        deliveryItem.save();
        return true;
    }

    public boolean queuePushMessage(long userId, PushMessage pushMessage)
    {
        List<PushNotificationItem> pushItemList = new ArrayList<>();

        //user's mobile devices
        Iterable<MobileDevice> mobileDevices = MobileDevice.findByUserId(userId).fetch();
        for (MobileDevice dvc : mobileDevices)
        {
            if (!dvc.isNotificationEnabled())
            {
                continue;
            }

            PushNotificationItem pushItem = new PushNotificationItem(
                    dvc.getPushServiceType(),
                    dvc.getDeviceToken(),
                    dvc.getUserId(),
                    pushMessage
            );
            pushItemList.add(pushItem);
        }

        //nothing to deliver
        if (pushItemList.isEmpty())
        {
            return false;
        }

        //queue
        for (PushNotificationItem pushItem : pushItemList)
        {
            queue(DeliveryMethod.MOBILE_PUSH, new Deliverable<>(pushItem));
        }

        return true;
    }
}
