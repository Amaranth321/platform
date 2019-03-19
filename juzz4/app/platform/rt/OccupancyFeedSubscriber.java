package platform.rt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import platform.devices.DeviceChannelPair;

import java.io.IOException;

/**
 * @author Aye Maung
 * @since v4.5
 */
class OccupancyFeedSubscriber extends RTSubscriber<OccupancyRTFeed>
{
    public OccupancyFeedSubscriber(Channel channel, String queueName) throws IOException
    {
        super(channel, queueName);
    }

    @Override
    protected OccupancyRTFeed extractMessage(QueueingConsumer.Delivery delivery)
    {
        if (delivery == null)
        {
            return null;
        }

        //parser
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DeviceChannelPair.class, new DeviceChannelPair.Deserializer());
        Gson gson = gsonBuilder.create();

        String message = new String(delivery.getBody());
        return gson.fromJson(message, OccupancyRTFeed.class);
    }
}
