package models.command.cloud;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.annotations.Entity;
import com.kaisquare.sync.CommandType;
import com.kaisquare.sync.TaskManager;
import models.command.QueuedPlatformCommand;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 */
@Entity
public class CloudNodeCommand extends QueuedPlatformCommand
{
    public static MorphiaQuery queryByMac(String macAddress)
    {
        return q().filter("macAddress", macAddress);
    }

    /**
     * @param nodeId      Node ID on cloud (cloudPlatformDeviceId of {@link models.node.NodeObject})
     * @param macAddress  Node's MAC address
     * @param commandType Command Type
     */
    public CloudNodeCommand(String nodeId,
                            String macAddress,
                            CommandType commandType)
    {
        this(nodeId, macAddress, commandType, new ArrayList<String>());
    }

    /**
     * @param nodeId      Node ID on cloud (cloudPlatformDeviceId of {@link models.node.NodeObject})
     * @param macAddress  Node's MAC address
     * @param commandType Command Type
     * @param params      List of parameters.
     */
    public CloudNodeCommand(String nodeId,
                            String macAddress,
                            CommandType commandType,
                            List<String> params)
    {
        super(nodeId, macAddress, commandType, params);
    }

    @Override
    public String toString()
    {
        return String.format("Cloud command (%s)", getCommandType());
    }

    @Override
    public Model save()
    {
        if (getCommandType().isSequentialExecutionForSameNode())
        {
            return super.save();
        }
        else
        {
            Logger.info("Sending non-sequential command (%s:%s)", getCommandType(), getNodeId());
            TaskManager.getInstance().sendCommand(getNodeId(),
                                                  getMacAddress(),
                                                  getCommandType(),
                                                  getParamsAsArray());
            return this;
        }
    }

}
