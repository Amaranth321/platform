package platform.kaisyncwrapper.cloud.tasks;

import platform.node.NodeManager;

/**
 * Sent from Cloud when node license is unsuspended by cloud
 * processCommand will be executed on node
 *
 * @author Aye Maung
 */
public class CloudUnsuspendLicense extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        NodeManager.getInstance().activateLicense();
        return true;
    }
}
