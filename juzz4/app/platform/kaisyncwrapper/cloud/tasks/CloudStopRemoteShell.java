package platform.kaisyncwrapper.cloud.tasks;

import platform.node.NodeManager;

public class CloudStopRemoteShell extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        boolean result = NodeManager.getInstance().stopRemoteShell();
        return result;
    }
}
