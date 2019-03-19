package platform.rt;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;

/**
 * @author Aye Maung
 * @since v4.5
 */
class VcaChangeRTSubscriber extends RTSubscriber<VcaChangeFeed>
{
    public VcaChangeRTSubscriber(Channel rtChannel, String queueName) throws IOException
    {
        super(rtChannel, queueName);
    }

    @Override
    protected VcaChangeFeed extractMessage(QueueingConsumer.Delivery delivery)
    {
        if (delivery == null)
        {
            return null;
        }

        String message = new String(delivery.getBody());
        return new Gson().fromJson(message, VcaChangeFeed.class);
    }
}
