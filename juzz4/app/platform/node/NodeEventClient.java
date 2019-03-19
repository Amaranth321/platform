package platform.node;

import com.kaisquare.events.thrift.EventDetails;
import com.kaisquare.kaisync.platform.IPlatformSync;
import com.kaisquare.kaisync.platform.MessagePacket;
import lib.util.exceptions.ApiException;
import models.node.NodeInfo;
import platform.CloudSyncManager;
import platform.Environment;
import platform.events.EventManager;
import play.Logger;

/**
 * Used to send events from node platform to cloud
 */
public class NodeEventClient
{
    private boolean enabled;

    private IPlatformSync platformClient;

    private NodeEventClient()
    {
        enabled = true;
    }

    public static NodeEventClient getInstance()
    {
        return Holder.INSTANCE;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean pushToCloud(EventDetails nodeEvent) throws ApiException
    {
        if (!enabled)
        {
            return true;
        }

        EventDetails evtToSent = nodeEvent.deepCopy();

        if (Environment.getInstance().onKaiNode())
        {            //this check is just to exempt simulated events
            NodeInfo nodeInfo = NodeManager.getInstance().getNodeInfo();
            //node's device ID on cloud server should be set as the deviceId
            //in the event object; and the local core device ID acts as the channel ID
            String deviceIdOnCloud = nodeInfo.getCloudCoreDeviceId();
            String channelIdOnCloud = evtToSent.getDeviceId();
            evtToSent.setDeviceId(deviceIdOnCloud);
            evtToSent.setChannelId(channelIdOnCloud);
        }

        try
        {
            synchronized (this)
            {
                if (platformClient == null)
                {
                    platformClient = CloudSyncManager.getInstance().getPlatformSync();
                }
            }
            MessagePacket packet = EventManager.getInstance().convertToMessagePacket(evtToSent);
            boolean result = platformClient.pushEvent(packet.toBytes());
            Logger.debug("push event %s: %s", evtToSent, result);
            return result;
        }
        catch (Exception e)
        {
            Logger.error("ERROR in NodeEventClient: " + e.getMessage());
            return false;
        }
    }

    private static class Holder
    {
        public static final NodeEventClient INSTANCE = new NodeEventClient();
    }
}
