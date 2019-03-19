package jobs;

import com.kaisquare.sync.CommandType;
import platform.CloudLicenseManager;
import platform.Environment;
import platform.InventoryManager;
import platform.analytics.VcaManager;
import platform.events.EventManager;
import platform.kaisyncwrapper.CommandDestination;
import platform.kaisyncwrapper.node.SequencedCommandQueue;
import play.Logger;
import play.jobs.Job;

/**
 * Do all startup registrations here
 *
 * @author Aye Maung
 */
public class RegisterTasks extends Job
{
    @Override
    public void doJob()
    {
        registerKaiSyncCommands();
        initPlatformEventSubscribers();
    }

    private void registerKaiSyncCommands()
    {
        try
        {
            int cmdCount = 0;
            if (Environment.getInstance().onKaiNode())
            {
                for (CommandType commandType : CommandType.values())
                {
                    if (commandType.destination().equals(CommandDestination.NODE))
                    {
                        commandType.register();
                        ++cmdCount;
                    }
                }
            }
            else if (Environment.getInstance().onCloud())
            {
                for (CommandType commandType : CommandType.values())
                {
                    if (commandType.destination().equals(CommandDestination.CLOUD))
                    {
                        commandType.register();
                        ++cmdCount;
                    }
                }
            }

            Logger.info("[%s] %s commands registered", getClass().getSimpleName(), cmdCount);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    public void initPlatformEventSubscribers()
    {
        //todo: find a better way to init subscriptions
        if (Environment.getInstance().onCloud())
        {
            CloudLicenseManager.getInstance().subscribePlatformEvents();
            InventoryManager.getInstance().subscribePlatformEvents();

        }
        else if (Environment.getInstance().onKaiNode())
        {
            SequencedCommandQueue.getInstance().subscribePlatformEvents();

        }

        //both cloud and node
        if (Environment.getInstance().onWebInstance())
        {
            EventManager.getInstance().subscribePlatformEvents();
            VcaManager.getInstance().subscribePlatformEvents();
        }
    }

}

