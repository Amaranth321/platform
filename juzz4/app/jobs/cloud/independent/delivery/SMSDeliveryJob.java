package jobs.cloud.independent.delivery;

import models.content.DeliveryItem;
import platform.content.delivery.Deliverable;
import platform.content.delivery.DeliveryManager;
import platform.content.delivery.DeliveryMethod;
import platform.content.mobile.sms.SMSClient;
import platform.content.mobile.sms.SMSItem;
import play.jobs.Every;

/**
 * Delivers {@link models.content.DeliveryItem} queued as {@link DeliveryMethod#SMS}
 * <p/>
 * DO NOT run this job directly. Use {@link DeliveryManager#queue}
 *
 * @author Aye Maung
 * @since v4.4
 */
@Every("10s")
public class SMSDeliveryJob extends QueuedContentDeliveryJob
{
    private final int RETRY_LIMIT = 0;

    @Override
    protected DeliveryMethod getDeliveryMethod()
    {
        return DeliveryMethod.SMS;
    }

    @Override
    protected int getRetryLimit()
    {
        return RETRY_LIMIT;
    }

    @Override
    protected void process(DeliveryItem deliveryItem)
    {
        Deliverable<SMSItem> deliverable = deliveryItem.getDeliverable();
        SMSItem smsItem = deliverable.getDetails();
        if (SMSClient.getInstance().send(smsItem))
        {
            deliverySuccessful(deliveryItem);
        }
        else
        {
            deliveryFailed(deliveryItem);
        }
    }

}
