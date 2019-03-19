package platform.rt;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import platform.config.readers.ConfigsShared;
import platform.data.collective.OccupancyData;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.db.cache.proxies.CachedStoreLabel;
import platform.devices.DeviceChannelPair;
import platform.events.EventInfo;
import platform.events.EventType;
import platform.mq.MQConnection;
import play.Logger;

import java.io.IOException;
import java.util.Set;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum RTFeedManager
{
    INSTANCE;

    public static RTFeedManager getInstance()
    {
        return INSTANCE;
    }

    public RTSubscriber<EventRTFeed> addEventSubscriber(long userId, EventType eventType)
    {
        Channel rtChannel = null;
        try
        {
            rtChannel = getRTChannel();
            String queueName = getEventQueueName(userId, eventType);
            return new EventFeedSubscriber(rtChannel, queueName);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            closeChannel(rtChannel);
            return null;
        }
    }

    /**
     * @param eventInfo event arrived
     * @param jsonData  json data of the event
     */
    public void newEventReceived(EventInfo eventInfo, String jsonData)
    {
        if (!ConfigsShared.getInstance().isRealTimeFeedEnabled(eventInfo.getType()))
        {
            return;
        }

        //find owners and push to mq
        CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByCoreId(eventInfo.getCamera().getCoreDeviceId());
        Set<String> deviceOwners = cachedDevice.getUserIdSet();
        for (String deviceOwner : deviceOwners)
        {
            EventRTFeed eventRTFeed = new EventRTFeed(eventInfo, jsonData);
            String queueName = getEventQueueName(Long.parseLong(deviceOwner), eventRTFeed.getEventInfo().getType());
            pushToMQ(queueName, eventRTFeed);
        }
    }

    public RTSubscriber<OccupancyRTFeed> addOccupancySubscriber(long userId, String labelId)
    {
        Channel rtChannel = null;
        try
        {
            rtChannel = getRTChannel();
            String queueName = getOccupancyQueueName(userId, labelId);
            return new OccupancyFeedSubscriber(rtChannel, queueName);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            closeChannel(rtChannel);
            return null;
        }
    }

    public void collectiveOccupancyChanged(OccupancyData occupancyData)
    {
        String labelId = occupancyData.getLabelId();

        //find owners and push to mq
        CachedStoreLabel storeLabel = CacheClient.getInstance().getStoreLabel(labelId);
        for (String deviceOwner : storeLabel.getCameraUserIdList())
        {
            OccupancyRTFeed rtFeed = new OccupancyRTFeed(occupancyData);
            String queueName = getOccupancyQueueName(Long.parseLong(deviceOwner), labelId);
            pushToMQ(queueName, rtFeed);
        }
    }

    public RTSubscriber<VcaChangeFeed> addVcaChangeSubscriber(long userId)
    {
        Channel rtChannel = null;
        try
        {
            rtChannel = getRTChannel();
            String queueName = getVcaChangeQueueName(userId);
            return new VcaChangeRTSubscriber(rtChannel, queueName);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            closeChannel(rtChannel);
            return null;
        }
    }

    /**
     * @param instanceId vca id
     * @param camera     owners of this camera will be notified
     */
    public void vcaInstanceChanged(String instanceId, DeviceChannelPair camera)
    {
        //find owners and push to mq
        CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByCoreId(camera.getCoreDeviceId());
        Set<String> deviceOwners = cachedDevice.getUserIdSet();
        for (String deviceOwner : deviceOwners)
        {
            VcaChangeFeed vcaChangeFeed = new VcaChangeFeed(instanceId, camera);
            String queueName = getVcaChangeQueueName(Long.parseLong(deviceOwner));
            pushToMQ(queueName, vcaChangeFeed);
        }
    }

    public void removeSubscriber(RTSubscriber subscriber)
    {
        subscriber.remove();
    }


    private Channel getRTChannel() throws IOException
    {
        Channel channel = MQConnection.createNewChannel();
        return channel;
    }

    private void closeChannel(Channel rtChannel)
    {
        try
        {
            if (rtChannel != null)
            {
                rtChannel.close();
            }
        }
        catch (Exception e)
        {
        }
    }

    private String getEventQueueName(long userId, EventType eventType)
    {
        return String.format("RT_EVENTS_%s_%s", eventType.toString(), userId);
    }

    private String getOccupancyQueueName(long userId, String labelId)
    {
        return String.format("RT_OCCUPANCY_%s_%s", labelId, userId);
    }

    private String getVcaChangeQueueName(long userId)
    {
        return String.format("RT_VCA_%s", userId);
    }

    private void pushToMQ(String queueName, RTFeed rtFeed)
    {
        Channel rtChannel = null;
        try
        {
            rtChannel = getRTChannel();
            rtChannel.basicPublish("", queueName, MessageProperties.TEXT_PLAIN, rtFeed.json().getBytes());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        closeChannel(rtChannel);
    }
}
