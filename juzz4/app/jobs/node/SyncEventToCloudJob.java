package jobs.node;

import com.kaisquare.events.thrift.EventDetails;
import jobs.SingletonJob;
import lib.util.Util;
import models.events.EventToCloud;
import platform.node.CloudConnector;
import platform.node.NodeEventClient;
import play.Logger;

/**
 * Singleton job to sync events to cloud.
 * <p>
 * This job will be started by {@link NodeJobsManager}
 *
 * @author Aye Maung
 * @since v4.4
 */
class SyncEventToCloudJob extends SingletonCloudDependentQueueJob<EventToCloud>
{
    private static final SyncEventToCloudJob instance = new SyncEventToCloudJob();

    public static SingletonJob getInstance()
    {
        return instance;
    }

    @Override
    protected int getPauseDuration()
    {
        return 200;
    }

    @Override
    protected EventToCloud getNext()
    {
        EventToCloud dbItem = EventToCloud.getOldest();
        if (dbItem != null && dbItem.isNew())
        {
            return null;
        }

        return dbItem;
    }

    @Override
    protected long countRemainingItems()
    {
        return EventToCloud.q().count();
    }

    @Override
    protected void process(EventToCloud eventToCloud)
    {
        try
        {
            NodeEventClient eventClient = CloudConnector.getInstance().getNodeEventClient();
            EventDetails thriftEvent = eventToCloud.getEventInfo().toThriftEvent(
                    eventToCloud.getJsonData(),
                    eventToCloud.getBinaryData());

            if (eventClient.pushToCloud(thriftEvent))
            {
                eventToCloud.delete();
            }
        }
        catch (Exception e)
        {
            if (!getStatus().equals(NodeJobStatus.ERROR))
            {
                Logger.error(Util.whichFn() + e.getMessage());
                setStatus(NodeJobStatus.ERROR);
            }
        }
    }
}
