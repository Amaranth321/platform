package jobs.node;

import lib.util.Util;
import models.licensing.LicenseStatus;
import models.licensing.NodeLicense;
import models.node.NodeInfo;
import platform.Environment;
import platform.config.readers.ConfigsNode;
import platform.node.NodeManager;
import platform.node.NodeProvisioning;
import play.Logger;
import play.jobs.Job;

/**
 * Disables nodes that stay offline for more than max period allowed
 *
 * @author Aye Maung
 * @since v4.0
 */
public class NodeConnectionStatusJob extends Job
{
    public static final int FREQ_SECONDS = 60;
    public static Long MAX_OFFLINE_PERIOD = 7 * 24 * 60 * 60 * 1000L;  //default one week

    @Override
    public boolean init()
    {
        if (!Environment.getInstance().onKaiNode())
        {
            return false;
        }

        MAX_OFFLINE_PERIOD = ConfigsNode.getInstance().allowedOfflineDays() * 24 * 60 * 60 * 1000L;
        return super.init();
    }

    @Override
    public void doJob()
    {
        try
        {
            NodeManager nodeMgr = NodeManager.getInstance();
            if (!nodeMgr.isRegisteredOnCloud())
            {
                return;
            }

            NodeInfo nodeInfo = nodeMgr.getNodeInfo();

            NodeLicense nodeLicense = nodeMgr.getLicense();
            Long currentTime = Environment.getInstance().getCurrentUTCTimeMillis();
            int status = NetworkCheck.getNetworkStatus();

            //update last known cloud contact
            if ((status & NetworkCheck.STATUS_CLOUD_SERVER_CONNECTED) != 0)
            {
                nodeInfo.setCloudLastContacted(currentTime);
                nodeInfo.save();
            }

            boolean hasExceeded = (currentTime > nodeInfo.getCloudLastContacted() + MAX_OFFLINE_PERIOD);
            boolean nodeSuspended = nodeMgr.isSuspended();
            boolean licenseSuspended = nodeLicense.status.equals(LicenseStatus.SUSPENDED);

            if (hasExceeded && !nodeSuspended)
            {
                Util.printImptLog("Node has been offline for more than the time allowed");
                NodeProvisioning.getInstance().suspendNode();
            }
            else if (!hasExceeded && nodeSuspended && !licenseSuspended)
            {
                Util.printImptLog("Node is back online");
                NodeProvisioning.getInstance().activateNode();
            }
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
        }
    }
}

