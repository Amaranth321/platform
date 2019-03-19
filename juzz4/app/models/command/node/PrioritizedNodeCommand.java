package models.command.node;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import com.kaisquare.sync.CommandType;
import com.kaisquare.util.SysInfoUtil;
import models.command.QueuedPlatformCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * For commands that should be executed based on priority.
 *
 * @author Aye Maung
 */
@Entity
public class PrioritizedNodeCommand extends QueuedPlatformCommand
{
    public static final int PRIORITY_ASAP = 0;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_LOW = 3;

    @Indexed
    private final int priority;

    /**
     * @param priority    Priority Value
     * @param nodeId      Node ID on cloud (cloudPlatformDeviceId)
     * @param commandType Command Type
     */
    public PrioritizedNodeCommand(int priority,
                                  String nodeId,
                                  CommandType commandType)
    {
        this(priority, nodeId, commandType, new ArrayList<String>());
    }

    /**
     * @param priority    Priority Value
     * @param nodeId      Node ID on cloud (cloudPlatformDeviceId)
     * @param commandType Command Type
     * @param params      List of parameters.
     */
    public PrioritizedNodeCommand(int priority,
                                  String nodeId,
                                  CommandType commandType,
                                  List<String> params)
    {
        super(nodeId, SysInfoUtil.getMacAddress(false), commandType, params);
        this.priority = priority;
    }

    public int getPriority()
    {
        return priority;
    }

    @Override
    public String toString()
    {
        return String.format("Prioritized command (%s : P=%s)", getCommandType(), getPriority());
    }

}
