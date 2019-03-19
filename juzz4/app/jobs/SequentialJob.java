package jobs;

import play.Logger;
import play.libs.F.Promise;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Run jobs as a sequence, this job will return a list of result for each added jobs
 */
public class SequentialJob extends GroupedJob
{

    public SequentialJob()
    {
        super();
    }

    public SequentialJob(List<Callable<Object>> list)
    {
        super(list);
    }

    @Override
    public void run()
    {
        Callable<Object> task = null;
        LinkedList<Object> list = new LinkedList<Object>();
        while ((task = pollJob()) != null)
        {
            Logger.info("Running startup task %s", task);
            Promise<Object> p = submitTask(task);
            try
            {
                list.add(p.get());
            }
            catch (InterruptedException | ExecutionException e)
            {
                Logger.error(e, "error running a task %s", task);
            }
            Logger.info("task %s done", task);
        }
        result = list;

        future.invoke(result);
    }
}
