package platform.kaisyncwrapper.node.tasks;

import com.kaisquare.sync.ITask;
import lib.util.Util;
import models.backwardcompatibility.Device;
import models.MongoDevice;
import models.NodeCommand;
import models.node.NodeObject;
import org.apache.commons.lang.StringUtils;
import platform.debug.CommandDebugger;
import platform.debug.CommandLogType;
import play.Logger;

/**
 * Abstract class for handling commands sent from nodes.
 *
 * @author Aye Maung
 * @since v4.4
 */
public abstract class NodeToCloudCommandTask implements ITask
{
    private NodeCommand nodeCommand;
    private NodeObject nodeObject;
    private Device nodeDevice;

    protected abstract boolean processCommand(NodeCommand command) throws Exception;

    @Override
    public final boolean doTask(NodeCommand command)
    {
        try
        {
            if (!verifyCommand(command))
            {
                return false;
            }

            boolean result = processCommand(nodeCommand);
            if (result)
            {
                String msg = Util.cutIfLong(StringUtils.join(command.getParameters(), " "), 100);
                CommandDebugger.getInstance().logCloudCommand(
                        command,
                        CommandLogType.PROCESSED,
                        nodeObject.getName(),
                        msg);
            }

            return result;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    protected String getNodeName()
    {
        return nodeObject.getName();
    }

    protected NodeObject getNodeObject()
    {
        return nodeObject;
    }

    protected Device getNodeDevice()
    {
        return nodeDevice;
    }

    private boolean verifyCommand(NodeCommand command)
    {
        try
        {
            //node object
            nodeObject = NodeObject.findByPlatformId(command.getNodeId());
            if (nodeObject == null)
            {
                return false;
            }

            //device
            MongoDevice mongoNodeDevice = MongoDevice.getByPlatformId(command.getNodeId());

            // for compatibility
            if (mongoNodeDevice == null)
            {
                Logger.error(Util.whichClass() + "node device not found (%s)", command.getCommand());
                return false;
            }
            nodeDevice = new Device(mongoNodeDevice);

            //mac
            if (!command.getMacAddress().equalsIgnoreCase(nodeDevice.deviceKey))
            {
                Logger.error(Util.whichClass() + "mac address doesn't match (%s)", command.getCommand());
                return false;
            }

            nodeCommand = command;
            return true;
        }
        catch (Exception e)
        {
            Logger.error("[%s] NodeObject not found (nodeId=%s) : %s",
                         command.getCommand(),
                         command.getNodeId(),
                         Util.getStackTraceString(e));
            return false;
        }
    }
}
