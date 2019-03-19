package jobs.node;

import lib.util.exceptions.InvalidEnvironmentException;
import platform.Environment;
import play.jobs.Job;

/**
 * @author Aye Maung
 * @since v4.4
 */
abstract class NodeCronJob extends Job
{
    protected NodeCronJob()
    {
        // all cron jobs should be singletons
        // hence, prevent instance creation from elsewhere
    }

    @Override
    public boolean init()
    {
        if (!Environment.getInstance().onKaiNode())
        {
            throw new InvalidEnvironmentException();
        }

        if (!Environment.getInstance().isStartupTasksCompleted())
        {
            return false;
        }

        return super.init();
    }
}
