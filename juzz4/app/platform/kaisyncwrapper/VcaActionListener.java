package platform.kaisyncwrapper;

import com.kaisquare.sync.NodeCommandState;
import models.NodeCommand;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class VcaActionListener implements QueuedCommandStateListener
{
    @Override
    public void onStateChanged(NodeCommand command, NodeCommandState state)
    {
        if (command.getCommand().isVcaAction())
        {
            if (state == NodeCommandState.Failed)
            {
                Logger.error("Vca action command failed (nodeId=%s, type=%s, params=%s)",
                             command.getNodeId(),
                             command.getCommand(),
                             command.getParameters());
            }
        }
    }

}
