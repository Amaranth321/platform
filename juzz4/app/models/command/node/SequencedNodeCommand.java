package models.command.node;

import com.google.code.morphia.annotations.Entity;
import com.kaisquare.sync.CommandType;
import com.kaisquare.util.SysInfoUtil;
import models.command.QueuedPlatformCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * For node commands that should be queued and executed in sequence
 *
 * @author Aye Maung
 */
@Entity
public class SequencedNodeCommand extends QueuedPlatformCommand
{
    /**
     * @param nodeId      Node ID on cloud (cloudPlatformDeviceId)
     * @param commandType Command Type
     */
    public SequencedNodeCommand(String nodeId,
                                CommandType commandType)
    {
        this(nodeId, commandType, new ArrayList<String>());
    }

    /**
     * @param nodeId      Node ID on cloud (cloudPlatformDeviceId)
     * @param commandType Command Type
     * @param params      List of parameters.
     */
    public SequencedNodeCommand(String nodeId,
                                CommandType commandType,
                                List<String> params)
    {
        super(nodeId,
              SysInfoUtil.getMacAddress(false),
              commandType,
              params);
    }

    @Override
    public String toString()
    {
        return String.format("Sequenced command (%s)", getCommandType());
    }
}
