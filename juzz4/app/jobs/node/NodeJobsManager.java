package jobs.node;

import jobs.SingletonJob;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * This class is relatively new, so not all singleton jobs are here yet
 *
 * @author Aye Maung
 * @since v4.5
 */
public enum NodeJobsManager
{
    INSTANCE;

    private final List<SingletonJob> singletonJobs = Arrays.asList(
            NodeNotificationsJob.getInstance(),
            SyncEventToCloudJob.getInstance(),
            SyncVcaListToCloudJob.getInstance(),
            SyncVideoToCloudJob.getInstance(),
            FailedActionsRetryJob.getInstance(),
            ThrottledCommandSenderJob.getInstance()
    );

    public static NodeJobsManager getInstance()
    {
        return INSTANCE;
    }

    public void startAll()
    {
        for (SingletonJob job : singletonJobs)
        {
            job.start();
        }
    }

    public String getPrintedStatusList()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println(String.format("Singleton Jobs (total: %s)", singletonJobs.size()));
        out.println("~~~~~~~~~~~~~~~~~~~~~~");
        out.println();

        for (SingletonJob job : singletonJobs)
        {
            out.println(String.format("%s (every %ss)", job.getClass().getSimpleName(), job.getFreqSeconds()));
            out.println(job.getPrintedStatus());
            out.println();
        }

        return sw.toString();
    }
}
