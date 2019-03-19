package platform.kaisyncwrapper.cloud.tasks;

import jobs.node.FailedActionsRetryJob;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import platform.kaisyncwrapper.CloudVcaRetryAction;
import play.Logger;

import java.util.concurrent.Callable;

/**
 * Sent from Cloud when node vca is activated by cloud
 * processCommand will be executed on node
 *
 * @author Aye Maung
 */
public class CloudActivateNodeVca extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        String vcaInstanceId = getParameter(0);
        Callable<Boolean> action = createAction(vcaInstanceId);
        if (!action.call())
        {
            CloudVcaRetryAction retryAction = new CloudVcaRetryAction(action, "CloudActivateNodeVca -> dbInstance.activate");
            FailedActionsRetryJob.getInstance().queue(retryAction);
        }

        return true;
    }

    private Callable<Boolean> createAction(final String vcaInstanceId)
    {
        return new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                try
                {
                    IVcaInstance dbInstance = VcaManager.getInstance().getVcaInstance(vcaInstanceId);
                    dbInstance.activate();
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
