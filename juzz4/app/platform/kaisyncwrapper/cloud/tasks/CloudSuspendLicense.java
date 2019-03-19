package platform.kaisyncwrapper.cloud.tasks;

import platform.node.NodeManager;

/**
 * Sent from Cloud when node license is suspended by cloud
 * processCommand will be executed on node
 *
 * @author Aye Maung
 */
public class CloudSuspendLicense extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        NodeManager.getInstance().suspendLicense();
        return true;
    }
}
