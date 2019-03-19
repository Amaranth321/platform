package platform.rt;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import play.Logger;
import play.jobs.Job;
import play.libs.F;

import java.io.IOException;

/**
 * Real time feed listener.
 *
 * @author Aye Maung
 * @since v4.4
 */
public abstract class RTSubscriber<T extends RTFeed>
{
    private final Channel channel;
    private final QueueingConsumer consumer;
    private final String queueName;

    public RTSubscriber(Channel rtChannel, String queueName) throws IOException
    {
        this.channel = rtChannel;
        this.queueName = queueName;
        consumer = new QueueingConsumer(rtChannel);

        rtChannel.queueDeclare(queueName, false, false, true, null);  // !durable, !exclusive, autoDelete
        rtChannel.basicConsume(queueName, true, consumer);
    }

    public F.Promise<T> getNext(final long timeout) throws Exception
    {
        Job<T> waitJob = new Job<T>()
        {
            @Override
            public T doJobWithResult()
            {
                try
                {
                    return extractMessage(consumer.nextDelivery(timeout));
                }
                catch (InterruptedException e)
                {
                    Logger.error(e, "");
                    return null;
                }
            }
        };

        return waitJob.now();
    }

    public void remove()
    {
        try
        {
            channel.close();
        }
        catch (Exception e)
        {
        }
    }

    public String getQueueName()
    {
        return queueName;
    }

    protected abstract T extractMessage(QueueingConsumer.Delivery delivery);
}
