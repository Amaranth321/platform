package jobs.cloud.independent;

import com.kaisquare.sync.CommandType;
import jobs.cloud.CloudCronJob;
import lib.util.exceptions.ApiException;
import models.command.cloud.ScheduledCommand;
import models.command.cloud.ScheduledCommand.Status;
import models.node.NodeObject;
import org.joda.time.DateTime;
import platform.CloudActionMonitor;
import platform.nodesoftware.NodeSoftwareStatus;
import play.Logger;
import play.jobs.Every;
import play.modules.morphia.Model;

import java.util.List;

@Every("5s")
public class ScheduledCommandJob extends CloudCronJob
{
    @Override
    public void doJob()
    {
        // get query hour job
        DateTime currentTime = new DateTime();
        DateTime queryTime = currentTime.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

        processOverdueCommands(queryTime.getMillis());

        Iterable<ScheduledCommand> commandList = ScheduledCommand.findByScheduledTime(queryTime.getMillis());
        if (commandList == null)
        {
            return;
        }

        for (ScheduledCommand scheduledCommand : commandList)
        {
            // check for available command
            if (!checkAvailableCommand(scheduledCommand.getCommandType()))
            {
                scheduledCommand.setStatus(Status.ERROR);
                scheduledCommand.save();
                continue;
            }

            process(scheduledCommand);
            scheduledCommand.setStatus(Status.COMPLETED);
            scheduledCommand.save();
        }
    }

    private boolean checkAvailableCommand(CommandType commandType)
    {
        switch (commandType)
        {
            case CLOUD_UPDATE_NODE:
                return true;
            default:
                Logger.warn("Scheduled Command Job not supported for: %s", commandType);
                return false;
        }
    }

    private void process(ScheduledCommand scheduledCommand)
    {
        List<String> cloudPlatformDeviceIdList = scheduledCommand.getScheduledNodeIds();

        for (String cloudPlatformDeviceId : cloudPlatformDeviceIdList)
        {
            try
            {
                switch (scheduledCommand.getCommandType())
                {
                    case CLOUD_UPDATE_NODE:
                        NodeObject nodeObject = NodeObject.findByPlatformId(cloudPlatformDeviceId);
                        if (nodeObject == null)
                        {
                            break;
                        }

                        if (nodeObject.getSoftwareStatus().equals(NodeSoftwareStatus.UPDATE_AVAILABLE))
                        {
                            //update status
                            CloudActionMonitor.getInstance().cloudUpdateNodeSoftware(cloudPlatformDeviceId);
                            nodeObject.setSoftwareStatus(NodeSoftwareStatus.UPDATING, null);
                            nodeObject.save();
                        }
                        break;
                    default:
                        Logger.warn("Scheduled Command Job not supported for: %s", scheduledCommand.getCommandType());
                        break;
                }
            }
            catch (ApiException e)
            {
                Logger.error("Unable to process command: %s in Cloud for Node: %s",
                        scheduledCommand.getCommandType(),
                        cloudPlatformDeviceId);
            }
        }
    }

    private void processOverdueCommands(long endTime)
    {
        Model.MorphiaQuery q = ScheduledCommand.q();
        q.filter("status", Status.PENDING);
        q.filter("scheduledTime <", endTime);

        Iterable<ScheduledCommand> overdueCommandList = q.fetch();
        for (ScheduledCommand scheduledCommand : overdueCommandList)
        {
            scheduledCommand.setStatus(Status.ERROR);
            scheduledCommand.save();
        }
    }

}
