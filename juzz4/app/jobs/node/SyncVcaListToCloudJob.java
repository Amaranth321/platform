package jobs.node;

import jobs.SingletonJob;
import platform.analytics.VcaThriftClient;
import platform.node.NodeManager;
import play.Logger;

/**
 * author tbnguyen1407
 */
public class SyncVcaListToCloudJob extends AbstractCloudDependentJob implements SingletonJob
{
    private boolean isStarted = false;

    public static final int FREQ_SECONDS = 3600;

    private static final SyncVcaListToCloudJob instance = new SyncVcaListToCloudJob();

    public static SingletonJob getInstance()
    {
        return instance;
    }

    @Override
    public void doTask()
    {
        if (!VcaThriftClient.getInstance().isVcaServerOnline())
        {
            Logger.info("[%s] waiting for VCA server to be ready", getClass().getSimpleName());
            in(10);
            return;
        }

        try
        {
            NodeManager.getInstance().syncVcaListWithCloud();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
        finally
        {
            in(FREQ_SECONDS);
        }
    }

    @Override
    public void start()
    {
        if (!isStarted)
        {
            now();
            this.isStarted = true;
        }
    }

    @Override
    public int getFreqSeconds()
    {
        return FREQ_SECONDS;
    }

    @Override
    public String getPrintedStatus()
    {
        return "";
    }
}
