package jobs.cloud;

import lib.util.exceptions.InvalidEnvironmentException;
import platform.Environment;
import play.jobs.Job;

/**
 * @author Aye Maung
 * @since v4.4
 */
public abstract class CloudCronJob extends Job
{
    @Override
    public boolean init()
    {
        if (!Environment.getInstance().onCloud())
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
