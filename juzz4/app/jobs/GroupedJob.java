package jobs;

import play.Logger;
import play.jobs.Job;
import play.libs.F.Promise;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Run a bunch of jobs at same time, and wait for all of the jobs to finish their work.
 * This class accepts {@link Job} and any {@link Callable} objects
 */
public class GroupedJob implements Runnable, Callable<Object>
{

    private Queue<Callable<Object>> jobs = new LinkedList<Callable<Object>>();
    private List<Promise<Object>> promises = new LinkedList<Promise<Object>>();
    protected final Promise<Object> future = new Promise<Object>();
    private String groupName;
    protected Object result = null;

    public GroupedJob()
    {
        super();
        groupName = String.format("GroupJob-%d", Thread.currentThread().getId());
    }

    public GroupedJob(String name)
    {
        super();
        groupName = name;
    }

    public GroupedJob(List<Callable<Object>> list)
    {
        super();
        jobs.addAll(list);
    }

    public String getGroupName()
    {
        return groupName;
    }

    public Promise<Object> now()
    {
        new Thread(this, groupName).start();
        return future;
    }

    /**
     * Add job in a queue, the first added job is the most priority
     *
     * @param Callable<Object>
     */
    public void addTask(Callable task)
    {
        jobs.add(task);
    }

    /**
     * Remove an added job
     *
     * @param Callable<Object>
     */
    public void removeTask(Callable task)
    {
        jobs.remove(task);
    }

    protected Callable<Object> pollJob()
    {
        return jobs.poll();
    }

    @Override
    public Object call()
    {
        run();
        return result;
    }

    @Override
    public void run()
    {
        Callable<Object> task = null;
        while ((task = jobs.poll()) != null)
        {
            promises.add(submitTask(task));
        }

        if (promises.size() > 0)
        {
            Promise<List<Object>> p = Promise.waitAll(promises);
            try
            {
                result = p.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                Logger.error(e, "");
            }
        }

        if (result == null)
        {
            result = new ArrayList<Object>();
        }

        future.invoke(result);
    }

    protected Promise<Object> submitTask(final Callable<Object> task)
    {
        if (task instanceof Job)
        {
            return ((Job) task).now();
        }
        else if (task instanceof GroupedJob)
        {
            return ((GroupedJob) task).now();
        }
        else
        {
            return new Job<Object>()
            {
                public Object doJobWithResult() throws Exception
                {
                    Object result = null;
                    try
                    {
                        result = task.call();
                    }
                    catch (Exception e)
                    {
                        Logger.error(e, "error when running group jobs.");
                    }
                    finally
                    {
                    }
                    return result;
                }

            }.now();
        }
    }

    @Override
    public String toString()
    {
        return String.format("[GroupJob] %s", groupName);
    }
}
