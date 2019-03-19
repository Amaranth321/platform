package platform.kaisyncwrapper.cloud.listeners;

import com.kaisquare.sync.NodeCommandState;
import models.NodeCommand;
import models.NodeLogFile;
import platform.kaisyncwrapper.QueuedCommandStateListener;

public class CloudPullLogListener implements QueuedCommandStateListener
{
    @Override
    public void onStateChanged(NodeCommand command, NodeCommandState state)
    {
        if (state == NodeCommandState.Failed)
        {
            NodeLogFile logFile = NodeLogFile.find("nodeId", command.getNodeId()).first();
            logFile.status = NodeLogFile.PullingStatus.Standby;
            logFile.save();
        }
    }
}
