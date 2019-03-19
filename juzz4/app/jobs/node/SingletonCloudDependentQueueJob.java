package jobs.node;

import jobs.SingletonJob;
import play.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This queue will be blocked if the item from {@link #getNext()} continuously fails.
 * This queue will be blocked if the cloud is unreachable.
 * <p>
 * Create another class if such behaviors are not optimal.
 * <p>
 * Inherited jobs should be included in {@link NodeJobsManager}
 *
 * @author Aye Maung
 * @since v4.4
 */
abstract class SingletonCloudDependentQueueJob<T> extends AbstractCloudDependentJob implements SingletonJob
{
    private final AtomicBoolean jobStarted = new AtomicBoolean(false);

    @Override
    public void start()
    {
        synchronized (jobStarted)
        {
            if (!jobStarted.get())
            {
                every(getFreqSeconds());
                jobStarted.set(true);
                Logger.info("'%s' job has started", this.getClass().getSimpleName());
            }
        }
    }

    @Override
    public int getFreqSeconds()
    {
        return 2;
    }

    @Override
    public String getPrintedStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println(String.format("Status       : %s", getStatus()));
        out.println(String.format("Items left   : %s", countRemainingItems()));
        return sw.toString();
    }

    /**
     * This is a delay between each {@link #process} to control the processing rate.
     * e.g. 500ms will limit the processing to the maximum of 2 tasks per minute.
     * <p>
     * pause duration value is recommended to be dependent on the server load.
     *
     * @return duration in milliseconds
     */
    protected abstract int getPauseDuration();

    /**
     * @return next item
     */
    protected abstract T getNext();

    /**
     * @return number of remaining items in the queue
     */
    protected abstract long countRemainingItems();

    /**
     * Unexpected exceptions will be caught and logged. And the job will not be stopped.
     * Checked exceptions should still be handled properly.
     *
     * @param item
     *
     * @throws Exception
     */
    protected abstract void process(T item) throws Exception;

    @Override
    protected final void doTask() throws Exception
    {
        T item = getNext();
        synchronized (this)
        {
            while (item != null)
            {
                process(item);
                pause();
                item = getNext();
            }
        }
    }

    private void pause()
    {
        try
        {
            wait(getPauseDuration());
        }
        catch (InterruptedException e)
        {
        }
    }
}
