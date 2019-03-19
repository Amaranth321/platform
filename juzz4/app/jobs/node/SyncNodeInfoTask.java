package jobs.node;

import platform.Environment;
import platform.analytics.VcaThriftClient;
import platform.node.NodeManager;
import play.Logger;
import play.jobs.Job;

public class SyncNodeInfoTask extends Job
{
    @Override
    public void doJob()
    {
        if (!Environment.getInstance().onKaiNode() || NodeManager.getInstance().isSuspended())
        {
            return;
        }

        if (!VcaThriftClient.getInstance().isVcaServerOnline())
        {
            Logger.info("[%s] waiting for VCA server to be ready", getClass().getSimpleName());
            in(10);
            return;
        }

        try
        {
            NodeManager nodeMgr = NodeManager.getInstance();
            nodeMgr.sendNodeInfoToCloud();
            nodeMgr.syncDevicesWithCloud();
            nodeMgr.syncVcaListWithCloud();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }
}
