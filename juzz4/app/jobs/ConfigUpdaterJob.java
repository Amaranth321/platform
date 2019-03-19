package jobs;

import platform.Environment;
import platform.config.readers.*;
import play.jobs.Every;
import play.jobs.Job;

@Every("10mn")
@play.db.jpa.NoTransaction
public class ConfigUpdaterJob extends Job
{
    /**
     * This is necessary because configuration files are loaded based on the application type.
     * Hence, this needs to be loaded separately first at server startup
     */
    public static void runOnceAtStartup()
    {
        if (!ConfigsServers.getInstance().loadConfigs())
        {
            Environment.getInstance().stopServer("Error: config.json");
        }

        ConfigUpdaterJob.readConfigFiles();
    }

    public static void readConfigFiles()
    {
        Environment env = Environment.getInstance();

        if (!ConfigsShared.getInstance().loadConfigs())
        {
            Environment.getInstance().stopServer("Error: configs.shared.json");
        }

        if (!AccountDefaultSettings.getInstance().loadConfigs())
        {
            Environment.getInstance().stopServer("Error: account.defaults.json");
        }

        if (env.onCloud())
        {
            if (!ConfigsCloud.getInstance().loadConfigs())
            {
                Environment.getInstance().stopServer("Error: configs.cloud.json");
            }
        }
        else if (env.onKaiNode())
        {
            if (!ConfigsNode.getInstance().loadConfigs())
            {
                Environment.getInstance().stopServer("Error: configs.node.json");
            }
        }
    }

    @Override
    public void doJob()
    {
        readConfigFiles();
    }
}
