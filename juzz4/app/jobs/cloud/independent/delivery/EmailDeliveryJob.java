package jobs.cloud.independent.delivery;

import models.content.DeliveryItem;
import platform.content.delivery.Deliverable;
import platform.content.delivery.DeliveryManager;
import platform.content.delivery.DeliveryMethod;
import platform.content.email.EmailClient;
import platform.content.email.EmailItem;
import play.Logger;
import play.jobs.Every;

/**
 * Delivers {@link DeliveryItem} queued as {@link DeliveryMethod#EMAIL}.
 * <p/>
 * DO NOT run this job directly. Use {@link DeliveryManager#queue}.
 *
 * @author Aye Maung
 * @since v4.3
 */
@Every("10s")
public class EmailDeliveryJob extends QueuedContentDeliveryJob
{
    private final int RETRY_LIMIT = 0;

    @Override
    protected DeliveryMethod getDeliveryMethod()
    {
        return DeliveryMethod.EMAIL;
    }

    @Override
    protected int getRetryLimit()
    {
        return RETRY_LIMIT;
    }

    @Override
    protected void process(DeliveryItem deliveryItem)
    {
        Deliverable<EmailItem> deliverable = deliveryItem.getDeliverable();
        EmailItem emailItem = deliverable.getDetails();
        EmailClient emailClient = new EmailClient(emailItem);
        if (emailClient.send())
        {
            deliverySuccessful(deliveryItem);
        }
        else
        {
            Logger.error("Email sending failed (%s)", emailItem);
            deliveryFailed(deliveryItem);
        }
    }
}
