package platform.kaisyncwrapper.cloud.tasks;

import jobs.node.FailedActionsRetryJob;
import platform.analytics.VcaInfo;
import platform.analytics.VcaManager;
import platform.kaisyncwrapper.CloudVcaRetryAction;
import platform.kaisyncwrapper.KaiSyncHelper;
import play.Logger;

import java.util.concurrent.Callable;

/**
 * Sent from Cloud when node vca is added by cloud
 * processCommand will be executed on node
 *
 * @author Aye Maung
 */
public class CloudAddNodeVca extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        String jsonInfo = getParameter(0);
        VcaInfo sentInfo = KaiSyncHelper.parseVcaInfoReceivedOnNode(jsonInfo);

        Callable<Boolean> action = createAddAction(sentInfo);
        if (!action.call())
        {
            CloudVcaRetryAction retryAction = new CloudVcaRetryAction(action, "CloudAddNodeVca > VcaManager.addNewVca");
            FailedActionsRetryJob.getInstance().queue(retryAction);
        }

        return true;
    }

    private Callable<Boolean> createAddAction(final VcaInfo sentInfo)
    {
        return new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                try
                {
                    VcaManager.getInstance().addNewVca(sentInfo);
                    return true;
                }
                catch (Exception e)
                {
                    Logger.error(e, "");
                    return false;
                }
            }
        };
    }
}
