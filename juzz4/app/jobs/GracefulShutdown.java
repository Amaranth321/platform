package jobs;

import core.CoreClient;
import platform.KaiPlatform;
import play.jobs.Job;
import play.jobs.OnApplicationStop;

@OnApplicationStop
public class GracefulShutdown extends Job
{
    @Override
    public void doJob()
    {
        KaiPlatform.stop();
        CoreClient.getInstance().stop();
        KaiPlatform.stopServers();
    }
}

