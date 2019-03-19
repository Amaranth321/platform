package platform.kaisyncwrapper;

import jobs.node.FailedActionsRetryJob;
import platform.analytics.VcaThriftClient;

import java.util.concurrent.Callable;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class CloudVcaRetryAction extends FailedActionsRetryJob.RetryAction
{
    public CloudVcaRetryAction(Callable<Boolean> action, String nameForLogging)
    {
        super(action, nameForLogging);
    }

    @Override
    public boolean canRetryNow()
    {
        return VcaThriftClient.getInstance().isVcaServerOnline();
    }

    @Override
    public boolean canStopRetrying()
    {
        //no way to inform cloud that the action failed
        //better keep retrying forever
        return false;
    }
}
