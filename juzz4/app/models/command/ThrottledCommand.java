package models.command;

import com.google.code.morphia.annotations.Entity;
import com.kaisquare.sync.CommandType;
import models.command.node.SequencedNodeCommand;
import play.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * For some types of commands, only the latest update matters. e.g. updating status
 * <p>
 * Use this for such commands to reduce the command traffic for Cloud
 *
 * @author Aye Maung
 */
@Entity
public class ThrottledCommand extends QueuedPlatformCommand
{
    private final long triggerTime;

    /**
     * Existing commands of the same type will be replaced based on the provided comparator
     *
     * @param nodeCommand     command to send out
     * @param throttleSeconds delay before sending out the command
     * @param comparator      comparator to check if the command is replaceable
     */
    public static void queue(SequencedNodeCommand nodeCommand,
                             int throttleSeconds,
                             Comparator<QueuedPlatformCommand> comparator)
    {
        long triggerTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(throttleSeconds);

        //check existing
        MorphiaQuery queryByType = q().filter("commandType", nodeCommand.getCommandType());
        Iterable<ThrottledCommand> existingList = queryByType.fetch();
        for (ThrottledCommand cmd : existingList)
        {
            if (comparator.compare(nodeCommand, cmd) == 0)
            {
                // use the latest triggerTime
                triggerTime = cmd.triggerTime > triggerTime ? cmd.triggerTime : triggerTime;
                Logger.debug("[Throttling] Replaced the existing command with the latest (%s)", nodeCommand.toString());
                cmd.delete();
            }
        }

        new ThrottledCommand(nodeCommand.getNodeId(),
                             nodeCommand.getMacAddress(),
                             nodeCommand.getCommandType(),
                             nodeCommand.getParams(),
                             triggerTime
        ).save();
        Logger.debug("Queued throttled command (%s) - %s seconds", nodeCommand.toString(), throttleSeconds);
    }

    public static MorphiaQuery queryReadyToTrigger()
    {
        return q().filter("triggerTime <", System.currentTimeMillis());
    }

    private ThrottledCommand(String nodeId,
                             String macAddress,
                             CommandType commandType,
                             List<String> params,
                             long triggerTime)
    {
        super(nodeId, macAddress, commandType, params);
        this.triggerTime = triggerTime;
    }

    public SequencedNodeCommand asSequencedCommand()
    {
        return new SequencedNodeCommand(getNodeId(), getCommandType(), getParams());
    }
}
