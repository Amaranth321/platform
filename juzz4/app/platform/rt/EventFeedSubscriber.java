package platform.rt;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;

/**
 * @author Aye Maung
 * @since v4.4
 */
class EventFeedSubscriber extends RTSubscriber<EventRTFeed>
{
    public EventFeedSubscriber(Channel channel, String queueName) throws IOException
    {
        super(channel, queueName);
    }

    @Override
    protected EventRTFeed extractMessage(QueueingConsumer.Delivery delivery)
    {
        if (delivery == null)
        {
            return null;
        }

        String message = new String(delivery.getBody());
        return new Gson().fromJson(message, EventRTFeed.class);
    }
}
