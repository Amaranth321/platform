package jobs.cloud.independent.delivery;

import jobs.cloud.CloudCronJob;
import models.content.DeliveryItem;
import platform.content.delivery.DeliveryManager;
import platform.content.delivery.DeliveryMethod;
import platform.content.delivery.DeliveryStats;
import play.Logger;
import play.modules.morphia.Model;

/**
 * Serves as the template for all delivery jobs to inherit from.
 * <p/>
 * All delivery items must be queued as {@link models.content.DeliveryItem} in the database
 * with the correct {@link DeliveryMethod}
 * using {@link DeliveryManager#queue}
 *
 * @author Aye Maung
 * @since v4.3
 */
abstract class QueuedContentDeliveryJob extends CloudCronJob
{
    protected abstract DeliveryMethod getDeliveryMethod();

    protected abstract void process(DeliveryItem deliveryItem);

    protected abstract int getRetryLimit();

    protected DeliveryItem getNextInQueue()
    {
        return query().first();
    }

    protected void deliverySuccessful(DeliveryItem deliveryItem)
    {
        deliveryItem.delete();
        updateStats(getDeliveryMethod(), true);
    }

    protected void deliveryFailed(DeliveryItem deliveryItem)
    {
        deliveryItem.incrementAttempts();
        updateStats(getDeliveryMethod(), false);
    }

    @Override
    public void doJob()
    {
        DeliveryItem deliveryItem = getNextInQueue();
        while (deliveryItem != null)
        {
            try
            {
                process(deliveryItem);
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
            deliveryItem = getNextInQueue();
        }
    }

    protected QueuedContentDeliveryJob()
    {
        DeliveryStats.reset(getDeliveryMethod());
    }

    private Model.MorphiaQuery query()
    {
        return DeliveryItem.q()
                .filter("method", getDeliveryMethod())
                .filter("attempts <=", getRetryLimit())
                .order("attempts, _created");
    }

    private void updateStats(DeliveryMethod method, boolean success)
    {
        DeliveryStats stats = DeliveryStats.get();
        if (success)
        {
            stats.incrementSuccess(method);
        }
        else
        {
            stats.incrementFail(method);
        }
        stats.setRemaining(method, (int) query().count());
        stats.save();
    }

}
