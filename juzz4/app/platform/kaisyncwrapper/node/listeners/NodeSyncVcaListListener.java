package platform.kaisyncwrapper.node.listeners;

import com.kaisquare.sync.NodeCommandState;
import models.NodeCommand;
import platform.kaisyncwrapper.QueuedCommandStateListener;
import platform.node.KaiSyncCommandClient;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventType;

/**
 * Author:  Aye Maung
 */
public class NodeSyncVcaListListener implements QueuedCommandStateListener
{
    @Override
    public void onStateChanged(NodeCommand command, NodeCommandState state)
    {
        KaiSyncCommandClient.analyticsSyncStatus.getAndSet(state == NodeCommandState.Success);

        if (KaiSyncCommandClient.isStartupSyncDone())
        {
            PlatformEventMonitor.getInstance().broadcast(PlatformEventType.STARTUP_SYNC_COMPLETED);
        }
    }
}
