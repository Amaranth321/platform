package platform.api;

import lib.util.Util;
import platform.api.progress.ProgressListener;
import play.Logger;
import play.jobs.Job;

import java.util.concurrent.TimeUnit;

/**
 * To be used for API-initiated long-running tasks.
 * For progress monitoring, use {@link APITaskTracker#startTaskWithListener(AsyncAPITask)}
 *
 * @author Aye Maung
 * @since v4.4
 */
public abstract class AsyncAPITask
{
    private final long userId;
    private final String apiName;
    private ProgressListener listener;

    public AsyncAPITask(long userId, String apiName)
    {
        this.userId = userId;
        this.apiName = apiName;
    }

    /**
     * @return User ID of the API caller
     */
    public long getUserId()
    {
        return userId;
    }

    /**
     * @return Name of the API that initiated this task
     */
    public String getApiName()
    {
        return apiName;
    }

    public void addProgressListener(ProgressListener listener)
    {
        this.listener = listener;
    }

    public void start()
    {
        new Job()
        {
            @Override
            public void doJob()
            {
                long startTime = System.nanoTime();
                try
                {
                    executeTask();
                    taskTerminated(false);
                }
                catch (Exception e)
                {
                    Logger.error(e, "");
                    taskTerminated(true);
                }

                long endTime = System.nanoTime();
                Logger.debug(Util.whichClass() + "%s api task took %s seconds",
                             apiName,
                             TimeUnit.NANOSECONDS.toSeconds(endTime - startTime));
            }
        }.now();
    }

    protected abstract void executeTask() throws Exception;

    protected boolean listenerExists()
    {
        return (listener != null);
    }

    protected void taskStarted(String taskName)
    {
        if (listenerExists())
        {
            listener.taskChanged(taskName);
        }
    }

    protected void progressChanged(int newPercent)
    {
        if (listenerExists())
        {
            listener.percentChanged(newPercent);
        }
    }

    protected void taskTerminated(boolean withError)
    {
        if (listenerExists())
        {
            listener.terminated(withError);
        }
    }
}
