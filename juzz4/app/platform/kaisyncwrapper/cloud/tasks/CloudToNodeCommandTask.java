package platform.kaisyncwrapper.cloud.tasks;

import com.kaisquare.sync.CommandType;
import com.kaisquare.sync.ITask;
import lib.util.Util;
import models.NodeCommand;
import models.node.NodeInfo;
import org.apache.commons.lang.StringUtils;
import platform.debug.CommandDebugger;
import platform.debug.CommandLogType;
import platform.node.NodeManager;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.4
 */
abstract class CloudToNodeCommandTask implements ITask
{
    private NodeCommand nodeCommand;

    protected CommandType getCommandType()
    {
        return nodeCommand.getCommand();
    }

    protected String getNodeId()
    {
        return nodeCommand.getNodeId();
    }

    protected String getParameter(int index)
    {
        return nodeCommand.getParameters().get(index);
    }

    protected abstract boolean processCommand() throws Exception;

    @Override
    public final boolean doTask(NodeCommand command)
    {
        try
        {
            if (!verifyCommand(command))
            {
                return false;
            }

            boolean result = processCommand();
            if (result)
            {
                //log
                String msg = Util.cutIfLong(StringUtils.join(command.getParameters(), " "), 100);
                CommandDebugger.getInstance().logNodeCommand(command, CommandLogType.PROCESSED, msg);
            }

            return result;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    private boolean verifyCommand(NodeCommand command)
    {
        try
        {
            String cmdNodeId = command.getNodeId();
            NodeInfo nodeInfo = NodeManager.getInstance().getNodeInfo();
            if (!nodeInfo.getCloudPlatformDeviceId().equals(cmdNodeId))
            {
                throw new Exception(String.format("Incorrect node Id (%s)", cmdNodeId));
            }

            //don't expose command object to inherited classes to reduce code clutter
            nodeCommand = command;
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }
}
