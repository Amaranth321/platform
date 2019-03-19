package jobs.node;

import jobs.SingletonJob;
import platform.kaisyncwrapper.CloudVcaRetryAction;
import play.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Use this job only if the failed action has the potential to be successful if retried later.
 * e.g. failures due to the target server being offline (might become online later)
 * <p/>
 * Add a new queue depending on the retry behavior
 *
 * @author Aye Maung
 * @since v4.5
 */
public class FailedActionsRetryJob extends NodeCronJob implements SingletonJob
{
    private static final FailedActionsRetryJob instance = new FailedActionsRetryJob();

    private final Queue<RetryAction> vcaRetryActionQueue = new ConcurrentLinkedQueue<>();
    private boolean jobStarted = false;

    public static FailedActionsRetryJob getInstance()
    {
        return instance;
    }

    @Override
    public synchronized void start()
    {
        if (!jobStarted)
        {
            every(getFreqSeconds());
            jobStarted = true;
        }
    }

    @Override
    public int getFreqSeconds()
    {
        return 10;
    }

    @Override
    public String getPrintedStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println(String.format("vca queue   : %s", vcaRetryActionQueue.size()));
        return sw.toString();
    }

    public void queue(CloudVcaRetryAction vcaRetryAction)
    {
        vcaRetryActionQueue.add(vcaRetryAction);
    }

    @Override
    public void doJob()
    {
        processVcaQueue();

        //clear more queues below if any
    }

    private void processVcaQueue()
    {
        RetryAction action = vcaRetryActionQueue.peek();
        if (action == null)
        {
            return;
        }

        if (action.canStopRetrying())
        {
            vcaRetryActionQueue.poll();
            return;
        }

        if (action.canRetryNow() && action.call())
        {
            vcaRetryActionQueue.poll();
            return;
        }
    }

    public static abstract class RetryAction implements Callable<Boolean>
    {
        private final Callable<Boolean> action;
        private final String nameForLogging;

        private int failedAttempts = 0;

        public RetryAction(Callable<Boolean> action, String nameForLogging)
        {
            this.action = action;
            this.nameForLogging = nameForLogging;
        }

        public abstract boolean canRetryNow();

        public abstract boolean canStopRetrying();

        protected int getFailedAttempts()
        {
            return failedAttempts;
        }

        @Override
        public Boolean call()
        {
            try
            {
                Logger.info("Retrying failed action [%s]. Attempt #%s", nameForLogging, failedAttempts);
                boolean result = action.call();
                if (!result)
                {
                    failed();
                }
                else
                {
                    Logger.info("Retry successful [%s]", nameForLogging);
                }
                return result;
            }
            catch (Exception e)
            {
                Logger.error(e, "");
                failed();
                return false;
            }
        }

        private void failed()
        {
            ++failedAttempts;
        }
    }
}
