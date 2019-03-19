package jobs.cloud.independent;

import com.kaisquare.sync.TaskManager;
import jobs.cloud.CloudCronJob;
import play.jobs.Every;

@Every("5mn")
public class CommandCheckJob extends CloudCronJob
{

    @Override
    public void doJob() throws Exception
    {
        TaskManager.getInstance().checkPendingCommands();
    }

}
