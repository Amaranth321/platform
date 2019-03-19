package platform.kaisyncwrapper.node.tasks;

import models.NodeCommand;

/**
 * Sent from nodes to update statistics on cloud
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeSendStatistics extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        //no longer supported.
        return true;
    }
}
