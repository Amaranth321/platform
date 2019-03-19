package jobs.cloud.independent.delivery;

import models.content.DeliveryItem;
import platform.content.delivery.Deliverable;
import platform.content.delivery.DeliveryManager;
import platform.content.delivery.DeliveryMethod;
import platform.content.mobile.push.MobilePushClient;
import platform.content.mobile.push.PushNotificationItem;
import play.jobs.Every;

/**
 * Delivers {@link models.content.DeliveryItem} queued as {@link DeliveryMethod#MOBILE_PUSH}.
 * <p/>
 * DO NOT run this job directly. Use {@link DeliveryManager#queue}.
 *
 * @author Aye Maung
 * @since v4.4
 */
@Every("10s")
public class MobilePushDeliveryJob extends QueuedContentDeliveryJob
{
    private final int RETRY_LIMIT = 0;

    @Override
    protected DeliveryMethod getDeliveryMethod()
    {
        return DeliveryMethod.MOBILE_PUSH;
    }

    @Override
    protected int getRetryLimit()
    {
        return RETRY_LIMIT;
    }

    @Override
    protected void process(DeliveryItem deliveryItem)
    {
        Deliverable<PushNotificationItem> deliverable = deliveryItem.getDeliverable();
        PushNotificationItem pushItem = deliverable.getDetails();
        MobilePushClient client = pushItem.getPushServiceType().getClient();
        if (client.send(pushItem))
        {
            deliverySuccessful(deliveryItem);
        }
        else
        {
            deliveryFailed(deliveryItem);
        }
    }

}
